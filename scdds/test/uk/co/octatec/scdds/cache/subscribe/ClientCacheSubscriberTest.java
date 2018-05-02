package uk.co.octatec.scdds.cache.subscribe;
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
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.GlobalProperties;
import uk.co.octatec.scdds.cache.ImmutableEntry;
import uk.co.octatec.scdds.utilities.AwaitParams;
import uk.co.octatec.scdds.utilities.DataWithDate;
import uk.co.octatec.scdds.utilities.SimpleCacheListener;
import uk.co.octatec.scdds.utilities.SimpleData;
import uk.co.octatec.scdds.cache.publish.CacheFilter;
import uk.co.octatec.scdds.cache.publish.CachePublisher;
import uk.co.octatec.scdds.cache.publish.AlwayAcceptingCacheFilter;
import uk.co.octatec.scdds.cache.publish.PropertyUtils;
import uk.co.octatec.scdds.mock.MockCacheLocator;
import uk.co.octatec.scdds.mock.MockClientConnector;
import uk.co.octatec.scdds.net.socket.InitialUpdateReaderWriterTest;
import uk.co.octatec.scdds.net.registry.CacheLocator;
import uk.co.octatec.scdds.net.registry.CacheLocatorImpl;
import uk.co.octatec.scdds.net.serialize.Serializer;
import uk.co.octatec.scdds.net.serialize.SerializerFactoryDefaultImpl;
import uk.co.octatec.scdds.net.socket.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by Jeromy Drake on 04/05/16
 */
public class ClientCacheSubscriberTest {

    // Test the subscription mechanism, from the client to the server, the actual connection
    // is mocked-ouy

    private final static Logger log = LoggerFactory.getLogger(ClientCacheSubscriberTest.class);

    static class TestCacheSubscriberImpl<K,T extends ImmutableEntry> extends CacheSubscriberImpl<K,T> {

        volatile Serializer<K,T> serializer = null;
        volatile boolean subscriptionLoopEntered;

        volatile boolean useDummySubscriptionLoop = true;
        volatile boolean gotConnectionError;

        public TestCacheSubscriberImpl(CacheImplClientSide<K,T> cache, CacheLocator locator, CacheFilter<K,T> filter) {
            super(new ClientConnectorImpl("CachePubTest"), cache, locator, filter, null, 0, new InitialUpdateReaderFactoryImpl<K, T>());
        }

        public TestCacheSubscriberImpl(ClientConnector con, CacheImplClientSide<K,T> cache, CacheLocator locator, CacheFilter<K,T> filter) {
            super(con, cache, locator, filter, null, 0, new InitialUpdateReaderFactoryImpl<K, T>());
        }

        @Override
        protected void subscriptionLoop(Session sc, Serializer<K,T> serializer, long timeoutMs ) throws IOException {
            this.serializer = serializer;
            subscriptionLoopEntered = true;
            log.info("--in dummy subscription loop subscriptionLoopEntered=[{}]--", subscriptionLoopEntered);
            if( useDummySubscriptionLoop ) {
                return;
            }
            else {
                log.info("--entering main subscription loop--");
                super.subscriptionLoop(sc, serializer, 100);
            }
        }

        protected void handleConnectionError(Session sd, int heartbeatSeconds) {
            gotConnectionError = true;
            log.info("--detected connection error--");
            sd.close();
        }

        public String getSessionId() {
            return sessionId;
        }

        public Thread getSubscriptionThread() {
            return subscriptionThread;
        }

        void awaitConnectionError() throws InterruptedException{
            for(int i = 0; i< AwaitParams.AWAIT_LOOP_COUNT; i++) {
                if( gotConnectionError ) {
                    break;
                }
                else {
                    Thread.sleep(AwaitParams.AWAIT_SLEEP_TIME);
                }
            }
            if( !gotConnectionError )  {
                log.warn("*** awaitConnectionError: wait failed");
            }
        }

        void awaitSubscriptionLoopEntered() throws InterruptedException{
            for(int i=0; i<AwaitParams.AWAIT_LOOP_COUNT; i++) {
                if( subscriptionLoopEntered ) {
                    break;
                }
                else {
                    Thread.sleep(AwaitParams.AWAIT_SLEEP_TIME);
                }
            }
            if( !subscriptionLoopEntered )  {
                log.warn("*** awaitSubscriptionLoopEntered: wait failed");
            }
        }
    }

    @BeforeClass
    public static void setup() {
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO");
        GlobalProperties.exposeHttpServer = false;
    }

