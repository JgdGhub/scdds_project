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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Created by Jeromy Drake on 30/04/2016.
 */
public class ServerSessionImpl implements ServerSession {

    private final Logger log = LoggerFactory.getLogger(ServerSessionImpl.class);

    private ServerSocketChannel serverChannel;
    private InetSocketAddress addr;
    private final String name;

    public ServerSessionImpl(String name) {
        this.name = name;
    }

    @Override
    public void start(int port) throws IOException {
        addr = new InetSocketAddress(port);
        start();


    }

    @Override
    public void start() throws IOException {
        serverChannel = ServerSocketChannel.open();
        serverChannel.socket().setReuseAddress(true);
        serverChannel.socket().bind(addr);
        addr = (InetSocketAddress)serverChannel.getLocalAddress();
        log.info("{}: server session created [{}]", name, addr);

    }

    @Override
    public void stop() {
        try {
            log.info("{}: stop server session [{}]", name, addr);
            serverChannel.close();
        }
        catch( Exception e) {
            log.error("{}: exception while stopping server session [{}]", name, e.getMessage());
        }
    }

    @Override
    public Session acceptBlockingClient() throws IOException {
        return acceptClient(true/*blocking*/, name);
    }

    @Override
    public Session acceptNoneBlockingClient() throws IOException {
        return acceptClient(false/*blocking*/, name);
    }

    private Session acceptClient(boolean blocking, String name) throws IOException {
        log.info("{}: await client connection, client connection mode will have blocking=[{}]", name, blocking);
        SocketChannel sc = serverChannel.accept();
        sc.configureBlocking(blocking);
        return new SessionImpl(sc, addr, name);
    }



    @Override
    public int getPort() {
        return addr.getPort();
    }
}
