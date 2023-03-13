import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;

public class DNSServer {
    DatagramSocket reciver;

    {
        try {
            reciver = new DatagramSocket(8053);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    DatagramSocket google;

    {
        try {
            google = new DatagramSocket(8964);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    DNSCache cache = new DNSCache();


    private void sendRequestToGoogle(DNSMessage message) throws IOException {
        InetAddress googleIP = InetAddress.getByName("8.8.8.8");
        DatagramPacket RequestGoogle = new DatagramPacket(message.getByteArrMessage(),
                message.getByteArrMessage().length,
                googleIP, 53);
        google.send(RequestGoogle);
    }

    public void server() throws IOException {
        System.out.println("waiting for connection...");

        byte[] inputBuffer = new byte[512];
        DatagramPacket inPacket = new DatagramPacket(inputBuffer, inputBuffer.length);
        reciver.receive(inPacket);

        byte[] clientBytes = new byte[inPacket.getLength()];
        System.arraycopy(inputBuffer, 0, clientBytes, 0, clientBytes.length);
        DNSMessage clientMessage = DNSMessage.decodeMessage(clientBytes);
        ArrayList<DNSRecord> outputAnswers = new ArrayList<>();

        for (DNSQuestion question : clientMessage.getQuestions()) {
            System.out.println("Requesting " + DNSMessage.joinDomainName(question.getqName()));

            DNSRecord answer = cache.querying(question);
            if (answer != null) {
                System.out.println("Question Already Asked!");
                outputAnswers.add(answer);
            }
            else {
                System.out.println("Not In Cache!");
                sendRequestToGoogle(clientMessage);
                System.out.println("Waiting for Google Response...");

                byte[] googleBuffer = new byte[512];
                DatagramPacket PacketGoogle = new DatagramPacket(googleBuffer, googleBuffer.length);
                google.receive(PacketGoogle);

                byte[] packetBytes = new byte[PacketGoogle.getLength()];
                System.arraycopy(googleBuffer, 0, packetBytes, 0, packetBytes.length);
                DNSMessage googleMessage = DNSMessage.decodeMessage(packetBytes);
                System.out.println("Caching Google Response...");

                if (googleMessage.getAnswers().length != 0) {
                    cache.insertRecord(question, googleMessage.getAnswers()[0]);
                    outputAnswers.add(googleMessage.getAnswers()[0]);
                }
            }
        }

        DNSMessage response = DNSMessage.buildResponse(clientMessage, outputAnswers.toArray(new DNSRecord[outputAnswers.size()]));
        byte[] outputBytes = response.toBytes();
        System.out.println("Sending Response To Client...");
        DatagramPacket outPacket = new DatagramPacket(outputBytes, outputBytes.length, inPacket.getAddress(), inPacket.getPort());
        reciver.send(outPacket);

    }
}



