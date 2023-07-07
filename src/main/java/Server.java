import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Server {
    public List<String> clientFileList = new ArrayList<>();
    public List<String> serverFileList = new ArrayList<>();

    public Server() {
    }

    static Thread serverThread = new Thread(() -> {
        Server server = new Server();
        try (DatagramSocket serverSocket = new DatagramSocket(Constants.ServerPort)) {
            System.out.println("Server started. \nListening on port: localhost:" + Constants.ServerPort + "\n");
            while (true) {
                String clientRequest = TransmissionHandler.receiveData(serverSocket);
                System.out.println("Client request: " + clientRequest);
                if (Objects.equals(clientRequest.trim(), "AcceptFileList")) {
                    server.serverFileList = Utils.getFileList(Constants.serverFileHolder);
                    String clientMessage = TransmissionHandler.receiveData(serverSocket);
                    System.out.println(clientMessage);
                    server.clientFileList.addAll(
                            Arrays.stream(clientMessage.substring(1, clientMessage.length() - 1).split(","))
                                    .map(String::trim)
                                    .filter(file -> !server.clientFileList.contains(file) && !file.equals(".DS_Store"))
                                    .toList()
                    );


                    System.out.println(server.serverFileList + "___" + server.clientFileList);

                    for (String f : server.clientFileList) {
                        System.out.println(f);
                        if (!server.serverFileList.contains(f.trim())) {
                            TransmissionHandler.sendData("FileRequest: " + f, Constants.UserClientPort);
                            TransmissionHandler.receiveFile(serverSocket);
                            System.out.println(f + " received.");
                            Thread.sleep(2000);
                        }
                    }
                    TransmissionHandler.sendData("Clear", Constants.UserClientPort);
                }
                if (Objects.equals(clientRequest.split(":")[0].trim(), "DeleteFile")) {
                    String fileName = clientRequest.split(":")[1];
                    File toBeDeletedFile = new File(Constants.serverFileHolder + fileName);
                    if (toBeDeletedFile.delete()) {
                        server.serverFileList.remove(fileName);
                        server.clientFileList.remove(fileName);
                        System.out.println("Deleted the file: " + toBeDeletedFile.getName());
                    } else {
                        System.out.println("Failed to delete the file.");
                    }
                }

                System.out.println("All the client files are synchronized.\n");

            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    });

    public static void main(String[] args) {
        serverThread.start();
    }

    public static void fileReceiver(DatagramSocket serverSocket) throws IOException {
        try {
            TransmissionHandler.receiveFile(serverSocket);
            System.out.println("File transfer complete");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
