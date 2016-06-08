package uk.co.octatec.scdds.net.socket;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Jeromy Drake on 04/05/16
 */
public interface BlockIO {

    static final byte DATA_FLAG = (byte)'D';
    static final byte DATA_REMOVAL_FLAG = (byte)'R';
    static final byte STRING_FLAG = (byte)'S';
    static final byte HEARTBEAT_FLAG = (byte)'H';
    static final byte INITIAL_UPDATE_FLAG = (byte)'I';
    static final byte INITIAL_UPDATE_COMPLETED_FLAG = (byte)'C';
    static final byte STALE_FLAG = (byte)'-';
    static final byte UN_STALE_FLAG = (byte)'+';

    public static final byte STALE_ARG  = (byte)'s';
    public static final byte ACTIVE_ARG = (byte)'a';

    public static final byte HEARTBEAT_ARG = (byte)'h';

    public static class Header {
        public int dataLength;
        public byte flag;
    }

    byte[] readBlock() throws IOException;

    void writeDataBlock(byte[] buff, int offset, int length) throws IOException;
    void writeDataBlock_HeaderSpaceIncluded(byte[] buff, int offset, int length) throws IOException;
    void writeDataBlock_HeaderSpaceIncluded(ByteBuffer buff) throws IOException;

    void writeRemovalBlock_HeaderSpaceIncluded(byte[] buff, int offset, int length) throws IOException;

    void writeString(String s)  throws IOException;

    String readString() throws IOException;

    void sendHeartbeat() throws IOException;

    void sendStaleNotification() throws IOException;

    void sendActiveNotification() throws IOException;

    Header readBlockHeader() throws IOException;
    byte[] readRestOfBlock()throws IOException;

    Header getCurrentHeader();

    int getSizeOfHeader();
}
