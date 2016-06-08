package uk.co.octatec.scdds.net.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static uk.co.octatec.scdds.ConditionalCompilation._DBG;
import uk.co.octatec.scdds.net.serialize.SerializerUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * Created by Jeromy Drake on 30/04/2016.
 */
public class SessionImpl implements Session {

    private final Logger log = LoggerFactory.getLogger(SessionImpl.class);

    private final SocketChannel sc;
    private final InetSocketAddress addr;
    private final BlockIO blockIO;

    private SingleReadSelector singleReadSelector;
    private SingleWriteSelector singleWriteSelector;
    private final String name;

    public SessionImpl(SocketChannel sc, InetSocketAddress addr, String name) {
        this.sc = sc;
        this.addr = addr;
        blockIO = new BlockIoImpl(this);
        this.name = name;
    }


    @Override
    public String toString() {
        return "["+name+";Session:"+addr+";blocking?"+sc.isBlocking()+";connected?"+sc.isConnected()+"]";

    }

    @Override
    public void enableReadTimeout() throws IOException {
        if( singleReadSelector == null ) {
            singleReadSelector = new SingleReadSelector(this, name);
        }
    }

    @Override
    public void enableWriteWaiting() throws IOException {
        if( singleWriteSelector == null ) {
            singleWriteSelector = new SingleWriteSelector(this, name);
        }
    }

    @Override
    public void awaitWriteReady() throws IOException {
        singleWriteSelector.awaitReady();
    }

    @Override
    public void awaitReadReady()throws IOException {
        singleReadSelector.awaitReady();
    }

    @Override
    public boolean awaitReadReady(long timeoutMs) throws IOException {
        return singleReadSelector.awaitReady(timeoutMs);
    }


    //public void setReadTimeout(int timeoutMs) throws IOException  {
        // NOT SUPPORTED ON SocketChannels
    //    log.info("setReadTimeout [{}ms]", timeoutMs);
    //    sc.socket().setSoTimeout(timeoutMs);
    //}

    @Override
    public void writeInt(int n) throws IOException {
        byte[] b = new byte[4];
        SerializerUtils.writeIntToBytes(n, b);
        write(b, 0, 4);
    }

    @Override
    public int readInt()  throws IOException {
        byte[] b = new byte[4];
        read(b, 0, 4);
        return SerializerUtils.readIntFromBytes(b);
    }

    //@Override
    //public void writeDataBlock(byte[] buff, int offset, int length) throws IOException {
    //    writeBlockWithHeader(DATA_FLAG, buff, offset, length);
    //}


    @Override
    public void write(byte[] buff, int offset, int length) throws IOException {
        ByteBuffer b = ByteBuffer.wrap(buff, offset, length);
        write(b);
    }

    @Override
    public void write(ByteBuffer b) throws IOException {
        long sleepTime = 5;
        while( b.remaining() > 0 ) {
            int n = sc.write(b);
            if( _DBG ) {
                log.debug("{}: byte count written [{}], length to write [{}], remaining [{}]", name, n, b.limit(), b.remaining());
            }
            if( n == 0 ) {
                // nothing could be written, sleep a short while to let the o/s network buffers drain
                if( singleWriteSelector != null ) {
                    singleWriteSelector.awaitReady();
                }
                else {
                    try {
                        Thread.sleep(sleepTime);
                        if (sleepTime < 100) {
                            sleepTime += 5;
                        }
                    } catch (InterruptedException e) {
                        throw new IOException("interrupted while waiting to write", e);
                    }
                }
            }
            else if( n == -1 ) {
                throw new IOException("remote closed connection");
            }
        }
    }

    @Override
    public void read(byte[] buff, int offset, int length) throws IOException {
        int lengthToRead =  length-offset;
        ByteBuffer b =  ByteBuffer.allocate(lengthToRead);
        if( _DBG ) {
            log.debug("{}: read buff-size={}, offset={} length={} lengthToRead={} b.remaining={}", name, buff.length, offset, length, lengthToRead, b.remaining());
        }
        long sleepTime = 5;
        while( b.remaining() > 0 ) {
            int n = sc.read(b);
            if( _DBG ) {
                log.debug("{}: byte count read [{}], total length to read [{}], remaining [{}]", name, n, lengthToRead, b.remaining());
            }
            if( n == 0 ) {
                // nothing could be read, sleep a short while, the expectation is that we are
                // reading because we have been notified that data is available
                try {
                    Thread.sleep(sleepTime);
                    if( sleepTime < 100 ) {
                        sleepTime += 5;
                    }
                }
                catch( InterruptedException e) {
                    throw new  IOException(name+": interrupted while waiting to read", e);
                }
            }
            else if( n == -1 ) {
                throw new IOException(name+": remote closed connection");
            }
        }
        // now copy the buffer into the input array
        b.flip();
        for(int i=offset; i<length; i++) {
            buff[i] = b.get();
        }
        if( _DBG ) {
            log.debug("{}: READ [{}]", name, buff);
        }
    }

    @Override
    public void close() {
        if( singleReadSelector != null ) {
            singleReadSelector.dispose();
        }
        try {
            sc.close();
        }
        catch(IOException e) {
            log.warn("{}: exception closing socket channel [{}] [{}]", name, addr, e.getMessage());
        }
    }

    @Override
    public BlockIO getBlockIO() {
        return blockIO;
    }

    @Override
    public InetSocketAddress getAddress() {
        return addr;
    }

    @Override
    public SelectionKey registerSelector(Selector selector, int selectorOp, Object attachment) {
        // op should be SelectionKey.OP_READ or SelectionKey.OP_WRITE
        try {
            log.info("{}: registered selector op=[{}] sc=[{}] selector=[{}] attachment=[{}]", name, selectorOp, sc, selector, attachment);
            return attachment==null? sc.register(selector, selectorOp) : sc.register(selector, selectorOp, attachment);
        }
        catch( ClosedChannelException x) {
            log.error(name+": ClosedChannelException while registering selector [{]]",selectorOp, x);
            return null;
        }
    }
}
