import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.io.IOException;
import java.nio.ByteBuffer;

public class DNSHeader {
    int headerID; //important!
    byte QR; //important!
    byte opcode; //important!
    byte AA;
    byte TC;
    byte RD;
    byte RA;
    byte Z;
    byte rcode; //important!
    int qdCount;
    int anCount;
    int nsCount;
    int arCount;

    public static DNSHeader decodeHeader(ByteArrayInputStream inputStream) throws IOException {
        DNSHeader header= new DNSHeader();

        byte[] rawID = inputStream.readNBytes(2);
        int tempID = 0;
        for (byte b : rawID) {
            tempID = (tempID << 8) | (b & 0xFF);
        }

        header.headerID = tempID;

        byte[] rawQROpcodeETC = inputStream.readNBytes(1);
        byte rawQROpcode = rawQROpcodeETC[0];

//        header.QR = (byte) ((rawQROpcode & 0x8//        header.QR = (byte) ((rawQROpcode & 0x80) >>> 7);0) >>> 7);
        header.QR = (byte) ((rawQROpcode >> 7) & 0x1);
        byte rawOpcode = (byte) (rawQROpcode << 1);
        header.opcode = (byte) (rawOpcode >> 4);

        byte rawOpcodeAATCRD = (byte) (rawQROpcode << 5);
        rawOpcodeAATCRD = (byte) (rawOpcodeAATCRD >> 5);

        header.AA = (byte) (rawOpcodeAATCRD >> 2 & 0x1);
        header.TC = (byte) ((byte) (rawOpcodeAATCRD & 0b00000010) >> 1);
        header.RD = (byte) (rawOpcodeAATCRD & 0x1);;

        byte[] rawRaZRcode = inputStream.readNBytes(1);
        byte halfRawRaZRcode = Array.getByte(rawRaZRcode, 0);

        header.RA = (byte) (halfRawRaZRcode >> 7 & 0x1);
        header.Z = (byte) ((halfRawRaZRcode & 0b01110000) >> 4);
        header.rcode = (byte) (halfRawRaZRcode & 0b00001111);


        byte[] rawQD = inputStream.readNBytes(2);
        int tempQD = 0;
        for (byte b : rawQD) {
            tempQD = (tempQD << 8) | (b & 0xFF);
        }
        header.qdCount = tempQD;

        byte[] rawAN = inputStream.readNBytes(2);
        int tempAN = 0;
        for (byte b : rawAN) {
            tempAN =  (tempAN << 8) | (b & 0xFF);
        }
        header.anCount = tempAN;

        byte[] rawNS = inputStream.readNBytes(2);
        int tempNS = 0;
        for (byte b : rawNS) {
            tempNS = (tempNS << 8) | (b & 0xFF);
        }
        header.nsCount = tempNS;

        byte[] rawAR = inputStream.readNBytes(2);
        int tempAR = 0;
        for (byte b : rawAR) {
            tempAR = (tempAR << 8) | (b & 0xFF);
        }
        header.arCount = tempAR;

//        System.out.println(Integer.toHexString(header.headerID));
//        System.out.println(header.QR);
//        System.out.println(header.opcode);
//        System.out.println(header.AA);
//        System.out.println(header.TC);
//        System.out.println(header.RD);
//        System.out.println(header.RA);
//        System.out.println(header.Z);
//        System.out.println(header.rcode);
//        System.out.println(header.qdCount);
//        System.out.println(header.anCount);
//        System.out.println(header.nsCount);
//        System.out.println(header.arCount);

        return header;
    }
    /*
     This will create the header for the response. It will copy some fields from the request
     */
    static DNSHeader buildHeaderForResponse(DNSMessage request, DNSMessage response) {
        DNSHeader reHeader = new DNSHeader();

        reHeader.headerID = request.header.headerID;
        reHeader.QR = 1;
        reHeader.opcode = request.header.opcode;
        reHeader.AA = request.header.AA;
        reHeader.TC = request.header.TC;
        reHeader.RD = request.header.RD;
        reHeader.RA = request.header.RA;
        reHeader.Z =  request.header.Z;
        reHeader.rcode = request.header.rcode;
        reHeader.qdCount = response.questions.length;
        reHeader.anCount = response.answers.length;
        reHeader.nsCount = response.authorityRecords.length;
        reHeader.arCount = response.additionalRecords.length;


        return reHeader;
    }

    byte[] intToBytes(int value) {
        return new byte[]{(byte) (value >>> 8 & 0xff), (byte) (value & 0xff)};
    }

    /*
    Encode the header to bytes to be sent back to the client.
    The OutputStream interface has methods to write a single byte or an array of bytes.
 */
    void writeBytes(OutputStream output) throws IOException {
        output.write(intToBytes(headerID));
        //third byte
        byte outQR = (byte) ((QR << 7) & 0xff);
        byte outOpcode = (byte) ((opcode << 3) & 0xff);
        byte outAA = (byte) ((AA << 2) & 0xff);
        byte outTC = (byte) ((TC << 1) & 0xff);
        byte outRD = (byte) (RD & 0xff);
        
        byte thirdByte = (byte) (outQR|outOpcode|outAA|outTC|outRD);
//        byte mushRoom = (byte)
        output.write(thirdByte);

        byte outRA = (byte) ((RA << 7) & 0xff);
        byte outZ = (byte) ((Z << 5) & 0xff);
        byte outRcode = (byte) ((rcode << 4) & 0xff);

        byte fourthByte = (byte) (outRA|outZ|outRcode);
        output.write(fourthByte);
//        for()
//        output.write(QR);
//        output.write(opcode);
//        output.write(AA);
//        output.write(TC);
//        output.write(RD);
//
//        byte[] secondFlag = {RA, Z, rcode};

        //fourth byte
//        output.write(RA);
//        output.write(Z);
//        output.write(rcode);

        output.write(intToBytes(qdCount));
        output.write(intToBytes(anCount));
        output.write(intToBytes(nsCount));
        output.write(intToBytes(arCount));
    }


    //cite convert int to byte method from https://javadeveloperzone.com/java-basic/java-convert-int-to-byte-array/
//    byte[] intToBytes(int i) {
//        ByteBuffer bb = ByteBuffer.allocate(4);
//        bb.putInt(i);
//        return bb.array();
//    }
    public int getQdCount() {
        return qdCount;
    }

    public int getAnCount() {
        return anCount;
    }

    public int getNsCount() {
        return nsCount;
    }

    public int getArCount() {
        return arCount;
    }

    @Override
    public String toString(){
        return "The infomation on this DNS Header:\n" + "ID: " + headerID + "\n"
                + "QR: " + QR + "\n"
                + "OPCODE: " + opcode + "\n"
                + "AA: " + AA + "\n"
                + "TC: " + TC + "\n"
                + "RD: " + RD + "\n"
                + "RA: " + RA + "\n"
                + "Z: " + Z + "\n"
                + "RCODE: " + rcode + "\n"
                + "QDCOUNT: " + qdCount + "\n"
                + "ANCOUNT: " + anCount + "\n"
                + "NSCOUNT: " + nsCount + "\n"
                + "ARCOUNT: " + arCount + "\n";
    }
}