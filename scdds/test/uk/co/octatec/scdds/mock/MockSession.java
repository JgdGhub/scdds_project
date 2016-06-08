package uk.co.octatec.scdds.mock;
/*
  SC/DDS - simple cached data distribution service

  Copyright 2016 by Jeromy Drake

  This program is free software; you may redistribute and/or modify it under
  the terms of the GNU General Public License Version 2 as published by the
  Free Software Foundation.

  This program is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY, without even the implied warranty of MERCHANTABILITY
  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
  for complete details.
*/
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.net.serialize.SerializerUtils;
import uk.co.octatec.scdds.net.socket.BlockIO;
import uk.co.octatec.scdds.net.socket.BlockIoImpl;
import uk.co.octatec.scdds.net.socket.Session;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;

/**
 * Created by Jeromy Drake on 04/05/16
 */
public class MockSession implements Session {

    private final static Logger log = LoggerFactory.getLogger(MockSession.class);

    static final int AWAIT_SLEEP_TIME = 20;
    static final int AWAIT_LOOP_COUNT = 20;

    volatile public byte[] lastBytetWrite;
    volatile public byte[] prevLastBytetWrite;
    volatile public String lastStringWrite;
    volatile public String initialStringRead;
    volatile public ArrayList<BlockIO.Header> headerToRead = new ArrayList<>();
    volatile public ArrayList<byte[]> dataToRead = new ArrayList<>();
    int nextDataToRead = 0;
    int nextHeaderToRead = 0;
    BlockIO.Header currentHeader;

    volatile public MockBlockIo mockBlockIo =  new MockBlockIo();

    volatile Object netDisconnectFlag;

    public void enableNetDisconnectDelay() {
        netDisconnectFlag = new Object();
    }

    public void triggeNetDisconnectDelay() {
        netDisconnectFlag = new Object();
    }

    class MockBlockIo implements BlockIO {

        @Override
        public byte[] readBlock() throws IOException {
            if( nextDataToRead >= dataToRead.size()) {
                if( netDisconnectFlag != null ) {
                    log.info("--- no more data to read, an networ disconnect will follow ---");
                    synchronized (netDisconnectFlag) {
                        try {
                            log.info("---waiting for net-disconnect trigger---");
                            netDisconnectFlag.wait(5000);
                        }
                        catch( Exception e) {

                        }
                    }
                }
                log.info("---readBlock: [MOCK_NET_DICONNECT]---");
                throw new  IOException("MOCK_NET_DICONNECT");
            }
            int n = nextDataToRead;
            ++nextDataToRead;
            log.info("---readBlock:  retyrning block[{}]---", n);
            return dataToRead.get(n);
        }

        @Override
        public void writeString(String s) throws IOException {
            log.info("---writeString:  [{}]---", s);
            lastStringWrite = s;
        }

        @Override
        public String readString() throws IOException {
            log.info("---readString: returning initialStringRead [{}]---", initialStringRead);
            return initialStringRead;
        }

        @Override
        public void sendHeartbeat() throws IOException {

        }

        @Override
        public void sendStaleNotification() throws IOException {
            log.info("---sendStaleNotification---");
            prevLastBytetWrite = lastBytetWrite;
            lastBytetWrite = BlockIoImpl.generateOneByteMessage(BlockIO.STALE_FLAG , BlockIO.ACTIVE_ARG);
        }

        @Override
        public void sendActiveNotification() throws IOException {
            log.info("---sendActiveNotification---");
            prevLastBytetWrite = lastBytetWrite;
            lastBytetWrite = BlockIoImpl.generateOneByteMessage(BlockIO.UN_STALE_FLAG, BlockIO.STALE_ARG);
        }

        @Override
        public void writeDataBlock(byte[] buff, int offset, int length) throws IOException {
            log.info("---writeDataBlock:  offset={} length={}---", offset, length);
            prevLastBytetWrite = lastBytetWrite;
            lastBytetWrite = buff;
        }

        @Override
        public void writeDataBlock_HeaderSpaceIncluded(byte[] buff, int offset, int length) throws IOException {
            prevLastBytetWrite = lastBytetWrite;
            log.info("---writeDataBlock_HeaderSpaceIncluded:  offset={} length={}---", offset, length);
            int n = length - offset;
            BlockIoImpl.copyHeaderToBytes(buff, offset, DATA_FLAG, n);
            lastBytetWrite = buff; // NB - this includes N bytes of header
        }