    @Test
    public void cacheSubscriberFactoryTest() {

        log.info("##cacheSubscriberFactoryTest");

        List<InetSocketAddress> registries = new ArrayList<>();
        InetSocketAddress addr = new InetSocketAddress("localhost", 9999);
        registries.add(addr);
        CacheLocator locator = new CacheLocatorImpl(registries);

        CacheSubscriberFactoryDefaultImpl<String, SimpleData> cacheSubscriberFactory = new CacheSubscriberFactoryDefaultImpl<>();
        CacheImplClientSide<String,SimpleData> cache = (CacheImplClientSide)CacheImplClientSide.createLocalCache("TestCache");
        Assert.assertNotNull("cache not null", cache);

        CacheSubscriber<String,SimpleData> cacheSubscriber = cacheSubscriberFactory.create(cache, locator, null/*filter*/, null/* filter-arg*/, new InitialUpdateReaderFactoryImpl<String,SimpleData>());
        Assert.assertNotNull("cacheSubscriber is not null", cacheSubscriber);
    }

    @Test
    public void cacheSubscriptionTest() throws InterruptedException {

        log.info("##cacheSubscriptionTest");

        MockClientConnector mockConnector = new MockClientConnector();
        CacheImplClientSide<String,SimpleData> cache = (CacheImplClientSide)CacheImplClientSide.createLocalCache("TestCache");
        CacheLocator locator = new MockCacheLocator();
        TestCacheSubscriberImpl<String,SimpleData> subscriber = new TestCacheSubscriberImpl<>(mockConnector, cache, locator, new AlwayAcceptingCacheFilter<String,SimpleData>());

        SerializerFactoryDefaultImpl<String,SimpleData> serializerFactory = new  SerializerFactoryDefaultImpl<String,SimpleData>();

        mockConnector.mockSession.initialStringRead = PropertyUtils.createPropertyString(
                                                                CachePublisher.ARG_SESSION_ID, 1001,
                                                                CachePublisher.ARG_SERIALIZER_FACTORY, serializerFactory.getClass().getName(),
                                                                CachePublisher.ARG_CONNECTION_COUNT, 1
                                                        );

        log.info("subscriber should read message: [{}]", mockConnector.mockSession.initialStringRead);

        subscriber.subscribe(15/*heartbet-seconds*/);

        subscriber.awaitSubscriptionLoopEntered();

        log.info("subsciption: sessionId=[{}] thread=[{}] subscriptionLoopEntered=[{}]",subscriber.getSessionId(), subscriber.getSubscriptionThread(), subscriber.subscriptionLoopEntered );

        Assert.assertTrue("subscription loop entered ", subscriber.subscriptionLoopEntered);
        Assert.assertNotNull("thread started ", subscriber.getSubscriptionThread());
        Assert.assertEquals("session id set ", "1001", subscriber.getSessionId());
    }

    @Test
    public void cacheSubscriptionMainLoopHeartbetTest() throws InterruptedException {

        log.info("##cacheSubscriptionMaiLoopTest");

        MockClientConnector mockConnector = new MockClientConnector();
        CacheImplClientSide<String,SimpleData> cache = (CacheImplClientSide)CacheImplClientSide.createLocalCache("TestCache");
        CacheLocator locator = new MockCacheLocator();
        TestCacheSubscriberImpl<String,SimpleData> subscriber = new TestCacheSubscriberImpl<>(mockConnector, cache, locator, new AlwayAcceptingCacheFilter<String,SimpleData>());

        SerializerFactoryDefaultImpl<String,SimpleData> serializerFactory = new  SerializerFactoryDefaultImpl<String,SimpleData>();

        mockConnector.mockSession.initialStringRead = PropertyUtils.createPropertyString(
                CachePublisher.ARG_SESSION_ID, 1001,
                CachePublisher.ARG_SERIALIZER_FACTORY, serializerFactory.getClass().getName(),
                CachePublisher.ARG_CONNECTION_COUNT, 1
        );

        // set up a singe 'mock' read  of a heartbeat
        BlockIO.Header header =  new BlockIO.Header();
        header.flag = BlockIoImpl.HEARTBEAT_FLAG;
        header.dataLength = 1;
        mockConnector.mockSession.headerToRead.add(header);
        byte[] heartbeatMessage = new byte[1];
        heartbeatMessage[0] = 'h';
        mockConnector.mockSession.dataToRead.add(heartbeatMessage);

        // once all 'mock' Header/Block data has been read, an IOException will be riased
        // to simulate a disconnection

        log.info("subscriber should read string message: [{}]", mockConnector.mockSession.initialStringRead);

        subscriber.useDummySubscriptionLoop = false; // this test will go through the main loop

        subscriber.subscribe(15/*heartbet-seconds*/);

        subscriber.awaitConnectionError();


        log.info("subsciption: sessionId=[{}] thread=[{}] subscriptionLoopEntered=[{}]",subscriber.getSessionId(), subscriber.getSubscriptionThread(), subscriber.subscriptionLoopEntered );

        Assert.assertTrue("subscription loop entered ", subscriber.subscriptionLoopEntered);
        Assert.assertTrue("connection error detected ", subscriber.gotConnectionError);
        Assert.assertNotNull("thread started ", subscriber.getSubscriptionThread());
        Assert.assertEquals("session id set ", "1001", subscriber.getSessionId());

    }

