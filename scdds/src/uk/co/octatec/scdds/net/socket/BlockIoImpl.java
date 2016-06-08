package uk.co.octatec.scdds.net.socket;

import uk.co.octatec.scdds.net.serialize.SerializerUtils;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Jeromy Drake on 04/05/16
 */
public class BlockIoImpl implements BlockIO {

    public static final int HEADER_LENGTH = 5; // [4:length-of-data-to-follow|1:flag]
    public static final int HEADER_FLAG_POS = 4;
    public static final int OFFSET_PAST_BLOCK_SIZE = SerializerUtils.SIZE_OF_INT;


    private Header header = new Header();

    //private byte[]  header = new byte[HEADER_LENGTH];
    private final Session sd;

    BlockIoImpl(Session sd) {
        this.sd = sd;
    }


    @Override
    public byte[] readRestOfBlock() throws IOException {
        byte[] buff = new byte[header.dataLength];
        sd.read(buff, 0, header.dataLength);
        return buff;
    }

    @Override
    public Header readBlockHeader() throws IOException {
        byte[] buff = new byte[HEADER_LENGTH];
        sd.read(buff, 0, HEADER_LENGTH);
        header.dataLength = SerializerUtils.readIntFromBytes(buff);
        header.flag = buff[HEADER_FLAG_POS];
        return header;
    }

    @Override
    public byte[] readBlock() throws IOException {
        readBlockHeader();
        return readRestOfBlock();
    }

    @Override
    public Header getCurrentHeader() {
        return header;
    }

    @Override
    public void writeDataBlock(byte[] buff, int offset, int length) throws IOException {
        writeBlockAndHeader(DATA_FLAG, buff, offset, length);
    }

    @Override
    public void writeDataBlock_HeaderSpaceIncluded(byte[] buff, int offset, int length) throws IOException  {
        int n = length - offset - HEADER_LENGTH;
        copyHeaderToBytes(buff, offset, DATA_FLAG, n);
        sd.write(buff, offset, length);
    }

    @Override
    public void writeDataBlock_HeaderSpaceIncluded(ByteBuffer buff) throws IOException {
        copyHeaderToByteBuffer(buff, DATA_FLAG);
        sd.write(buff);
    }

    @Override
    public void writeRemovalBlock_HeaderSpaceIncluded(byte[] buff, int offset, int length) throws IOException {
        //writeBlockWithHeader(DATA_REMOVAL_FLAG, buff, offset, length);
        int n = length - offset - HEADER_LENGTH;
        copyHeaderToBytes(buff, offset, DATA_REMOVAL_FLAG, n);
        sd.write(buff, offset, length);
    }

    private void writeBlockAndHeader(byte flag, byte[] buff, int offset, int length) throws IOException {
        int n = length - offset;
        writeHeader(flag, n);
        sd.write(buff, offset, length);
    }

    private void writeHeader(byte flag, int n) throws IOException {
        byte[] buff = new byte[HEADER_LENGTH];
        copyHeaderToBytes(buff, 0, flag, n);
        sd.write(buff, 0, HEADER_LENGTH);
    }

    public static void copyHeaderToBytes(byte[] b, byte flag) {
        int n = b.length - HEADER_LENGTH;
        copyHeaderToBytes(b, 0, flag, n);
    }

    public static void copyHeaderToBytes(byte[] b, int offset, byte flag, int length) {
        SerializerUtils.writeIntToBytes(length, b, offset);
        b[HEADER_FLAG_POS+offset] = flag;
    }
    public static void copyHeaderToByteBuffer(ByteBuffer b, byte flag) {
        SerializerUtils.writeIntToByteBuffer(b.limit()-HEADER_LENGTH, b, 0);
        b.put(HEADER_FLAG_POS, flag);
    }

    @Override
    public void writeString(String s)  throws IOException {
        byte[] b = s.getBytes();
        writeBlockAndHeader(STRING_FLAG, b, 0, b.length);
    }


    @Override
    public String readString() throws IOException {
        byte[] b = readBlock();
        return new String(b);
    }

    @Override
    public void sendHeartbeat() throws IOException {
        sendOneByteMessage(BlockIO.HEARTBEAT_FLAG, BlockIO.HEARTBEAT_ARG);
    }

    @Override
    public void sendActiveNotification() throws IOException {
          sendOneByteMessage(BlockIO.UN_STALE_FLAG, ACTIVE_ARG);
    }

    @Override
    public void sendStaleNotification() throws IOException {
        sendOneByteMessage(BlockIO.STALE_FLAG, STALE_ARG);
    }

    public static byte[] generateOneByteMessage(byte flag, byte value) throws IOException {
        byte[] buff = new byte[HEADER_LENGTH+1];
        SerializerUtils.writeIntToBytes(1, buff, 0);
        buff[HEADER_FLAG_POS] = flag;
        buff[HEADER_FLAG_POS+1] = value;
        return buff;
    }
    public void sendOneByteMessage(byte flag, byte value) throws IOException {
        byte[] buff = generateOneByteMessage(flag, value);
        sd.write(buff, 0, buff.length);
    }

    @Override
    public int getSizeOfHeader() {
        return HEADER_LENGTH;
    }
}
