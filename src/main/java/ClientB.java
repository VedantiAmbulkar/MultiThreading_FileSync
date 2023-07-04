import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;

public class ClientB {
    private Socket clientBSocketObj;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private File[] clientBFiles;
    private ServerSocket clientSocket;
    private String[] clientBDirectoryFiles;
    private String fileModified;

    public ClientB(int portNumber) {
        try {
            clientSocket = new ServerSocket(portNumber);
            System.out.println("Client B has been started successfully!!");

            clientBSocketObj = clientSocket.accept();
            System.out.println("Client B is connected successfully to the Server!!");

            inputStream = new DataInputStream(new BufferedInputStream(clientBSocketObj.getInputStream()));
            outputStream = new DataOutputStream(clientBSocketObj.getOutputStream());

            File pathDirectory = new File("./ClientB_dir");
            clientBFiles = pathDirectory.listFiles();

            DateFormat dateFormat = new SimpleDateFormat("dd-MMM");

            clientBDirectoryFiles = new String[clientBFiles.length];

            int counter = 0;
            for (File file : clientBFiles) {
                clientBDirectoryFiles[counter] = file.getName() + "\t" + file.length() / 1024 + "KB" + "\t" + dateFormat.format(file.lastModified());
                counter++;
            }

            try {
                PrintWriter printWriter = new PrintWriter(clientBSocketObj.getOutputStream(), true);
                printWriter.println(Arrays.toString(clientBDirectoryFiles));
                clientSocket.close();
                clientBSocketObj.close();
            } catch (IOException io) {
                System.out.println("The Server faced an input-output exception: " + io);
            }
        } catch (IOException ioMain) {
            System.out.println("Client A could not be connected to Client B due to an input-output exception: " + ioMain);
        }
    }

    public void compareFilesDirB(File[] fileDirB, String filesInDirA) throws IOException {
        if (!"false".equals(filesInDirA)) {
            int counter = 0;
            DateFormat dateFormat = new SimpleDateFormat("dd-MMM");
            String[] fileListDirA = filesInDirA.split(",");
            String[] fileDetailsDirAString = new String[fileListDirA.length];

            for (int i = 0; i < fileListDirA.length; i++) {
                fileDetailsDirAString[i] = fileListDirA[i].replaceAll("\\[", "").replaceAll("\\]", "");
                counter++;
            }
            counter = 0;
            for (File file : fileDirB) {
                for (String fileDetails : fileDetailsDirAString) {
                    if (!file.getName().equals(fileDetails.substring(fileDetails.lastIndexOf('\\') + 1))) {
                        fileModified = fileDetails;
                        Path source = file.toPath();
                        Path destination = new File("./ClientA_dir/" + file.getName()).toPath();
                        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                    }
                    counter++;
                }
            }
        }
    }

    public static void main(String[] args) {
        while (true) {
            ClientB clientBObj = new ClientB(8002);
        }
    }
}