    @Test
    public void cacheSubscriptionMainLoopDataTest() throws InterruptedException {

        log.info("##cacheSubscriptionMainLoopDataTest");

        MockClientConnector mockConnector = new MockClientConnector();
        CacheImplClientSide<String,SimpleData> cache = (CacheImplClientSide)CacheImplClientSide.createLocalCache("TestCache");
        CacheLocator locator = new MockCacheLocator();
        TestCacheSubscriberImpl<String,SimpleData> subscriber = new TestCacheSubscriberImpl<>(mockConnector, cache, locator, new AlwayAcceptingCacheFilter<String,SimpleData>());

        SerializerFactoryDefaultImpl<String,SimpleData> serializerFactory = new  SerializerFactoryDefaultImpl<String,SimpleData>();

        mockConnector.mockSession.initialStringRead = PropertyUtils.createPropertyString(
                CachePublisher.ARG_SESSION_ID, 1001,
                CachePublisher.ARG_SERIALIZER_FACTORY, serializerFactory.getClass().getName(),
                CachePublisher.ARG_CONNECTION_COUNT, 1
        );

        SimpleCacheListener<String,SimpleData> listener = new SimpleCacheListener<>();
        cache.addListener(listener);
                // we always get an onActive and onInitialUpdate in a newly added listener
                // in this case there is no data in the cache yet

        Thread.sleep(100);
        Serializer<String, SimpleData> serializer = serializerFactory.create();

        // set up a  'mock' read  of a data item [1]

        SimpleData d1 = new SimpleData("d1", 1);
        byte[] data1 = serializer.serialize("d1", d1, 0) ;
        BlockIO.Header header1 =  new BlockIO.Header();
        header1.flag = BlockIoImpl.DATA_FLAG;
        header1.dataLength = data1.length;
        mockConnector.mockSession.headerToRead.add(header1);
        mockConnector.mockSession.dataToRead.add(data1);

        // set up a  'mock' read  of a data item [2]

        SimpleData d2 = new SimpleData("d2", 2);
        byte[] data2 = serializer.serialize("d2", d2, 0) ;
        BlockIO.Header header2 =  new BlockIO.Header();
        header2.flag = BlockIoImpl.DATA_FLAG;
        header2.dataLength = data2.length;
        mockConnector.mockSession.headerToRead.add(header2);
        mockConnector.mockSession.dataToRead.add(data2);


        // once all 'mock' Header/Block data has been read, an IOException will be riased
        // to simulate a disconnection

        log.info("subscriber should read string message: [{}]", mockConnector.mockSession.initialStringRead);

        subscriber.useDummySubscriptionLoop = false; // this test will go through the main loop

        subscriber.subscribe(15/*heartbet-seconds*/);

        subscriber.awaitConnectionError();

        log.info("subsciption: sessionId=[{}] thread=[{}] subscriptionLoopEntered=[{}]",subscriber.getSessionId(), subscriber.getSubscriptionThread(), subscriber.subscriptionLoopEntered );


        log.info("cache key set {}", cache.keySet());

        Assert.assertTrue("subscription loop entered ", subscriber.subscriptionLoopEntered);
        Assert.assertTrue("connection error detected ", subscriber.gotConnectionError);
        Assert.assertNotNull("thread started ", subscriber.getSubscriptionThread());
        Assert.assertEquals("data copied to cache ", 2, cache.size());

        listener.awaitOnInitialUpdateCount(1);

        log.info("counters: onActiveCount={} onInitialUpdateCount={} onInitialUpdateMapSize={} onUpdateCount={} onRemoveCount={} onDataStaleCount={}",
                listener.onActiveCount,listener.onInitialUpdateCount, listener.onInitialUpdateMapSize, listener.onUpdateCount,listener.onRemoveCount,listener.onDataStaleCount);

        Assert.assertEquals("onActiveCount ", 1, listener.onActiveCount);
        Assert.assertEquals("onInitialUpdateCount", 1, listener.onInitialUpdateCount);
        Assert.assertEquals("onInitialUpdateMapSize", 0, listener.onInitialUpdateMapSize);
        Assert.assertEquals("onUpdateCount", 2, listener.onUpdateCount);
        Assert.assertEquals("onRemoveCount", 0, listener.onRemoveCount);
        Assert.assertEquals("onDataStaleCount", 1, listener.onDataStaleCount);

        // now test unsubsubscribe

        boolean unsubscribeOk = subscriber.unSubscribe();

        Assert.assertTrue("unsubscribeOk", unsubscribeOk);

        // verufy the subscribe wrote an 'unsubscreib' message to the Server
        log.info("lastStringWrite [{}]", mockConnector.mockSession.lastStringWrite);

        Properties properties = PropertyUtils.getPropertiesFromString(mockConnector.mockSession.lastStringWrite);
        String request = properties.getProperty("request");
        Assert.assertEquals("correct request", "unsubscribe", request);
        String cacheName = properties.getProperty("cache-name");
        Assert.assertEquals("correct cache-name=", "TestCache", cacheName);

    }

