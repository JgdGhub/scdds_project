package uk.co.octatec.scdds.cache.subscribe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static uk.co.octatec.scdds.ConditionalCompilation._DBG;

import uk.co.octatec.scdds.GlobalDefaults;
import uk.co.octatec.scdds.cache.publish.CacheFilter;
import uk.co.octatec.scdds.cache.publish.CachePublisher;
import uk.co.octatec.scdds.cache.publish.PropertyUtils;
import uk.co.octatec.scdds.net.registry.CacheLocator;
import uk.co.octatec.scdds.net.serialize.Serializer;
import uk.co.octatec.scdds.net.serialize.SerializerFactory;
import uk.co.octatec.scdds.net.socket.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Jeromy Drake on 04/05/2016.
 */
public class CacheSubscriberImpl<K,T> implements CacheSubscriber<K,T> {

    private final static Logger log = LoggerFactory.getLogger(CacheSubscriberImpl.class);

    static final long SERVER_RETRY_INTERVAL = 100;  // no real point in retrying to connect to the server as we used the Locator to find the server
    static final int MAX_SERVER_RETRIES = 2;       // and verify that it is up, if we can't connect, it must have died, so we need to go back to the Regitry and ask again

    final private CacheLocator locator;
    final private CacheImplClientSide<K, T> cache;
    final private CacheFilter<K,T> filter;
    final private String filterArg;
    final private int maxRetries;
    final ClientConnector connector;
    final InitialUpdateReaderFactory<K,T> initialUpdateReaderFactory;
    protected String sessionId;
    protected Thread subscriptionThread;

    private Session sc;

    private static final AtomicInteger threadCounter = new AtomicInteger(0);

    private volatile boolean quit;

    private volatile int missedHeartbeatCount;
    private volatile int cconnectionCount;
    private volatile int connectionErrorCount;

    public CacheSubscriberImpl(CacheImplClientSide<K,T> cache, CacheLocator locator, CacheFilter<K,T> filter, String filterArg, InitialUpdateReaderFactory<K,T>  initialUpdateReaderFactory ) {
        this(new ClientConnectorImpl("CacheSubClient"), cache, locator, filter, filterArg, MAX_SERVER_RETRIES, initialUpdateReaderFactory);
    }

    public CacheSubscriberImpl(CacheImplClientSide<K,T> cache, CacheLocator locator, CacheFilter<K,T> filter, String filterArg, int maxRetries, InitialUpdateReaderFactory<K,T>  initialUpdateReaderFactory ) {
        this(new ClientConnectorImpl("CacheSubClient"), cache, locator, filter, filterArg, maxRetries, initialUpdateReaderFactory);
    }

    public CacheSubscriberImpl(ClientConnector connector, CacheImplClientSide<K,T> cache, CacheLocator locator, CacheFilter<K,T> filter, String filterArg, int maxRetries, InitialUpdateReaderFactory<K,T>  initialUpdateReaderFactory) {
        this.connector = connector;
        this.locator = locator;
        this.filter = filter;
        this.filterArg = filterArg;
        this.cache = cache;
        this.maxRetries = maxRetries;
        this.initialUpdateReaderFactory = initialUpdateReaderFactory;
    }

    @Override
    public boolean subscribe(final int heartbeatSeconds) {

        final InetSocketAddress cacheAddr = locator.locate(cache.getName(),0/*unlimited retries*/);
        log.info("client has located server at [{}]", cacheAddr);
        if (cacheAddr == null) {
            log.error("No Cache could be located [{}]", cache.getName());
            return false;
        }

        subscriptionThread = new Thread() {
            public void run() {
                startSubscription(cacheAddr, heartbeatSeconds);
            }
        };
        log.info("starting subscription thread for [{}] connecting to [{}]", cache.getName(), cacheAddr);
        //subscriptionThread.setDaemon(true);
        subscriptionThread.setName("Subscription:"+cache.getName()+":"+ threadCounter.incrementAndGet());
        subscriptionThread.start();
        return true;
    }