        @Override
        public void writeDataBlock_HeaderSpaceIncluded(ByteBuffer buff) throws IOException {
            prevLastBytetWrite = lastBytetWrite;
            log.info("---writeDataBlock_HeaderSpaceIncluded:  length={}---", buff.limit());
            BlockIoImpl.copyHeaderToByteBuffer(buff, DATA_FLAG);
            //lastBytetWrite = buff.array(); // NB - this includes N bytes of header
            lastBytetWrite = new byte[buff.limit()];
            for(int i=0; i<lastBytetWrite.length; i++){
                lastBytetWrite[i]  = buff.get(i);
            }
        }

        @Override
        public void writeRemovalBlock_HeaderSpaceIncluded(byte[] buff, int offset, int length) throws IOException {
            prevLastBytetWrite = lastBytetWrite;
            log.info("---writeRemovalBlock_HeaderSpaceIncluded:  offset={} length={}---", offset, length);
            int n = length - offset;
            BlockIoImpl.copyHeaderToBytes(buff, offset, DATA_REMOVAL_FLAG, n);
            lastBytetWrite = buff; // NB - this includes N bytes of header
        }

        @Override
        public Header readBlockHeader() throws IOException {
            if( nextHeaderToRead >= headerToRead.size()) {
                try {
                    Thread.sleep(50);
                }
                catch(InterruptedException x) {

                }
                log.info("---readBlockHeader: [MOCK_EOR]---");
                throw new  IOException("MOCK-EOF");
            }
            currentHeader = headerToRead.get(nextHeaderToRead);
            log.info("---readBlockHeader:  returning header num=[{}]---", nextHeaderToRead);
            ++nextHeaderToRead;
            return currentHeader;
        }

        @Override
        public Header getCurrentHeader() {
            return currentHeader;
        }

        @Override
        public byte[] readRestOfBlock() throws IOException {
            if( nextDataToRead >= dataToRead.size()) {
                log.info("---readRestOfBlock: [MOCK_EOR]---");
                throw new  IOException("MOCK-EOF");
            }
            int n = nextDataToRead;
            ++nextDataToRead;
            log.info("---readRestOfBlock:  returning block num=[{}]---", n);
            return dataToRead.get(n);
        }

        @Override
        public int getSizeOfHeader() {
            return 5;
        }
    }


    @Override
    public void enableReadTimeout() throws IOException {

    }

    public MockSession() {

    }

    @Override
    public boolean awaitReadReady(long timeoutMs) throws IOException {
        return true;
    }

    @Override
    public void awaitReadReady() throws IOException {

    }

    @Override
    public void awaitWriteReady() throws IOException {

    }

    @Override
    public void writeInt(int n) throws IOException {

    }

    @Override
    public int readInt() throws IOException {
        return 0;
    }

    @Override
    public void write(byte[] buff, int offset, int length) throws IOException {
        log.info("---write---");
        prevLastBytetWrite = lastBytetWrite;
        lastBytetWrite = new byte[length];
        System.arraycopy(buff, offset, lastBytetWrite, 0, length);

    }

    @Override
    public void write(ByteBuffer buff) throws IOException {
        log.info("---write---");
        prevLastBytetWrite = lastBytetWrite;
        lastBytetWrite = new byte[buff.limit()];
        for(int i=0; i<lastBytetWrite.length; i++) {
            lastBytetWrite[i] = buff.get(i);
        }
        //System.arraycopy(buff.array(), 0, lastBytetWrite, 0, lastBytetWrite.length);
    }

    @Override
    public void read(byte[] buff, int offset, int length) throws IOException {
        System.arraycopy(lastBytetWrite, 0, buff, offset, length);
    }

    @Override
    public InetSocketAddress getAddress() {
        return new InetSocketAddress("localhost", 1);
    }

    @Override
    public void close() {

    }

    @Override
    public void enableWriteWaiting() throws IOException {

    }

    @Override
    public SelectionKey registerSelector(Selector selector, int selectorOp, Object attachment) {
        return null;
    }

    @Override
    public BlockIO getBlockIO() {

        return mockBlockIo;
    }

    public void awaitLastStringWrite() throws InterruptedException{
        for (int i = 0; i < AWAIT_LOOP_COUNT; i++)    {
            // wait for the cache publisher to write a reply
            if ( lastStringWrite != null) {
                break;
            }
            Thread.sleep(AWAIT_SLEEP_TIME);
        }
        if( lastStringWrite == null  ) {
            log.warn("*** awaitLastStringWrite: wait failed");
        }
    }
    public void awaitLastByteWrite() throws InterruptedException{
        for (int i = 0; i < AWAIT_LOOP_COUNT; i++)    {
            // wait for the cache publisher to write a reply
            if ( lastBytetWrite != null) {
                break;
            }
            Thread.sleep(AWAIT_SLEEP_TIME);
        }
        if( lastBytetWrite == null  ) {
            log.warn("*** awaitLastByteWrite: wait failed");
        }
    }
}