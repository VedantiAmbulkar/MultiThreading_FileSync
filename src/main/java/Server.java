import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class Server {

    private static final Logger log = LoggerFactory.getLogger(Server.class);

    private static DataOutputStream dataOutputStream = null;
    private static DataInputStream dataInputStream = null;

    public static void startServer(int portNumber) {
        try(ServerSocket serverSocket = new ServerSocket(portNumber)){
            log.info("Listening to client connections on http://localhost:{} ...", portNumber);

            Socket clientSocket = serverSocket.accept();
            log.info("Connection request made to server ...");

            dataInputStream = new DataInputStream(clientSocket.getInputStream());
            dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());

            receiveFile("serverFileHolder/NewFile1.txt");

            dataInputStream.close();
            dataOutputStream.close();
            clientSocket.close();

        } catch (Exception e){
            log.error("Connection failed with Error : {}", e.getMessage());
        }
    }

    private static void receiveFile(String fileName) throws Exception{
        int bytes = 0;
        FileOutputStream fileOutputStream = new FileOutputStream(fileName);

        long size = dataInputStream.readLong();     // read file size
        byte[] buffer = new byte[4*1024];
        while (size > 0 && (bytes = dataInputStream.read(buffer, 0, (int)Math.min(buffer.length, size))) != -1) {
            fileOutputStream.write(buffer,0,bytes);
            size -= bytes;      // read upto file size
        }
        fileOutputStream.close();
    }

    public static void main(String[] args) {
        startServer(8000);
    }
}
