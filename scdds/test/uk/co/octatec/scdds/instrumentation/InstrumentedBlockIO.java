package uk.co.octatec.scdds.instrumentation;
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
import uk.co.octatec.scdds.net.socket.BlockIO;
import uk.co.octatec.scdds.utilities.AwaitParams;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Jeromy Drake on 12/05/2016.
 */
public class InstrumentedBlockIO implements BlockIO {

    private final Logger log = LoggerFactory.getLogger(InstrumentedBlockIO.class);

    final BlockIO delegate;

    volatile int heartbeatCount;
    volatile int initialUpdateCompleteCount;
    volatile int initialUpdateCount;
    volatile int dataCount;
    volatile int staleCount;
    volatile int unStaleCount;
    volatile int dataRemovalCount;

    volatile int heartbeatSentCount;

    volatile boolean dropHeartbeats;

    String type;

    public void awaitHeartbeatCount(int n) throws InterruptedException {
        for(int i=0; i< AwaitParams.AWAIT_LOOP_COUNT; i++) {
            if( heartbeatCount>= n ) {
                break;
            }
            Thread.sleep(AwaitParams.AWAIT_SLEEP_TIME);
        }
    }

    public void setDropHeartbeats(boolean dropHeartbeats) {
        log.info("setDropHeartbeats [{}] [{}] #={}", type, dropHeartbeats, System.identityHashCode(this));
        this.dropHeartbeats = dropHeartbeats;
    }

    public int getHeartbeatSentCount() {
        return heartbeatSentCount;
    }

    public int getDataRemovalCount() {
        return dataRemovalCount;
    }

    public int getUnStaleCount() {
        return unStaleCount;
    }

    public int getStaleCount() {
        return staleCount;
    }

    public int getDataCount() {
        return dataCount;
    }

    public int getInitialUpdateCount() {
        return initialUpdateCount;
    }

    public int getInitialUpdateCompleteCount() {
        return initialUpdateCompleteCount;
    }

    public int getHeartbeatCount() {
        return heartbeatCount;
    }

    public InstrumentedBlockIO(BlockIO delegate, String type) {
        this.delegate = delegate;
        this.type = type;
    }

    @Override
    public byte[] readBlock() throws IOException {
        return delegate.readBlock();
    }

    @Override
    public void writeDataBlock(byte[] buff, int offset, int length) throws IOException {
        delegate.writeDataBlock(buff, offset, length);
    }

    @Override
    public void writeDataBlock_HeaderSpaceIncluded(byte[] buff, int offset, int length) throws IOException {
        delegate.writeDataBlock_HeaderSpaceIncluded(buff, offset, length);
    }

    @Override
    public void writeRemovalBlock_HeaderSpaceIncluded(byte[] buff, int offset, int length) throws IOException {
        delegate.writeRemovalBlock_HeaderSpaceIncluded(buff, offset, length);
    }

    @Override
    public void writeDataBlock_HeaderSpaceIncluded(ByteBuffer buff) throws IOException {
        delegate.writeDataBlock_HeaderSpaceIncluded(buff);
    }

    @Override
    public void writeString(String s) throws IOException {
        delegate.writeString(s) ;
    }

    @Override
    public String readString() throws IOException {
        return delegate.readString();
    }

    @Override
    public void sendHeartbeat() throws IOException {
        if( dropHeartbeats ) {
            log.info("*** [{}] DROPING HEARTBEAT *** #={}", type, System.identityHashCode(this));
        }
        else {
            log.info("*** [{}] SENDING HEARTBEAT *** #={}", type, System.identityHashCode(this));
            ++heartbeatSentCount;
            delegate.sendHeartbeat();
        }
    }

    @Override
    public void sendStaleNotification() throws IOException {
        delegate.sendStaleNotification();
    }

    @Override
    public void sendActiveNotification() throws IOException {
        delegate.sendActiveNotification();
    }

    @Override
    public Header readBlockHeader() throws IOException {
        Header h = delegate.readBlockHeader();
        if( h.flag == BlockIO.HEARTBEAT_FLAG) {
            ++heartbeatCount;
        }
        else if( h.flag == BlockIO.INITIAL_UPDATE_COMPLETED_FLAG) {
            ++initialUpdateCompleteCount;
        }
        else if( h.flag == BlockIO.INITIAL_UPDATE_FLAG) {
            ++initialUpdateCount;
        }
        else if( h.flag == BlockIO.DATA_FLAG) {
            ++dataCount;
        }
        else if( h.flag == BlockIO.STALE_FLAG) {
            ++staleCount;
        }
        else if( h.flag == BlockIO.UN_STALE_FLAG) {
            ++unStaleCount;
        }
        else if( h.flag == BlockIO.DATA_REMOVAL_FLAG) {
            ++dataRemovalCount;
        }
        return h;
    }

    @Override
    public byte[] readRestOfBlock() throws IOException {
        return delegate.readRestOfBlock();
    }

    @Override
    public Header getCurrentHeader() {
        return delegate.getCurrentHeader();
    }

    @Override
    public int getSizeOfHeader() {
        return delegate.getSizeOfHeader();
    }
}
