package uk.co.octatec.scdds.instrumentation;

import uk.co.octatec.scdds.net.socket.ClientConnector;
import uk.co.octatec.scdds.net.socket.ClientConnectorImpl;
import uk.co.octatec.scdds.net.socket.Session;

/**
 * Created by Jeromy Drake on 12/05/2016.
 */
public class InstrumentedClientConnector implements ClientConnector{

    ClientConnectorImpl delegate = new ClientConnectorImpl("InstDelCli");
    InstrumentedSession instrumentedSession;

    public InstrumentedSession getInstrumentedSession() {
        return instrumentedSession;
    }

    @Override
    public Session connect(boolean blockingMode, String host, int port, long retryWaitPeriod, int maxRetries) {
        instrumentedSession =  new InstrumentedSession(delegate.connect(blockingMode, host, port, retryWaitPeriod, maxRetries), "client");
        return instrumentedSession;
    }

    @Override
    public Session connectNoneBlocking(String host, int port) {
        instrumentedSession = new InstrumentedSession(delegate.connectNoneBlocking(host, port), "client");
        return instrumentedSession;
    }

    @Override
    public void abortRetryAttempts() {
        delegate.abortRetryAttempts();
    }
}
