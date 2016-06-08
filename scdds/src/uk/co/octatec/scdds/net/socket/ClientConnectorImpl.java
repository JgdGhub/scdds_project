package uk.co.octatec.scdds.net.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;

/**
 * Created by Jeromy Drake on 30/04/2016.
 */
public class ClientConnectorImpl implements ClientConnector {

    private final Logger log = LoggerFactory.getLogger(ClientConnectorImpl.class);

    private static final long DEFAULT_RETRY_WAIT = 1000*5;

    public static final int UNLIMITED_RETRUES = 0;

    private volatile boolean retryAllowed = true;

    private final String name;

    public ClientConnectorImpl(String name) {
        this.name = name;
    }

    @Override
    public Session connectNoneBlocking(String host, int port) {
        return connect(NONE_BLOCKING_MODE, host, port, DEFAULT_RETRY_WAIT, UNLIMITED_RETRUES);
    }

    @Override
    public Session connect(boolean blocking, String host, int port, long retryWaitPeriod, int maxRetries) {
        int retryCount = 0;
        while( retryAllowed &&  (maxRetries == 0 || retryCount < maxRetries) ) {
            try {
                log.info("{}: about to connect to server [{}:{}] blocking=[{}] retryCount=[{}]", name, host, port, blocking, retryCount);
                SocketChannel socketChannel = SocketChannel.open();
                log.info("{}: socket channel now open, not connected...", name);
                socketChannel.configureBlocking(blocking);
                log.info("{}: socket channel set to blocking={}", name, blocking);
                InetSocketAddress addr = new InetSocketAddress(host, port);
                log.info("{}: attempting to connect to [{}:{}]...", name, host, port);
                boolean connected = socketChannel.connect(addr);
                log.info("{}: connect returned [{}] (if this is false it is not a problem)", name, connected);
                while (!socketChannel.finishConnect()) {
                    Thread.sleep(500);
                    log.info("{}: attempting finishConnect...", name);
                }
                log.info("{}: finishConnect returned true, connected to [{}:{}]", name, host, port);
                return new SessionImpl(socketChannel, addr, name);
            }
            catch( InterruptedException e) {
                log.error(name+": failed to connect client to server host=[{}], port=[{}], retryWaitPeriod=[{}], InterruptedException while awaiting finishConnect", host, port,retryWaitPeriod);
                throw new RuntimeException(name+": Connection Failed (InterruptedException while awaiting finishConnect)", e);
            }
            catch( Exception e) {

                log.error(name+": failed to connect client to server host=[{}], port=[{}], retryWaitPeriod=[{}], retryCount=[{}] maxRetries=[{}]", host, port,retryWaitPeriod, retryCount, maxRetries, e);
                ++retryCount;
                if(retryCount >= maxRetries &&  maxRetries > 0  ) {
                    break;
                }

                try {
                    Thread.sleep(retryWaitPeriod);
                }
                catch( InterruptedException x) {
                    log.error(name+": failed to connect client to server host=[{}], port=[{}], retryWaitPeriod=[{}], InterruptedException while retrying", host, port,retryWaitPeriod);
                    throw new RuntimeException(name+": Connection Failed (InterruptedException while retrying)", x);
                }
            }
        }
        log.error(name+": connect failed and retries nolonger allowed, host=[{}], port=[{}] retryWaitPeriod=[{}] maxRetries=[{}], ",host, port, retryWaitPeriod, maxRetries);
        throw new RuntimeException(name+": Connection Failed (retries nolonger allowed)");
    }



    @Override
    public void abortRetryAttempts() {
        retryAllowed = false;
    }
}
