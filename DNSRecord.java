import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;

/*
    Everything after the header and question parts of the DNS message are stored as records.
This should have all the fields listed in the spec as well as a Date object storing
when this record was created by your program.
 */
public class DNSRecord {
    String[] name;
    byte[] type;
    byte[] Class;
    int TTL; //unsign
    int rdLength; //unsign
    byte[] RData;
    LocalDateTime deadTime;

    static DNSRecord decodeRecord(InputStream input, DNSMessage message) throws IOException {
        DNSRecord record = new DNSRecord();
        input.mark(2);
        byte[] rawName = input.readNBytes(2);

        if(((rawName[0] >> 6) & 0b00000011) == 3){
            int offset = rawName[1] & 0xff;
            record.name = message.readDomainName(offset);
        }
        else{
            input.reset();
            record.name = message.readDomainName(input);
        }

        record.type = input.readNBytes(2);
        record.Class = input.readNBytes(2);

        byte[] rawTTL = input.readNBytes(4);
        int tempTTL = 0;
        for (byte b : rawTTL) {
            tempTTL = (tempTTL << 8) + (b & 0xFF);
        }
        record.TTL = tempTTL;

        byte[] rawRdLength = input.readNBytes(2);
        int tempRdLength = 0;
        for (byte b : rawRdLength) {
            tempRdLength = (tempRdLength << 8) + (b & 0xFF);
        }
        record.rdLength = tempRdLength;

        record.RData = input.readNBytes(record.rdLength);

        record.deadTime = LocalDateTime.now().plusSeconds(record.TTL);

        return record;
    }

    public void writeBytes(ByteArrayOutputStream output, HashMap<String, Integer> domainMap) throws IOException {
        DNSMessage.writeDomainName(output, domainMap, name);

        output.write(type);
        output.write(Class);
        byte[] tempTTL = {(byte) (rdLength >>> 24 & 0xff),(byte) (rdLength >>> 16 & 0xff), (byte) (rdLength >>> 8 & 0xff),
                (byte) (rdLength & 0xff)};
        output.write(tempTTL);
        byte[] temp = {(byte) (rdLength >>> 8 & 0xff), (byte) (rdLength & 0xff)};
        output.write(temp);
        output.write(RData);
    }

    boolean isExpired() {
        if (deadTime.isAfter(LocalDateTime.now())) {
            System.out.println("Record expires at: " + deadTime);
            return true;
        }
        else {
            System.out.println("Record is expired!");
            return false;
        }
    }

    @Override
    public String toString() {
        return "DNSRecord{" +
                "name=" + Arrays.toString(name) +
                ", type=" + Arrays.toString(type) +
                ", Class=" + Arrays.toString(Class) +
                ", TTL=" + TTL +
                ", rdLength=" + rdLength +
                ", RData=" + Arrays.toString(RData) +
                ", deadTime=" + deadTime +
                '}';
    }
}