    @Override
    public boolean unSubscribe() {
        try {
            quit = true;
            if( sc == null ) {
                log.info("session is null, can't send unsubscribe message");
                if( subscriptionThread != null ) {
                    subscriptionThread.interrupt();
                }
                return false;
            }
            String unSubscriptionRqst = PropertyUtils.createPropertyString(CachePublisher.ARG_REQUEST, CachePublisher.RQST_UNSUBSCRIBE,
                    CachePublisher.ARG_SESSION_ID, sessionId,
                    CachePublisher.ARG_CACHE_NAME, cache.getName());
            log.info("send unsubscibe request [{}]...", unSubscriptionRqst);
            sc.getBlockIO().writeString(unSubscriptionRqst);
            log.info("...unsubscribe request sent");
            Thread.sleep(100);
            sc.close();
            subscriptionThread.interrupt();
            cache.notifyStale();
            return true;
        }
        catch( Exception e) {
            log.error("error during unsubscribe [{}]", cache.getName(), e);
            return false;
        }
    }

    @Override
    public String getCacheName() {
        return cache.getName();
    }

    private InetSocketAddress locate(String cacheName) throws InterruptedException{
        log.info("...locating cache [{}]...", cacheName);
        while(true) {
            InetSocketAddress cacheAddr = locator.locate(cache.getName(), 1);
            if( cacheAddr != null ) {
                log.info("...found cache [{}]...", cacheName);
                return cacheAddr;
            }
            log.info("can't locate cache [{}], will retry... retry-interval=[{}]", cacheName, GlobalDefaults.CACHE_REGISTRY_RETRY_WAIT);
            Thread.sleep( GlobalDefaults.CACHE_REGISTRY_RETRY_WAIT);
        }
    }

    public void startSubscription(InetSocketAddress cacheAddr, int heartbeatSeconds) {

        log.info("starting subscription for [{}] in thread [{}]...", cache.getName(), Thread.currentThread().getName());

        long readTimeoutMs = heartbeatSeconds*1000;

        try {

            if( cacheAddr == null ) {
                log.info("locating server in registry...");
                cacheAddr = locate(cache.getName());
                log.info("server located in registry [{}]",cacheAddr );
            }
            String filterClassName = filter==null ? "null" : filter.getClass().getName();
            String host = cacheAddr.getHostName();
            int port = cacheAddr.getPort();
            log.info("subscribing to [{}] at [{}:{}] heartbeatSeconds=[{}] maxRetries=[{}] retry-interval=[{}] [{}]", cache.getName(), host, port, heartbeatSeconds, maxRetries, SERVER_RETRY_INTERVAL, (new Date()));
            //sc = connector.connect(ClientConnector.BLOCKING_MODE, host, port, RETRY_INTERVAL, maxRetries);
            sc = connector.connect(ClientConnector.NONE_BLOCKING_MODE, host, port, SERVER_RETRY_INTERVAL, maxRetries);
            log.info("connection established for [{}] at [{}:{}]", cache.getName(), host, port);
            ++cconnectionCount;
            sc.enableReadTimeout();

            String subscriptionRqst = PropertyUtils.createPropertyString(CachePublisher.ARG_REQUEST, CachePublisher.RQST_SUBSCRIBE,
                                                                CachePublisher.ARG_CACHE_NAME, cache.getName(),
                                                                CachePublisher.ARG_TIMESTAMP, System.currentTimeMillis(),
                                                                CachePublisher.ARG_FILTER, filterClassName);
            log.info("D [{}]", subscriptionRqst);
            BlockIO bIO = sc.getBlockIO();
            long sendTime = System.currentTimeMillis();
            bIO.writeString(subscriptionRqst);
            sc.awaitReadReady();
            String reply = bIO.readString();
            long recvTime = System.currentTimeMillis();
            long roundTripTime =  recvTime-sendTime;
            log.info("read reply [{}] roundTripTime=[{}]", reply, roundTripTime);
            Properties properties = PropertyUtils.getPropertiesFromString(reply);
            String errorText = properties.getProperty(CachePublisher.ARG_ERROR);
            if( errorText != null ) {
                // should not be possible
                log.error("error reply from subscription request [{}]", errorText);
                sc.close();
                return;
            }
            sessionId =  properties.getProperty(CachePublisher.ARG_SESSION_ID);

            String serializerFactoryName = properties.getProperty(CachePublisher.ARG_SERIALIZER_FACTORY);
            Serializer<K,T> serializer = null;
            if( serializerFactoryName != null ) {
                log.info("creating client-side serializer-factor instance [{}]", serializerFactoryName);
                try {
                    Class clz = Class.forName(serializerFactoryName);
                    SerializerFactory<K,T> factory = (SerializerFactory)clz.newInstance();
                    serializer = factory.create();
                }
                catch( Exception e) {
                    log.error("failed to create serializer", e);
                    sc.close();
                    return;
                }
            }
            else {
                // should not be possible
                log.error("no seriializer factory name specified in server message");
                sc.close();
                return;
            }
            log.info("staring main subscription loop");
            subscriptionLoop(sc, serializer, readTimeoutMs);
        }
        catch( IOException e) {
            cache.notifyStale();
            handleConnectionError(sc, heartbeatSeconds);
            log.info("previous main loop exiting thread={}", Thread.currentThread().getName() );
        }
        catch( Throwable t) {
            log.error("FATAL ERROR - cannot continue due to to exception [{}]", t.getMessage());
            log.error("unexpected exception from subscription loop cache=[{}]", cache.getName(), t);
            cache.clientSideStart(); // start the cache so that the fatal error gets delivered to listeners
                                     // (its safe to start the cache multiple times)
            cache.clientNotifyFatalError(t.getMessage());
        }
    }

