import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public class TransmissionHandler {


    public static void receiveFile(DatagramSocket serverSocket, String folder) throws IOException {
        byte[] buffer = new byte[Constants.MAX_PACKET_SIZE];

        DatagramPacket fileInfoPacket = new DatagramPacket(buffer, buffer.length);
        serverSocket.receive(fileInfoPacket);

        ByteArrayInputStream byteStream = new ByteArrayInputStream(fileInfoPacket.getData());
        DataInputStream dataInputStream = new DataInputStream(byteStream);

        String fileName = dataInputStream.readUTF();
        long fileSize = dataInputStream.readLong();

        File file = new File(folder + fileName);
        FileOutputStream fileOutputStream = new FileOutputStream(file);

        long totalBytesReceived = 0;
        int expectedPacketNumber = 0;

        while (totalBytesReceived < fileSize) {
            DatagramPacket dataPacket = new DatagramPacket(buffer, buffer.length);
            serverSocket.receive(dataPacket);

            ByteArrayInputStream packetByteStream = new ByteArrayInputStream(dataPacket.getData());
            DataInputStream packetDataInputStream = new DataInputStream(packetByteStream);

            int packetNumber = packetDataInputStream.readInt();
            byte[] packetData = new byte[dataPacket.getLength() - 4];
            packetDataInputStream.read(packetData);

            if (packetNumber == expectedPacketNumber) {
                fileOutputStream.write(packetData);
                totalBytesReceived += packetData.length;

                Utils.showProgress((int) totalBytesReceived, (int) fileSize);

                ByteBuffer ackBuffer = ByteBuffer.allocate(4);
                ackBuffer.putInt(expectedPacketNumber);
                byte[] ackBytes = ackBuffer.array();
                DatagramPacket ackPacket = new DatagramPacket(ackBytes, ackBytes.length, dataPacket.getSocketAddress());
                serverSocket.send(ackPacket);

                expectedPacketNumber++;
            }
        }
        fileOutputStream.close();
    }

    public static String receiveData(DatagramSocket serverSocket) {
        String clientMessage = null;
        try {
            byte[] receiveBuffer = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);

            while (true) {
                serverSocket.receive(receivePacket);
                clientMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
                return clientMessage;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return clientMessage;
    }

    public static void sendData(String message, int port) {
        try {
            DatagramSocket clientSocket = new DatagramSocket();
            InetAddress serverAddress = InetAddress.getByName("localhost");

            byte[] sendBuffer = message.getBytes();

            DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, serverAddress, port);

            clientSocket.send(sendPacket);
            clientSocket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendFile(String filePath, Integer port) throws IOException {
        File file = new File(filePath);
        String fileName = file.getName();
        long fileSize = file.length();

        // Create a UDP socket for data transfer
        try (DatagramSocket udpSocket = new DatagramSocket()) {
            InetAddress serverAddress = InetAddress.getByName("localhost");

            // Send file name and file size to the server
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(byteStream);
            dataOutputStream.writeUTF(fileName);
            dataOutputStream.writeLong(fileSize);

            byte[] fileInfo = byteStream.toByteArray();
            DatagramPacket fileInfoPacket = new DatagramPacket(fileInfo, fileInfo.length, serverAddress, port);
            udpSocket.send(fileInfoPacket);

            // Read file data in chunks and send them as UDP packets
            byte[] buffer = new byte[Constants.MAX_PACKET_SIZE - 8]; // Subtract 8 bytes for file name and size
            BufferedInputStream fileInputStream = new BufferedInputStream(new FileInputStream(file));

            int bytesRead;
            long totalBytesSent = 0;
            int packetNumber = 0;

            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                byte[] packetData = new byte[bytesRead];
                System.arraycopy(buffer, 0, packetData, 0, bytesRead);

                // Create a packet with packet number and data
                ByteArrayOutputStream packetByteStream = new ByteArrayOutputStream();
                DataOutputStream packetDataOutputStream = new DataOutputStream(packetByteStream);
                packetDataOutputStream.writeInt(packetNumber);
                packetDataOutputStream.write(packetData);

                byte[] packetBytes = packetByteStream.toByteArray();
                DatagramPacket dataPacket = new DatagramPacket(packetBytes, packetBytes.length, serverAddress, port);

                // Send the packet
                udpSocket.send(dataPacket);

                // Wait for acknowledgement
                byte[] ackBuffer = new byte[4];
                DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
                udpSocket.receive(ackPacket);

                int ackPacketNumber = ByteBuffer.wrap(ackPacket.getData()).getInt();
                if (ackPacketNumber == packetNumber) {
                    totalBytesSent += bytesRead;
                    Utils.showProgress((int) totalBytesSent, (int) fileSize);
                    packetNumber++;
                }
            }

            fileInputStream.close();
        }
    }
}
