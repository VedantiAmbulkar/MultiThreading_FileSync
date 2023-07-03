import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;

public class Server {
    private static final int UDP_PORT = 8000;
    private static final int MAX_PACKET_SIZE = 2000; // Maximum packet size for your network

    public static void main(String[] args) {
        try {
            startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void startServer() throws IOException {
        DatagramSocket udpSocket = new DatagramSocket(UDP_PORT);
        byte[] buffer = new byte[MAX_PACKET_SIZE];

        while (true) {
            DatagramPacket fileInfoPacket = new DatagramPacket(buffer, buffer.length);
            udpSocket.receive(fileInfoPacket);

            ByteArrayInputStream byteStream = new ByteArrayInputStream(fileInfoPacket.getData());
            DataInputStream dataInputStream = new DataInputStream(byteStream);

            String fileName = dataInputStream.readUTF();
            long fileSize = dataInputStream.readLong();

            File file = new File("serverFileHolder/" + fileName); // Replace with the desired save path
            FileOutputStream fileOutputStream = new FileOutputStream(file);

            long totalBytesReceived = 0;
            int expectedPacketNumber = 0;

            while (totalBytesReceived < fileSize) {
                DatagramPacket dataPacket = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(dataPacket);

                ByteArrayInputStream packetByteStream = new ByteArrayInputStream(dataPacket.getData());
                DataInputStream packetDataInputStream = new DataInputStream(packetByteStream);

                int packetNumber = packetDataInputStream.readInt();
                byte[] packetData = new byte[dataPacket.getLength() - 4]; // Subtract 4 bytes for packet number
                packetDataInputStream.read(packetData);

                if (packetNumber == expectedPacketNumber) {
                    fileOutputStream.write(packetData);
                    totalBytesReceived += packetData.length;
                    System.out.println("Received " + totalBytesReceived + " bytes out of " + fileSize + " bytes");

                    // Send acknowledgement for the received packet
                    ByteBuffer ackBuffer = ByteBuffer.allocate(4);
                    ackBuffer.putInt(expectedPacketNumber);
                    byte[] ackBytes = ackBuffer.array();
                    DatagramPacket ackPacket = new DatagramPacket(ackBytes, ackBytes.length, dataPacket.getSocketAddress());
                    udpSocket.send(ackPacket);

                    expectedPacketNumber++;
                }
            }

            System.out.println("File transfer complete");

            fileOutputStream.close();
        }
    }
}