    @Test
    public void cacheSubscriptionMainLoopDataRemovalTest() throws InterruptedException {

        log.info("##cacheSubscriptionMainLoopDataRemovalTest");

        MockClientConnector mockConnector = new MockClientConnector();
        CacheImplClientSide<String,SimpleData> cache = (CacheImplClientSide)CacheImplClientSide.createLocalCache("TestCache");
        CacheLocator locator = new MockCacheLocator();
        TestCacheSubscriberImpl<String,SimpleData> subscriber = new TestCacheSubscriberImpl<>(mockConnector, cache, locator, new AlwayAcceptingCacheFilter<String,SimpleData>());

        SerializerFactoryDefaultImpl<String,SimpleData> serializerFactory = new  SerializerFactoryDefaultImpl<String,SimpleData>();

        mockConnector.mockSession.initialStringRead = PropertyUtils.createPropertyString(
                CachePublisher.ARG_SESSION_ID, 1001,
                CachePublisher.ARG_SERIALIZER_FACTORY, serializerFactory.getClass().getName(),
                CachePublisher.ARG_CONNECTION_COUNT, 1
        );

        SimpleCacheListener<String,SimpleData> listener = new SimpleCacheListener<>();
        cache.addListener(listener);
        // we always get an onActive and onInitialUpdate in a newly added listener
        // in this case there is no data in the cache yet

        SimpleData d1 = new SimpleData("d1", 1);
        SimpleData d2 = new SimpleData("d2", 1);
        SimpleData d3 = new SimpleData("d3", 1);
        cache.put("d1", d1);
        cache.put("d2", d2);
        cache.put("d3", d3);

        Serializer<String, SimpleData> serializer = serializerFactory.create();

        // set up a  'mock' removal message


        byte[] data2 = serializer.serialize("d2", null, 0) ;
        BlockIO.Header header2 =  new BlockIO.Header();
        header2.flag = BlockIoImpl.DATA_REMOVAL_FLAG;
        header2.dataLength = data2.length;
        mockConnector.mockSession.headerToRead.add(header2);
        mockConnector.mockSession.dataToRead.add(data2);

        // once all 'mock' Header/Block data has been read, an IOException will be riased
        // to simulate a disconnection

        log.info("subscriber should read string message: [{}]", mockConnector.mockSession.initialStringRead);

        subscriber.useDummySubscriptionLoop = false; // this test will go through the main loop

        subscriber.subscribe(15/*heartbet-seconds*/);

        subscriber.awaitConnectionError();

        log.info("subsciption: sessionId=[{}] thread=[{}] subscriptionLoopEntered=[{}]",subscriber.getSessionId(), subscriber.getSubscriptionThread(), subscriber.subscriptionLoopEntered );

        Assert.assertTrue("subscription loop entered ", subscriber.subscriptionLoopEntered);
        Assert.assertTrue("connection error detected ", subscriber.gotConnectionError);
        Assert.assertNotNull("thread started ", subscriber.getSubscriptionThread());


        listener.awaitOnRemoveCount(1);


        log.info("counters: onActiveCount={} onInitialUpdateCount={} onInitialUpdateMapSize={} onUpdateCount={} onRemoveCount={} onDataStaleCount={} cache-size={}",
                listener.onActiveCount,listener.onInitialUpdateCount, listener.onInitialUpdateMapSize,
                listener.onUpdateCount,listener.onRemoveCount,listener.onDataStaleCount, cache.size());

        log.info("cache key set {}", cache.keySet());

        Assert.assertEquals("cache reflects data removal ", 2, cache.size());

        Assert.assertEquals("onActiveCount ", 1, listener.onActiveCount);
        Assert.assertEquals("onInitialUpdateCount", 1, listener.onInitialUpdateCount);
        Assert.assertEquals("onUpdateCount", 3, listener.onUpdateCount);
        Assert.assertEquals("onRemoveCount", 1, listener.onRemoveCount);
        Assert.assertEquals("onDataStaleCount", 1, listener.onDataStaleCount);

    }

