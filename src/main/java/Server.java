import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    public Map<Integer, List<String>> clientFileList = new HashMap<>();
    public List<String> serverFileList = new ArrayList<>();
    public Map<Integer, String> connectedPorts = new HashMap<>();

    public Server() {
    }

    static Thread serverThread = new Thread(() -> {
        Server server = new Server();
        Boolean justInit = false;
        try (DatagramSocket serverSocket = new DatagramSocket(Constants.ServerPort)) {
            System.out.println("Server started. \nListening on port: localhost:" + Constants.ServerPort + "\n");
            while (true) {

                String clientRequest = TransmissionHandler.receiveData(serverSocket);
                List<String> decodedRequest = decodeRequest(clientRequest);
                String request = decodedRequest.get(0);
                int port = Integer.parseInt(decodedRequest.get(1));
                String fileName = "";
                if (decodedRequest.size() > 2) {
                    fileName = decodedRequest.get(2);
                }

                if (Objects.equals(request, "CheckingForChanges")) {
                    TransmissionHandler.sendData("NoChanges", port);
                    continue;
                }

                System.out.println("Client request: " + clientRequest);

                switch (request.trim()) {

                    case "InitializeConnection" -> {
                        justInit = true;
                        String clientData = TransmissionHandler.receiveData(serverSocket);
                        Integer connPort = Integer.parseInt(clientData.split(":")[0]);
                        String connFolder = clientData.split(":")[1];
                        server.connectedPorts.put(connPort, connFolder);
                        server.clientFileList.put(port, new ArrayList<>());
                        TransmissionHandler.sendData("Connected", port);
                    }

                    case "AcceptFileList" -> {
                        handleAcceptFileList(server, serverSocket, port);
                        TransmissionHandler.sendData("Clear", port);
                        syncClients("C", server, "", port, justInit);
                        System.out.println("All the client files are synchronized.\n");
                        justInit = false;
                    }

                    case "AcceptModifiedFile" -> {
                        if (fileName.equals(".DS_Store")) {
                            continue;
                        }
                        TransmissionHandler.sendData("FileRequest: " + fileName, port);
                        TransmissionHandler.receiveFile(serverSocket, Constants.serverFileHolder);
                        System.out.println(fileName + " received.");
                        TransmissionHandler.sendData("Clear", port);
                        syncClients("M", server, fileName, port, false);
                        System.out.println("All the client files are synchronized.\n");
                    }

                    case "DeleteFile" -> {
                        File toBeDeletedFile = new File(Constants.serverFileHolder + fileName);
                        if (toBeDeletedFile.delete()) {
                            server.serverFileList.remove(fileName);
                            server.clientFileList.get(port).remove(fileName);
                            System.out.println("Deleted the file: " + toBeDeletedFile.getName());
                            syncClients("D", server, fileName, port, false);
                        } else {
                            System.out.println("Failed to delete the file.");
                        }
                        System.out.println("All the client files are synchronized.\n");
                    }

                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    });

    public static void handleAcceptFileList(Server server, DatagramSocket serverSocket, int port) throws IOException {
        server.serverFileList = Utils.getFileList(Constants.serverFileHolder);
        String clientMessage = TransmissionHandler.receiveData(serverSocket);
        System.out.println();
        if (clientMessage.equals("[]")) return;
        server.clientFileList.get(port).addAll(
                Arrays.stream(
                                clientMessage
                                        .substring(1, clientMessage.length() - 1)
                                        .split(","))
                        .map(String::trim)
                        .filter(file -> !server.clientFileList.get(port).contains(file) && !file.equals(".DS_Store"))
                        .toList()
        );
        for (String fileName : server.clientFileList.get(port)) {
            if (!server.serverFileList.contains(fileName.trim())) {
                TransmissionHandler.sendData("FileRequest: " + fileName, port);
                TransmissionHandler.receiveFile(serverSocket, Constants.serverFileHolder);
                System.out.println(fileName + " received.");
                server.serverFileList.add(fileName);
            }
        }
    }

    public static void syncClients(String action, Server server, String file, Integer currPort, Boolean justInit) throws IOException {
        switch (action) {

            case "C" -> {
                for (Integer port : server.connectedPorts.keySet()) {
                    if (server.serverFileList.isEmpty()) {
                        break;
                    }
                    if (Objects.equals(port, currPort) && !justInit) {
                        continue;
                    }
                    TransmissionHandler.sendData("AcceptSyncFiles", port);
                    for (String fileName : server.serverFileList.stream().filter(f -> !server.clientFileList.get(port).contains(f.trim()) && !f.trim().isBlank() && !f.trim().isEmpty()).toList()) {
                        TransmissionHandler.sendData(fileName, port);
                        TransmissionHandler.sendFile(Constants.serverFileHolder + fileName, port);
                    }
                    TransmissionHandler.sendData("TransmissionComplete", port);
                }
            }

            case "M" -> {
                for (Integer port : server.connectedPorts.keySet()) {
                    if (Objects.equals(port, currPort)) {
                        continue;
                    }
                    TransmissionHandler.sendData("AcceptSyncModFiles", port);
                    TransmissionHandler.sendFile(Constants.serverFileHolder + file, port);
                    TransmissionHandler.sendData("TransmissionComplete", port);
                }
            }

            case "D" -> {
                for (Integer port : server.connectedPorts.keySet()) {
                    if (Objects.equals(port, currPort)) {
                        continue;
                    }
                    TransmissionHandler.sendData("AcceptSyncDeletedFiles", port);
                    TransmissionHandler.sendData(file, port);
                    TransmissionHandler.sendData("TransmissionComplete", port);
                }
            }
        }
    }

    public static List<String> decodeRequest(String clientMessage) {
        return Arrays
                .stream(clientMessage.split(":"))
                .filter(s -> !s.isBlank())
                .toList();
    }

    public static void main(String[] args) {
        serverThread.start();
    }
}
