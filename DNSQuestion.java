import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
public class DNSQuestion {

    String[] qName;
    byte[] qType;
    byte[] qClass;

    static DNSQuestion decodeQuestion(InputStream input, DNSMessage message) throws IOException {
        DNSQuestion question = new DNSQuestion();

        question.qName = message.readDomainName(input);
        question.qType = input.readNBytes(2);
        question.qClass = input.readNBytes(2);

        return question;
    }

    /*
    write the question bytes which will be sent to the client. The hash map is used for us to compress the message,
    see the DNSMessage class below.
     */
    void writeBytes(ByteArrayOutputStream byteArrOutPut, HashMap<String,Integer> domainNameLocations) throws IOException {
        DNSMessage.writeDomainName(byteArrOutPut, domainNameLocations, qName);
        byteArrOutPut.write(qType);
        byteArrOutPut.write(qClass);
    }

    public String[] getqName() {
        return qName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DNSQuestion that)) return false;
        return Arrays.equals(qName, that.qName) && Arrays.equals(qType, that.qType) && Arrays.equals(qClass, that.qClass);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(qName);
        result = 31 * result + Arrays.hashCode(qType);
        result = 31 * result + Arrays.hashCode(qClass);
        return result;
    }
    @Override
    public String toString() {
        return "DNSQuestion{" +
                "qName = " + Arrays.toString(qName) +
                ", qType = " + Arrays.toString(qType) +
                ", qClass = " + Arrays.toString(qClass) +
                '}';
    }
}