    @Test
    public void cacheSubscriptionMainLoopStaleTest() throws InterruptedException {
        log.info("##cacheSubscriptionMainLoopStaleTest");
        doCacheSubscriptionMainLoopStaleAndActiveTest(false) ;
    }

    @Test
    public void cacheSubscriptionMainLoopActiveTest() throws InterruptedException {
        log.info("##cacheSubscriptionMainLoopActivelTest");
        doCacheSubscriptionMainLoopStaleAndActiveTest(true);
    }


    private void doCacheSubscriptionMainLoopStaleAndActiveTest(boolean makeActive) throws InterruptedException {

        log.info("##cacheSubscriptionMainLoopStaleAndActiveTest makeActive={}", makeActive);

        MockClientConnector mockConnector = new MockClientConnector();
        CacheImplClientSide<String,SimpleData> cache = (CacheImplClientSide)CacheImplClientSide.createLocalCache("TestCache");
        CacheLocator locator = new MockCacheLocator();
        TestCacheSubscriberImpl<String,SimpleData> subscriber = new TestCacheSubscriberImpl<>(mockConnector, cache, locator, new AlwayAcceptingCacheFilter<String,SimpleData>());

        SerializerFactoryDefaultImpl<String,SimpleData> serializerFactory = new  SerializerFactoryDefaultImpl<String,SimpleData>();

        mockConnector.mockSession.initialStringRead = PropertyUtils.createPropertyString(
                CachePublisher.ARG_SESSION_ID, 1001,
                CachePublisher.ARG_SERIALIZER_FACTORY, serializerFactory.getClass().getName(),
                CachePublisher.ARG_CONNECTION_COUNT, 1
        );

        SimpleCacheListener<String,SimpleData> listener = new SimpleCacheListener<>();
        cache.addListener(listener);
        // we always get an onActive and onInitialUpdate in a newly added listener
        // in this case there is no data in the cache yet

        SimpleData d1 = new SimpleData("d1", 1);
        cache.put("d1", d1);


        Serializer<String, SimpleData> serializer = serializerFactory.create();

        // set up a  'mock' stale message

        mockConnector.mockSession.enableNetDisconnectDelay(); // the reader will wait until we decide when the
                                                              // network disconnect simulation will be

        // set up a singe 'mock' read  of a heartbeat
        BlockIO.Header header =  new BlockIO.Header();
        header.flag = BlockIoImpl.STALE_FLAG;
        header.dataLength = 1;
        mockConnector.mockSession.headerToRead.add(header);
        byte[] staleMsg = new byte[1];
        staleMsg[0] = 's';
        mockConnector.mockSession.dataToRead.add(staleMsg);

        if( makeActive ) {
            // make the cache active immediately
            BlockIO.Header header2 =  new BlockIO.Header();
            header2.flag = BlockIoImpl.UN_STALE_FLAG;
            header2.dataLength = 1;
            mockConnector.mockSession.headerToRead.add(header2);
            byte[] activeMsg = new byte[1];
            staleMsg[0] = 'a';
            mockConnector.mockSession.dataToRead.add(activeMsg);
        }

        // once all 'mock' Header/Block data has been read, an IOException will be riased
        // to simulate a disconnection

        log.info("subscriber should read string message: [{}]", mockConnector.mockSession.initialStringRead);

        subscriber.useDummySubscriptionLoop = false; // this test will go through the main loop

        subscriber.subscribe(15/*heartbet-seconds*/);

        listener.awaitOnDataStaleCount(1);
        if( makeActive ) {
            listener.awaitOnActiveCount(2);
        }
        else {
            listener.awaitOnActiveCount(1);
        }


        log.info("subsciption: sessionId=[{}] thread=[{}] subscriptionLoopEntered=[{}]",subscriber.getSessionId(), subscriber.getSubscriptionThread(), subscriber.subscriptionLoopEntered );

        Assert.assertTrue("subscription loop entered ", subscriber.subscriptionLoopEntered);
        //Assert.assertTrue("connection error detected ", subscriber.gotConnectionError);
        Assert.assertNotNull("thread started ", subscriber.getSubscriptionThread());

        log.info("counters: onActiveCount={} onInitialUpdateCount={} onInitialUpdateMapSize={} onUpdateCount={} onRemoveCount={} onDataStaleCount={} cache-size={}",
                listener.onActiveCount,listener.onInitialUpdateCount, listener.onInitialUpdateMapSize,
                listener.onUpdateCount,listener.onRemoveCount,listener.onDataStaleCount, cache.size());

        log.info("cache is-stale {}", cache.isStale());

        if( makeActive ) {
            Assert.assertEquals("onActiveCount ", 2, listener.onActiveCount);
            Assert.assertTrue("onDataStaleCount", listener.onDataStaleCount >= 1);
        }
        else {
            Assert.assertEquals("onActiveCount ", 1, listener.onActiveCount);
            Assert.assertTrue("onDataStaleCount", listener.onDataStaleCount >= 1);

        }

        mockConnector.mockSession.triggeNetDisconnectDelay();
                // trigger the 'mock disconnect' now just to end the reading thread

    }

