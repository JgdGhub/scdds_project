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
import uk.co.octatec.scdds.net.socket.ServerSession;
import uk.co.octatec.scdds.net.socket.ServerSessionImpl;
import uk.co.octatec.scdds.net.socket.Session;

import java.io.IOException;

/**
 * Created by Jeromy Drake on 12/05/2016.
 */
public class InstrumentedServerSession implements ServerSession {

    private final ServerSessionImpl delegate = new ServerSessionImpl("InstDelSrv");

    private InstrumentedSession instrumentedSession;

    public InstrumentedSession getInstrumentedSession() {
        return instrumentedSession;
    }

    @Override
    public void start() throws IOException {
        delegate.start();
    }

    @Override
    public void stop() {
        delegate.stop();
    }

    @Override
    public void start(int port) throws IOException {
        delegate.start(port);
    }

    @Override
    public Session acceptBlockingClient() throws IOException {
        instrumentedSession = new  InstrumentedSession(delegate.acceptBlockingClient(), "server");
        return instrumentedSession;
    }

    @Override
    public Session acceptNoneBlockingClient() throws IOException {
        instrumentedSession = new  InstrumentedSession(delegate.acceptNoneBlockingClient(), "server");
        return instrumentedSession;
    }

    @Override
    public int getPort() {
        return delegate.getPort();
    }
}
