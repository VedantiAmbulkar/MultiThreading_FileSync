import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;

public class ClientB {
    private static final int UDP_PORT = 8000;
    private static final int MAX_PACKET_SIZE = 2000; // Maximum packet size for your network

    public static void main(String[] args) {
        try {
            String filePath = "clientFileHolder/text_small.txt"; // Replace with the actual file path
            sendFile(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendFile(String filePath) throws IOException {
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
            DatagramPacket fileInfoPacket = new DatagramPacket(fileInfo, fileInfo.length, serverAddress, UDP_PORT);
            udpSocket.send(fileInfoPacket);

            // Read file data in chunks and send them as UDP packets
            byte[] buffer = new byte[MAX_PACKET_SIZE - 8]; // Subtract 8 bytes for file name and size
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
                DatagramPacket dataPacket = new DatagramPacket(packetBytes, packetBytes.length, serverAddress, UDP_PORT);

                // Send the packet
                udpSocket.send(dataPacket);

                // Wait for acknowledgement
                byte[] ackBuffer = new byte[4];
                DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
                udpSocket.receive(ackPacket);

                int ackPacketNumber = ByteBuffer.wrap(ackPacket.getData()).getInt();
                if (ackPacketNumber == packetNumber) {
                    totalBytesSent += bytesRead;
                    System.out.println("Sent " + totalBytesSent + " bytes out of " + fileSize + " bytes");
                    packetNumber++;
                }
            }

            System.out.println("File transfer complete");

            fileInputStream.close();
        }
    }
}