    protected void subscriptionLoop(final Session sc, final Serializer<K,T> serializer, final long readTimeoutMs ) throws IOException {

        int waitingForHeartbeat = 0;
        BlockIO bIO = sc.getBlockIO();
        InitialUpdateReader<K,T>  initialUpdateReader = null;

        log.info("main subscription loop running...");

        while(true) {

            byte[] buff;
            //try {
            if( _DBG ) {
                log.debug("reading block header");
            }

                boolean readReady = sc.awaitReadReady(readTimeoutMs);

                if (readReady) {

                    BlockIO.Header hdr = bIO.readBlockHeader();

                    if (hdr.flag == BlockIoImpl.DATA_FLAG) {
                        buff = bIO.readRestOfBlock();
                        Serializer.Pair<K, T> pair = serializer.deserialize(buff, 0);
                        cache.put(pair.key, pair.value);
                    } else if (hdr.flag == BlockIoImpl.HEARTBEAT_FLAG) {
                        // this is a heartbeat response, we don't
                        // need to process it
                        log.info("received heartbeat");
                        bIO.readRestOfBlock();
                    } else if (hdr.flag == BlockIoImpl.INITIAL_UPDATE_FLAG || hdr.flag == BlockIoImpl.INITIAL_UPDATE_COMPLETED_FLAG) {
                        log.info("read initial update [{}] flag=[{}]", cache.getName(), hdr.flag);
                        if (initialUpdateReader == null) {
                            initialUpdateReader = initialUpdateReaderFactory.create(bIO, serializer, cache);
                            cache.clientSideGetData().clear();
                        }
                        initialUpdateReader.readInitialUpdate(cache.clientSideGetData());
                        if (hdr.flag == BlockIoImpl.INITIAL_UPDATE_COMPLETED_FLAG) {
                            log.info("end of initial update detected, starting cache on client side cache-size={}", cache.clientSideGetData().size());
                            cache.clientSideStart();
                            // any listeners that have been added while the cache was stopped will be
                            // notified first with an onActive() then with an onInitialData();
                            // by virtue of the listener being added
                            // NB: if we are here because of a disconnection then the
                            // call to clientSideStart() will re-dispatch  onActive()  and
                            // onInitialData() to all listeners
                            initialUpdateReader = null;
                        }
                    } else if (hdr.flag == BlockIoImpl.STALE_FLAG) {
                        // we have been told by the server the cache is
                        // stale - maybe the source of the data in the
                        // server has gone down - if our connection
                        // to the server goes down, we will also
                        // call notifyStale
                        log.info("received stale indication");
                        bIO.readRestOfBlock();
                        cache.notifyStale();
                    } else if (hdr.flag == BlockIoImpl.UN_STALE_FLAG) {
                        log.info("received un-stale indication");
                        bIO.readRestOfBlock();
                        cache.notifyUnStale();
                    } else if (hdr.flag == BlockIoImpl.DATA_REMOVAL_FLAG) {
                        buff = bIO.readRestOfBlock();
                        Serializer.Pair<K, T> pair = serializer.deserialize(buff, 0);
                        T value = cache.remove(pair.key);
                        if (value == null) {
                            log.warn("attempted removal of key=[{}] did not find a value in the cache [{}]", cache.getName());
                        }
                    } else {
                        log.error("unexpected message hdr: flag=[{}] length={}", hdr.flag, hdr.dataLength);
                    }
                    waitingForHeartbeat = 0;
                }
                else  { // (SocketTimeoutException x){

                    // we timed out after waiting for 'heartbeat interval' in the read, so
                    // we must send a heartbeat request to make sure the server is alive

                    if( _DBG ) {
                        log.debug("read timeout from socket, readTimeoutMs={}, waitingForHeartbeat={}", readTimeoutMs, waitingForHeartbeat);
                    }

                    if (quit) {
                        log.info("unsubscribe detected");
                        return;
                    }
                    if (waitingForHeartbeat > 1) {
                        ++missedHeartbeatCount;
                        log.error("too many missed heartbeats, closing connection [{}] sessionId=[{}] missedHeartbeatCount=[{}]", cache.getName(), sessionId, missedHeartbeatCount);
                        sc.close();
                        throw new IOException("too many missed heartbeats");
                    } else {
                        ++waitingForHeartbeat;
                        log.info("requesting heartbeat from sessionId=[{}] waitingForHeartbeat={}", sessionId, waitingForHeartbeat);
                    }
                    bIO.writeString(PropertyUtils.createPropertyString(CachePublisher.ARG_REQUEST, CachePublisher.RQST_HEARTBET,
                            CachePublisher.ARG_SESSION_ID, sessionId, CachePublisher.ARG_TIMESTAMP, System.currentTimeMillis()));
               // }
            }
        }
    }

