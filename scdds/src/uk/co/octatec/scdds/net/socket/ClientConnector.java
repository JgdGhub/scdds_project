package uk.co.octatec.scdds.net.socket;

/**
 * Created by Jeromy Drake on 30/04/2016.
 */
public interface ClientConnector {
    static final boolean BLOCKING_MODE = true;
    static final boolean NONE_BLOCKING_MODE = false;
    Session connect(boolean blockingMode, String host, int port, long retryWaitPeriod, int maxRetries);
    Session connectNoneBlocking(String host, int port);
    void abortRetryAttempts();
}