    @Test
    public void cacheSubscriptionMainLoopInitialUpdateTest() throws InterruptedException {
        log.info("##cacheSubscriptionMainLoopInitialUpdateTest");
        doCacheSubscriptionMainLoopInitialUpdateTest(false);
    }

    @Test
    public void cacheSubscriptionMainLoopInitialUpdateReconnectTest() throws InterruptedException {
        log.info("##cacheSubscriptionMainLoopInitialUpdateReconnectTest");
        doCacheSubscriptionMainLoopInitialUpdateTest(true);
    }

    private void doCacheSubscriptionMainLoopInitialUpdateTest(boolean reconnectionLogicTest) throws InterruptedException {

        log.info("#doCacheSubscriptionMainLoopInitialUpdateTest netError={}",reconnectionLogicTest);

        byte[] updateA = InitialUpdateReaderWriterTest.loadDataFromFile(InitialUpdateReaderWriterTest.DATA_FILE_2a);
        byte[] updateB = InitialUpdateReaderWriterTest.loadDataFromFile(InitialUpdateReaderWriterTest.DATA_FILE_2b);
        if( updateA == null || updateB == null ) {
            log.warn("cacheSubscriptionMainLoopInitialUpdateTest will be postponed until InitialUpdateReaderWriterTest has run");
            return;
        }

        byte[] dataA = new byte[updateA.length-BlockIoImpl.HEADER_LENGTH];
        System.arraycopy(updateA, BlockIoImpl.HEADER_LENGTH, dataA, 0, dataA.length);
        byte[] dataB = new byte[updateB.length-BlockIoImpl.HEADER_LENGTH];
        System.arraycopy(updateB, BlockIoImpl.HEADER_LENGTH, dataB, 0, dataB.length);
                            // we just striped off the leading 5 header bytes (which is what this tests wants)

        updateA =  updateB = null; // detect errors in the test


        MockClientConnector mockConnector = new MockClientConnector();

        CacheImplClientSide<String,DataWithDate> cache;
        if( reconnectionLogicTest ) {
            cache = (CacheImplClientSide)CacheImplClientSide.createLocalCache("TestCache");
                // cache is started just as it would be if we were making a re-connection after
                // a network error
        }
        else {
            cache = new CacheImplClientSide<>("TestCache");
                //NB: cache is created but not started - it should get started by the subscriber
                //when it detects the end of the initial download
        }

        CacheLocator locator = new MockCacheLocator();
        TestCacheSubscriberImpl<String,DataWithDate> subscriber = new TestCacheSubscriberImpl<>(mockConnector, cache, locator, new AlwayAcceptingCacheFilter<String,DataWithDate>());

        SerializerFactoryDefaultImpl<String,DataWithDate> serializerFactory = new  SerializerFactoryDefaultImpl<>();

        mockConnector.mockSession.initialStringRead = PropertyUtils.createPropertyString(
                CachePublisher.ARG_SESSION_ID, 1001,
                CachePublisher.ARG_SERIALIZER_FACTORY, serializerFactory.getClass().getName(),
                CachePublisher.ARG_CONNECTION_COUNT, 1
        );

        SimpleCacheListener<String,DataWithDate> listener = new SimpleCacheListener<>();
        cache.addListener(listener);
            // we will get an OnActive and an OnInitialUpdate

        if( reconnectionLogicTest ) {
            // if a network error occurs, this flag is set, and attempts
            // to re-connect are made - eventually a reconnectiuon will be made
            cache.setNetworkErrorState();
        }
        Serializer<String, DataWithDate> serializer = serializerFactory.create();

        // set up a  'mock' read  of a data item [1]

        BlockIO.Header header1 =  new BlockIO.Header();
        header1.flag = BlockIoImpl.INITIAL_UPDATE_FLAG;
        header1.dataLength = dataA.length;
        mockConnector.mockSession.headerToRead.add(header1);
        mockConnector.mockSession.dataToRead.add(dataA);

        // set up a  'mock' read  of a data item [2]

        BlockIO.Header header2 =  new BlockIO.Header();
        header2.flag = BlockIoImpl.INITIAL_UPDATE_COMPLETED_FLAG;
        header2.dataLength = dataB.length;
        mockConnector.mockSession.headerToRead.add(header2);
        mockConnector.mockSession.dataToRead.add(dataB);


        // once all 'mock' Header/Block data has been read, an IOException will be raised
        // to simulate a disconnection

        log.info("subscriber should read string message: [{}]", mockConnector.mockSession.initialStringRead);

        subscriber.useDummySubscriptionLoop = false; // this test will go through the main loop

        subscriber.subscribe(15/*heartbet-seconds*/);

        subscriber.awaitConnectionError();


        log.info("subsciption: sessionId=[{}] thread=[{}] subscriptionLoopEntered=[{}]",subscriber.getSessionId(), subscriber.getSubscriptionThread(), subscriber.subscriptionLoopEntered );

        log.info("cache key set {}", cache.keySet());

        Assert.assertTrue("subscription loop entered ", subscriber.subscriptionLoopEntered);
        Assert.assertTrue("connection error detected ", subscriber.gotConnectionError);
        Assert.assertNotNull("thread started ", subscriber.getSubscriptionThread());
        Assert.assertEquals("data copied to cache ", 5, cache.size());

        DataWithDate d1 = cache.get("d1");
        Assert.assertNotNull("d1 exists in map", d1);
        DataWithDate d2 = cache.get("d2");
        Assert.assertNotNull("d2 exists in map", d2);
        DataWithDate d3 = cache.get("d3");
        Assert.assertNotNull("d3 exists in map", d3);

        DataWithDate dd1 = cache.get("dd1");
        Assert.assertNotNull("dd1 exists in map", dd1);
        DataWithDate dd2 = cache.get("dd2");
        Assert.assertNotNull("dd2 exists in map", dd2);

        listener.awaitOnInitialUpdateCount(1);


        log.info("counters: onActiveCount={} onInitialUpdateCount={} onInitialUpdateMapSize={} onUpdateCount={} onRemoveCount={} onDataStaleCount={}",
                listener.onActiveCount,listener.onInitialUpdateCount, listener.onInitialUpdateMapSize, listener.onUpdateCount,listener.onRemoveCount,listener.onDataStaleCount);

        if( reconnectionLogicTest ) {
            Assert.assertEquals("onActiveCount ", 2, listener.onActiveCount);
            Assert.assertEquals("onInitialUpdateCount", 2, listener.onInitialUpdateCount);
        }
        else {
            Assert.assertEquals("onActiveCount ", 1, listener.onActiveCount);
            Assert.assertEquals("onInitialUpdateCount", 1, listener.onInitialUpdateCount);
        }
        Assert.assertEquals("onInitialUpdateMapSize", 5, listener.onInitialUpdateMapSize);
        Assert.assertEquals("onUpdateCount", 0, listener.onUpdateCount);
        Assert.assertEquals("onRemoveCount", 0, listener.onRemoveCount);
        Assert.assertTrue("onDataStaleCount", listener.onDataStaleCount>=1);

    }

