package uk.co.octatec.scdds.mock;

import uk.co.octatec.scdds.net.socket.ClientConnector;
import uk.co.octatec.scdds.net.socket.Session;

/**
 * Created by Jeromy Drake on 04/05/16
 */
public class MockClientConnector implements ClientConnector {


    public MockSession mockSession = new MockSession();

    @Override
    public Session connect(boolean blockingMode, String host, int port, long retryWaitPeriod, int maxRetries) {
        return mockSession;
    }

    @Override
    public Session connectNoneBlocking(String host, int port) {
        return mockSession;
    }

    @Override
    public void abortRetryAttempts() {

    }
}
