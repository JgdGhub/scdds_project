package uk.co.octatec.scdds.net.socket;

import java.io.IOException;

/**
 * Created by Jeromy Drake on 30/04/2016.
 */
public interface ServerSession {
    void start() throws IOException;
    void stop();
    void start(int port) throws IOException;
    Session acceptBlockingClient() throws IOException;
    Session acceptNoneBlockingClient() throws IOException;
    int getPort();

}