    @Test
    public void cacheSubscriptionMainLoopInitialUpdateEmptySectionTest() throws InterruptedException {

        log.info("##cacheSubscriptionMainLoopInitialUpdateEmptySectionTest");

        byte[] updateA = InitialUpdateReaderWriterTest.loadDataFromFile(InitialUpdateReaderWriterTest.DATA_FILE_3a);
        byte[] updateB = InitialUpdateReaderWriterTest.loadDataFromFile(InitialUpdateReaderWriterTest.DATA_FILE_3b);
        if( updateA == null || updateB == null ) {
            log.warn("cacheSubscriptionMainLoopInitialUpdateTest will be postponed until InitialUpdateReaderWriterTest has run");
            return;
        }

        byte[] dataA = new byte[updateA.length-BlockIoImpl.HEADER_LENGTH];
        System.arraycopy(updateA, BlockIoImpl.HEADER_LENGTH, dataA, 0, dataA.length);
        byte[] dataB = new byte[updateB.length-BlockIoImpl.HEADER_LENGTH];
        System.arraycopy(updateB, BlockIoImpl.HEADER_LENGTH, dataB, 0, dataB.length);
        updateA =  updateB = null; // detect errors in the test
        // strip off the leading 5 header bytes (which is what this tests wants)

        MockClientConnector mockConnector = new MockClientConnector();
        CacheImplClientSide<String,DataWithDate> cache = (CacheImplClientSide)CacheImplClientSide.createLocalCache("TestCache");
        CacheLocator locator = new MockCacheLocator();
        TestCacheSubscriberImpl<String,DataWithDate> subscriber = new TestCacheSubscriberImpl<>(mockConnector, cache, locator, new AlwayAcceptingCacheFilter<String,DataWithDate>());

        SerializerFactoryDefaultImpl<String,DataWithDate> serializerFactory = new  SerializerFactoryDefaultImpl<>();

        mockConnector.mockSession.initialStringRead = PropertyUtils.createPropertyString(
                CachePublisher.ARG_SESSION_ID, 1001,
                CachePublisher.ARG_SERIALIZER_FACTORY, serializerFactory.getClass().getName(),
                CachePublisher.ARG_CONNECTION_COUNT, 1
        );

        Serializer<String, DataWithDate> serializer = serializerFactory.create();

        // set up a  'mock' read  of a data item [1]

        BlockIO.Header header1 =  new BlockIO.Header();
        header1.flag = BlockIoImpl.INITIAL_UPDATE_FLAG;
        header1.dataLength = dataA.length;
        mockConnector.mockSession.headerToRead.add(header1);
        mockConnector.mockSession.dataToRead.add(dataA);

        // set up a  'mock' read  of a data item [2]

        BlockIO.Header header2 =  new BlockIO.Header();
        header2.flag = BlockIoImpl.INITIAL_UPDATE_COMPLETED_FLAG;
        header2.dataLength = dataB.length;
        mockConnector.mockSession.headerToRead.add(header2);
        mockConnector.mockSession.dataToRead.add(dataB);


        // once all 'mock' Header/Block data has been read, an IOException will be raised
        // to simulate a disconnection

        log.info("subscriber should read string message: [{}]", mockConnector.mockSession.initialStringRead);

        subscriber.useDummySubscriptionLoop = false; // this test will go through the main loop

        subscriber.subscribe(15/*heartbet-seconds*/);

        subscriber.awaitConnectionError();


        log.info("subsciption: sessionId=[{}] thread=[{}] subscriptionLoopEntered=[{}]",subscriber.getSessionId(), subscriber.getSubscriptionThread(), subscriber.subscriptionLoopEntered );

        log.info("cache key set {}", cache.keySet());

        Assert.assertTrue("subscription loop entered ", subscriber.subscriptionLoopEntered);
        Assert.assertTrue("connection error detected ", subscriber.gotConnectionError);
        Assert.assertNotNull("thread started ", subscriber.getSubscriptionThread());
        Assert.assertEquals("data copied to cache ", 3, cache.size());

        DataWithDate d1 = cache.get("d1");
        Assert.assertNotNull("d1 exists in map", d1);
        DataWithDate d2 = cache.get("d2");
        Assert.assertNotNull("d2 exists in map", d2);
        DataWithDate d3 = cache.get("d3");
        Assert.assertNotNull("d3 exists in map", d3);

    }

}
