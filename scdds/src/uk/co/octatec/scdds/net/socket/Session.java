package uk.co.octatec.scdds.net.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * Created by Jeromy Drake on 30/04/2016.
 */
public interface Session {
    SelectionKey registerSelector(Selector selector, int selectorOp, Object attachment) ;
    void enableReadTimeout() throws IOException ;
    void enableWriteWaiting() throws IOException ;
    void awaitReadReady()throws IOException ;
    void awaitWriteReady() throws IOException;
    boolean awaitReadReady(long timeoutMs)throws IOException ;
    void writeInt(int n) throws IOException ;
    int readInt() throws IOException ;
    void write(byte[] buff, int offset, int length) throws IOException ;
    void write(ByteBuffer buff) throws IOException ;
    void read(byte[] buff, int offset, int length) throws IOException ;
    InetSocketAddress getAddress();
    void close();
    BlockIO getBlockIO();

}
