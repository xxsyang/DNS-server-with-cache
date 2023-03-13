import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class DNSMessage {
    DNSHeader header;
    byte[] byteArrMessage;
    DNSQuestion[] questions;
    DNSRecord[] answers;
    DNSRecord[] authorityRecords;
    DNSRecord[] additionalRecords;

    static DNSMessage decodeMessage(byte[] inputBytes) throws IOException {
        DNSMessage message = new DNSMessage();
        message.byteArrMessage = inputBytes;
        ByteArrayInputStream byteStream = new ByteArrayInputStream(inputBytes);
        message.header = DNSHeader.decodeHeader(byteStream);

        message.questions = new DNSQuestion [message.header.getQdCount()];
        for (int i = 0; i < message.questions.length; i++) {
            message.questions[i] = DNSQuestion.decodeQuestion(byteStream, message);
        }

        message.answers = new DNSRecord [message.header.getAnCount()];
        for (int i = 0; i < message.answers.length; i++) {
            message.answers[i] = DNSRecord.decodeRecord(byteStream, message);
        }

        message.authorityRecords = new DNSRecord [message.header.getNsCount()];
        for (int i = 0; i < message.authorityRecords.length; i++) {
            message.authorityRecords[i] = DNSRecord.decodeRecord(byteStream, message);
        }

        message.additionalRecords = new DNSRecord [message.header.getArCount()];
        for (int i = 0; i < message.additionalRecords.length; i++) {
            message.additionalRecords[i] = DNSRecord.decodeRecord(byteStream, message);
        }

        return message;
    }

    /*
    read a question from the input stream. Due to compression, you may have to
    ask the DNSMessage containing this question to read some of the fields.
    cited idea from https://levelup.gitconnected.com/dns-response-in-java-a6298e3cc7d9
     */
    public String[] readDomainName(InputStream input) throws IOException {
        ArrayList<byte[]> labels = new ArrayList<>();
        byte[] octet = input.readNBytes(1);
        while(octet[0] != 0){
            byte[] eachLabel = input.readNBytes(octet[0]);
            labels.add(eachLabel);
            octet = input.readNBytes(1);
        }

        //convert byte into character string
        String[] DomainNameStrings = new String[labels.size()];
        for(int i = 0; i < labels.size(); i++){
            DomainNameStrings[i] = new String(labels.get(i), StandardCharsets.UTF_8);
        }

        return DomainNameStrings;
    }

    // compressed
    String[] readDomainName(int firstByte) throws IOException {
        return readDomainName(new ByteArrayInputStream(byteArrMessage, firstByte,
                byteArrMessage.length-firstByte));
    }

    //build a response based on the request and the answers you intend to send back.
    static DNSMessage buildResponse(DNSMessage request, DNSRecord[] answers){
        DNSMessage response = new DNSMessage();
        response.questions = request.getQuestions();
        response.answers = answers;
        response.authorityRecords = request.getAuthorityRecords();
        response.additionalRecords = request.getAdditionalRecords();

        response.header = DNSHeader.buildHeaderForResponse(request, response);

        return response;
    }
//
    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        header.writeBytes(output);
        HashMap<String,Integer> domainNameLocations = new HashMap<>();
        for (DNSQuestion question : questions) {
            question.writeBytes(output, domainNameLocations);
        }
        for (DNSRecord record : answers) {
            record.writeBytes(output, domainNameLocations);
        }
        for (DNSRecord record : authorityRecords) {
            record.writeBytes(output, domainNameLocations);
        }
        for (DNSRecord record : additionalRecords) {
            record.writeBytes(output, domainNameLocations);
        }
        return output.toByteArray();

    }
/*
 If this is the first time we've seen this domain name in the packet, write it using the DNS encoding
 (each segment of the domain prefixed with its length, 0 at the end), and add it to the hash map.
 Otherwise, write a back pointer to where the domain has been seen previously.
 */
    public static void writeDomainName(ByteArrayOutputStream output, HashMap<String,Integer> domainMap,
                                       String[] domainPieces) throws IOException {
        String domainNameKey = joinDomainName(domainPieces);
        if (domainMap.containsKey(domainNameKey)) {
            int intPointer = domainMap.get(domainNameKey);
            byte secondByte = (byte) intPointer;
            intPointer >>= 8;
            byte firstByte = (byte) intPointer;
            byte mask = (byte) 0xc0;
            firstByte |= mask;

            output.write(firstByte);
            output.write(secondByte);

        } else {
            domainMap.put(domainNameKey, output.size());

            for (int i = 0; i < domainPieces.length; i++) {
                output.write(domainPieces[i].length());
                for (char c : domainPieces[i].toCharArray()) {
                    output.write(c);
                }
            }
            output.write(0);
        }
    }

    static String joinDomainName(String[] pieces) {
        StringBuilder fullName = new StringBuilder();
        for (int i = 0; i < pieces.length; i++) {
            fullName.append(pieces[i]);
            if (i < pieces.length - 1) {
                fullName.append('.');
            }
        }
        return fullName.toString();
    }

    public DNSRecord[] getAnswers() {
        return answers;
    }

    public DNSRecord[] getAdditionalRecords() {
        return additionalRecords;
    }

    public DNSRecord[] getAuthorityRecords() {
        return authorityRecords;
    }

    public DNSQuestion[] getQuestions() {
        return questions;
    }

    public byte[] getByteArrMessage() {
        return byteArrMessage;
    }

    @Override
    public String toString() {
        return "DNSMessage{" +
                "header=" + header +
                ", byteArrMessage=" + Arrays.toString(byteArrMessage) +
                ", questions=" + Arrays.toString(questions) +
                ", answers=" + Arrays.toString(answers) +
                ", authorityRecords=" + Arrays.toString(authorityRecords) +
                ", additionalRecords=" + Arrays.toString(additionalRecords) +
                '}';
    }
}
