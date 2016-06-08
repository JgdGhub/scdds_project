package uk.co.octatec.scdds.mock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.cache.ListenerEvent;
import uk.co.octatec.scdds.cache.publish.*;
import uk.co.octatec.scdds.net.socket.*;
import uk.co.octatec.scdds.utilities.AwaitParams;

import java.io.IOException;
import java.net.InetSocketAddress;

import java.util.Collection;
import java.util.Map;


/**
 * Created by Jeromy Drake on 07/05/16
 */
public class MockCachePublisher<K,T> implements CachePublisher<K,T> {

    private final Logger log = LoggerFactory.getLogger(CachePublisher.class);

    final private ServerSession serverSession = new ServerSessionImpl("InMockSrv");
    final private GeneralRequestHandler generalRequestHandler;

    private Thread serverThread;

    private static long nextSessionId = 0;
    private volatile long lastSession;

    volatile boolean listening;
    public volatile long removedSessionId;

    public volatile String initString;

    public MockCachePublisher(GeneralRequestHandler generalRequestHandler) {
        this.generalRequestHandler = generalRequestHandler;
    }

    public boolean waitForStart() throws InterruptedException{
        for(int i=0; i<10; i++){
            Thread.sleep(10);
        }
        Thread.sleep(10);
        return listening;
    }

    @Override
    public int initializePort() throws IOException {
        serverSession.start();
        return serverSession.getPort();
    }

    @Override
    public void start() {
        serverThread = new Thread(){
            public void run() {
                serverLoop();
            }
        };
        serverThread.setName("MockCachePublisher");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    @Override
    public void stop() {
        serverThread.interrupt();
        serverSession.stop();
        for(long i=1; i<nextSessionId; i++) {
            generalRequestHandler.unRegisterSession(i);
        }
    }

    @Override
    public String getCacheName() {
        return "NoName";
    }

    private void serverLoop() {
        while( true ) {
            doServerLoop();
        }
    }

    private void doServerLoop() {
        try {
            // the accept loop is blocking, but the client returned is none blocking

            log.info("listening for client subscription request...");

            listening = true;

            Session sc = serverSession.acceptNoneBlockingClient();
            InetSocketAddress addr = sc.getAddress();

            BlockIO bio = sc.getBlockIO();
            log.info("got client connection from [{}]", sc.getAddress());
            initString = bio.readString();
            log.info("got client initialization string [{}]", initString);
            lastSession = ++nextSessionId;
            log.info("registering session [{}}", lastSession);
            generalRequestHandler.registerSession(lastSession, this, sc);

        }
        catch( IOException e) {
            log.error("exception while in server-loop",e);
        }
    }

    public long getLastSessionId() {
        return lastSession;
    }

    @Override
    public boolean removeSubscription(long sessionId) {
        removedSessionId = sessionId;
        return true;
    }

    @Override
    public void onInitialUpdate(ServerSideCacheSubscription<K, T> theClient, Map<K, T> data) {

    }

    @Override
    public void onUpdate(K key, T value) {

    }

    @Override
    public void onBatchUpdate(Collection<ListenerEvent<K, T>> listenerEvents) {

    }

    @Override
    public void onDataStale() {

    }

    @Override
    public void onActive() {

    }

    @Override
    public void onRemoved(K key, T value) {

    }

    @Override
    public void onFailedClient(ServerSideCacheSubscription<K, T> client) {

    }

    public void awaitSessionIdGt(long n) throws InterruptedException{
        for (int i = 0; i < AwaitParams.AWAIT_LOOP_COUNT; i++) {
            if ( lastSession > n) {
                break;
            }
            Thread.sleep(AwaitParams.AWAIT_SLEEP_TIME);
        }
        if( lastSession <= n ) {
            log.warn("*** awaitSessionIdGt: wait failed got {} wanted > {}", lastSession, n);
        }
    }

    public void awaitRemovedSessionIdGt(long n) throws InterruptedException{
        for(int i=0; i<AwaitParams.AWAIT_LOOP_COUNT; i++) {
            if( removedSessionId > n ) {
                break;
            }
            Thread.sleep(AwaitParams.AWAIT_SLEEP_TIME);;
        }
        if( getLastSessionId() <= n ) {
            log.warn("*** awaitRemovedSessionIdGt: wait failed got {} wanted > {}", removedSessionId, n);
        }
    }
}