    protected void handleConnectionError(Session sd, int heartbeatSeconds) {
        log.info("handling network error for cache [{}] sessionId=[{}] quit?{}", cache.getName(), sessionId, quit);
        ++connectionErrorCount;
        cache.notifyStale();
        cache.setNetworkErrorState();
        // the cache and all listeners have been notified it is stale
        // when the subscription is re-established, we will get a new initial
        // download and and an unStale notification (onActive) will be
        // sent first before the onInitalUpdate
        sd.close();
        if( quit ) {
            log.info("unsubscribe detected, no reconnect");
            return;
        }
        log.info("restarting subscription for cache [{}] sessionId=[{}]", cache.getName(), sessionId);
        startSubscription(null, heartbeatSeconds) ;
    }

    boolean isUnsubscribed() {
        return quit = true;
    }

    boolean isSubscriptionThreadFinished() {
        log.info("subscriptionThread state: [{}] isInterrupted={} isAlive={} tid=[{}}",
                subscriptionThread.getState(), subscriptionThread.isInterrupted(), subscriptionThread.isAlive(), subscriptionThread.getId());
        return subscriptionThread.isInterrupted() || !subscriptionThread.isAlive();
    }

    @Override
    public String toString() {
        return "{"+cache.getName()+":"+sessionId+"]";
    }

    public int getConnectionErrorCount() {
        return connectionErrorCount;
    }

    public int getCconnectionCount() {
        return cconnectionCount;
    }

    public int getMissedHeartbeatCount() {
        return missedHeartbeatCount;
    }
}
