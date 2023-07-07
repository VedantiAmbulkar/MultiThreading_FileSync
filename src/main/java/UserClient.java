import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class UserClient {
    public List<String> clientFileList = new ArrayList<>();
    public List<String> serverFileList = new ArrayList<>();

    static Thread userClientFileWatcher = new Thread(() -> {
        try {
            FileWatcher.start(Constants.clientFileHolder);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    });

    public static volatile String trigger = "C";
    public static volatile String modFileName = "";

    static Thread UserClient = new Thread(() -> {
        UserClient userClient = new UserClient();
        try (DatagramSocket clientSocket = new DatagramSocket(Constants.UserClientPort)) {
            System.out.println("UserClient thread started on port : " + Constants.UserClientPort);
            while (true) {
                if (Objects.deepEquals(trigger, "D")) {
                    userClient.clientFileList = Utils.getFileList(Constants.clientFileHolder);
                    for (String file : userClient.serverFileList) {
                        System.out.println("Requesting server to delete file: " + file);
                        TransmissionHandler.sendData("DeleteFile:" + file, Constants.ServerPort);
                    }
                    trigger = "";
                } else if (Objects.deepEquals(trigger, "C")) {
                    TransmissionHandler.sendData("AcceptFileList", Constants.ServerPort);
                    userClient.clientFileList = Utils.getFileList(Constants.clientFileHolder);
                    TransmissionHandler.sendData(userClient.clientFileList.toString(), Constants.ServerPort);
                    String serverMessage = "";
                    while (!serverMessage.equals("Clear")) {
                        serverMessage = TransmissionHandler.receiveData(clientSocket);
                        System.out.println(serverMessage);
                        if (serverMessage.split(":")[0].equals("FileRequest")) {
                            String fileName = serverMessage.split(":")[1].trim();
                            System.out.println("Sending file " + fileName + " to server.");
                            TransmissionHandler.sendFile(Constants.clientFileHolder + fileName);
                            userClient.serverFileList.add(fileName);
                        }
                    }
                    userClient.serverFileList = new ArrayList<>(userClient.clientFileList);
                    trigger = "";
                } else if (Objects.deepEquals(trigger, "M")) {
                    TransmissionHandler.sendData("AcceptModifiedFile:" + modFileName, Constants.ServerPort);
                    String serverMessage = "";
                    while (!serverMessage.equals("Clear")) {
                        serverMessage = TransmissionHandler.receiveData(clientSocket);
                        System.out.println(serverMessage);
                        if (serverMessage.split(":")[0].equals("FileRequest")) {
                            String fileName = serverMessage.split(":")[1].trim();
                            System.out.println("Sending file " + fileName + " to server.");
                            TransmissionHandler.sendFile(Constants.clientFileHolder + fileName);
                            userClient.serverFileList.add(fileName);
                        }
                    }
                    trigger = "";
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    });

    public static void setTrigger(String flag) {

        trigger = flag;
    }

    public static void setTrigger(String flag, String fileName) {
        modFileName = fileName;
        trigger = flag;
    }

    public static void main(String[] args) {
        UserClient.start();
        userClientFileWatcher.start();
    }

}
