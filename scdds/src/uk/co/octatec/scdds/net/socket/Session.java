package uk.co.octatec.scdds.net.socket;
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
