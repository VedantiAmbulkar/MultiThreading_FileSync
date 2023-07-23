import java.io.File;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UserClientB implements Client {
    public List<String> clientFileList = new ArrayList<>();
    public List<String> serverFileList = new ArrayList<>();
    public Thread userClient2FileWatcher;

    public UserClientB() {
        this.userClient2FileWatcher = new Thread(() -> {
            try {
                FileWatcher fileWatcher = new FileWatcher(Constants.CLIENT_2_FILE_HOLDER, this);
                fileWatcher.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public volatile String trigger = "I";
    public volatile String modFileName = "";

    public Thread userClient2Handler = new Thread(() -> {
        long lastSyncTime = 0;
        try (DatagramSocket clientSocket = new DatagramSocket(Constants.UserClientBPort)) {
            System.out.println("UserClientB thread started on port : " + Constants.UserClientBPort);
            while (true) {
                switch (trigger) {
                    case "I" -> {
                        TransmissionHandler.sendData("InitializeConnection:" + Constants.UserClientBPort, Constants.ServerPort);
                        TransmissionHandler.sendData(Constants.UserClientBPort + ":" + Constants.CLIENT_2_FILE_HOLDER, Constants.ServerPort);
                        String serverMessage = TransmissionHandler.receiveData(clientSocket);
                        if (Objects.deepEquals(serverMessage, "Connected")) {
                            setTrigger("C");
                            System.out.println("Connected to server ...");
                        }
                    }

                    case "D" -> {
                        this.clientFileList = Utils.getFileList(Constants.CLIENT_2_FILE_HOLDER);
                        for (String file : this.serverFileList.stream().filter(f -> !this.clientFileList.contains(f)).toList()) {
                            System.out.println("Requesting server to delete file: " + file);
                            TransmissionHandler.sendData("DeleteFile:" + Constants.UserClientBPort + ":" + file, Constants.ServerPort);
                            this.serverFileList.remove(file);
                        }
                        setTrigger("X");
                    }

                    case "C" -> {
                        TransmissionHandler.sendData("AcceptFileList:" + Constants.UserClientBPort, Constants.ServerPort);
                        this.clientFileList = Utils.getFileList(Constants.CLIENT_2_FILE_HOLDER);
                        TransmissionHandler.sendData(this.clientFileList.toString(), Constants.ServerPort);
                        String serverMessage = "";
                        while (!serverMessage.equals("Clear")) {
                            serverMessage = TransmissionHandler.receiveData(clientSocket);
                            if (!Objects.deepEquals(serverMessage, "Clear")) {
                                System.out.println(serverMessage);
                            } else {
                                System.out.println();
                            }
                            if (serverMessage.split(":")[0].equals("FileRequest")) {
                                String fileName = serverMessage.split(":")[1].trim();
                                System.out.println("Sending file " + fileName + " to server.");
                                TransmissionHandler.sendFile(Constants.CLIENT_2_FILE_HOLDER + fileName, Constants.ServerPort);
                                this.serverFileList.add(fileName);
                            }
                        }
                        this.serverFileList = new ArrayList<>(this.clientFileList);
                        setTrigger("X");
                    }

                    case "M" -> {
                        TransmissionHandler.sendData("AcceptModifiedFile:" + Constants.UserClientBPort + ":" + modFileName, Constants.ServerPort);
                        String serverMessage = "";
                        while (!serverMessage.equals("Clear")) {
                            serverMessage = TransmissionHandler.receiveData(clientSocket);
                            if (!Objects.deepEquals(serverMessage, "Clear")) {
                                System.out.println(serverMessage);
                            } else {
                                System.out.println();
                            }
                            if (serverMessage.split(":")[0].equals("FileRequest")) {
                                String fileName = serverMessage.split(":")[1].trim();
                                System.out.println("Sending file " + fileName + " to server.");
                                TransmissionHandler.sendFile(Constants.CLIENT_2_FILE_HOLDER + fileName, Constants.ServerPort);
                                this.serverFileList.add(fileName);
                            }
                        }
                        setTrigger("X");
                    }
                    case "X" -> {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastSyncTime >= Constants.SYNC_INTERVAL) {
                            TransmissionHandler.sendData("CheckingForChanges:" + Constants.UserClientBPort, Constants.ServerPort);
                            String msg = TransmissionHandler.receiveData(clientSocket);
                            switch (msg) {
                                case "NoChanges" -> {
                                }

                                case "Hold" -> {
                                    Thread.sleep(5000);
                                }

                                case "AcceptSyncFiles" -> {
                                    this.userClient2FileWatcher.interrupt();
                                    String serverMessage = "";
                                    String incomingFile = "";
                                    while (true) {
                                        serverMessage = TransmissionHandler.receiveData(clientSocket);
                                        if (Objects.deepEquals(serverMessage, "TransmissionComplete")) {
                                            break;
                                        } else {
                                            incomingFile = serverMessage;
                                        }
                                        TransmissionHandler.receiveFile(clientSocket, Constants.CLIENT_2_FILE_HOLDER);
                                        System.out.println(incomingFile + " received.");
                                    }
                                    this.userClient2FileWatcher = new Thread(() -> {
                                        try {
                                            FileWatcher fileWatcher = new FileWatcher(Constants.CLIENT_2_FILE_HOLDER, this);
                                            fileWatcher.start();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    });
                                    this.userClient2FileWatcher.start();
                                }

                                case "AcceptSyncModFiles" -> {
                                    this.userClient2FileWatcher.interrupt();
                                    TransmissionHandler.receiveFile(clientSocket, Constants.CLIENT_2_FILE_HOLDER);
                                    TransmissionHandler.receiveData(clientSocket);
                                    this.userClient2FileWatcher = new Thread(() -> {
                                        try {
                                            FileWatcher fileWatcher = new FileWatcher(Constants.CLIENT_2_FILE_HOLDER, this);
                                            fileWatcher.start();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    });
                                    this.userClient2FileWatcher.start();
                                }

                                case "AcceptSyncDeletedFiles" -> {
                                    this.userClient2FileWatcher.interrupt();
                                    String fileName = TransmissionHandler.receiveData(clientSocket);
                                    File toBeDeletedFile = new File(Constants.CLIENT_2_FILE_HOLDER + fileName);
                                    if (toBeDeletedFile.delete()) {
                                        this.clientFileList.remove(fileName);
                                        this.serverFileList.remove(fileName);
                                        System.out.println("Deleted the file: " + toBeDeletedFile.getName());
                                    } else {
                                        System.out.println("Failed to delete the file.");
                                    }
                                    this.userClient2FileWatcher = new Thread(() -> {
                                        try {
                                            FileWatcher fileWatcher = new FileWatcher(Constants.CLIENT_2_FILE_HOLDER, this);
                                            fileWatcher.start();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    });
                                    this.userClient2FileWatcher.start();
                                }
                            }
                            lastSyncTime = System.currentTimeMillis();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    });

    public void setTrigger(String flag) {
        trigger = flag;
    }

    public void setTrigger(String flag, String fileName) {
        modFileName = fileName;
        trigger = flag;
    }

    public static void main(String[] args) {
        UserClientB userClientB = new UserClientB();
        userClientB.userClient2Handler.start();
        userClientB.userClient2FileWatcher.start();
    }

}
