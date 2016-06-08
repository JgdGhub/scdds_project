package uk.co.octatec.scdds.mock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.net.socket.ServerSession;
import uk.co.octatec.scdds.net.socket.Session;

import java.io.IOException;

/**
 * Created by Jeromy Drake on 04/05/16
 */
public class MockServerSession implements ServerSession {

    private final Logger log = LoggerFactory.getLogger(MockServerSession.class);

    int port = 1;
    public MockSession mockSession = new MockSession();
    boolean waitAsterFirstSession = true;
    int sessionCount;
    @Override
    public void start() throws IOException {

    }

    @Override
    public void stop() {

    }

    @Override
    public void start(int port) throws IOException {
        this.port = port;
    }

    @Override
    public Session acceptBlockingClient() throws IOException {
        log.info("---acceptBlockingClient sessionCount={}", sessionCount);
        if( sessionCount > 0 ) {
            while( true ) {
                try {
                    Thread.sleep(1000 * 60 * 60);
                } catch (Exception e) {
                    log.warn("---ignore Exception {}", e.getMessage());
                }
            }
        }
        ++sessionCount;
        log.info("---acceptBlockingClient ACCEPTED session sessionCount={}", sessionCount);
        return mockSession;
    }

    @Override
    public Session acceptNoneBlockingClient() throws IOException {
        log.info("---acceptNoneBlockingClient sessionCount={}", sessionCount);
        if( sessionCount > 0 ) {
            while( true ) {
                try {
                    Thread.sleep(1000 * 60 * 60);
                } catch (Exception e) {
                    log.warn("---ignore Exception {}", e.getMessage());
                }
            }
        }
        ++sessionCount;
        log.info("---acceptNoneBlockingClient ACCEPTED session sessionCount={}", sessionCount);
        return mockSession;
    }

    @Override
    public int getPort() {
        return port;
    }


}
