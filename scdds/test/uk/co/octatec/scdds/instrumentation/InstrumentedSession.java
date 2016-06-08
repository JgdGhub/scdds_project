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
import uk.co.octatec.scdds.net.socket.Session;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * Created by Jeromy Drake on 12/05/2016.
 */
public class InstrumentedSession implements Session {

    private final Logger log = LoggerFactory.getLogger(InstrumentedSession.class);

    final Session delegate;

    InstrumentedBlockIO  instrumentedBlockIO;
    String type;

    public InstrumentedBlockIO getInstrumentedBlockIO() {
        return instrumentedBlockIO;
    }

    public InstrumentedSession(Session delegate, String type) {
        this.delegate = delegate;
        this.type = type;
    }

    @Override
    public SelectionKey registerSelector(Selector selector, int selectorOp, Object attachment) {
        return delegate.registerSelector(selector, selectorOp, attachment);
    }

    @Override
    public void enableReadTimeout() throws IOException {
        delegate.enableReadTimeout();
    }

    @Override
    public void enableWriteWaiting() throws IOException {
        delegate.enableWriteWaiting();
    }

    @Override
    public void awaitReadReady() throws IOException {
        delegate.awaitReadReady();
    }

    @Override
    public void awaitWriteReady() throws IOException {
        delegate.awaitWriteReady();
    }

    @Override
    public boolean awaitReadReady(long timeoutMs) throws IOException {
        return delegate.awaitReadReady(timeoutMs);
    }

    @Override
    public void writeInt(int n) throws IOException {
        delegate.writeInt(n);
    }

    @Override
    public int readInt() throws IOException {
        return delegate.readInt();
    }

    @Override
    public void write(byte[] buff, int offset, int length) throws IOException {
        delegate.write(buff, offset, length);
    }

    @Override
    public void write(ByteBuffer buff) throws IOException {
        delegate.write(buff);
    }

    @Override
    public void read(byte[] buff, int offset, int length) throws IOException {
        delegate.read(buff, offset,length);
    }

    @Override
    public InetSocketAddress getAddress() {
        return delegate.getAddress();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public BlockIO getBlockIO() {
        if( instrumentedBlockIO == null ) {
            instrumentedBlockIO = new InstrumentedBlockIO(delegate.getBlockIO(), type);
            log.info("create [{}] instrumentedBlockIO #={}", type, System.identityHashCode(instrumentedBlockIO));
        }
        log.info("return [{}]cinstrumentedBlockIO #={}", type, System.identityHashCode(instrumentedBlockIO));
        return instrumentedBlockIO;
    }
}
