package uk.co.octatec.scdds.cache;
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
import uk.co.octatec.scdds.cache.publish.*;
import uk.co.octatec.scdds.cache.publish.threading.ThreaderImpl;
import uk.co.octatec.scdds.utilities.AwaitParams;
import uk.co.octatec.scdds.utilities.SimpleCacheListener;
import uk.co.octatec.scdds.utilities.SimpleData;
import uk.co.octatec.scdds.mock.MockGeneralRequestHandler;
import uk.co.octatec.scdds.mock.MockServerSession;
import uk.co.octatec.scdds.net.serialize.Serializer;
import uk.co.octatec.scdds.net.serialize.SerializerFactory;
import uk.co.octatec.scdds.net.serialize.SerializerFactoryDefaultImpl;
import uk.co.octatec.scdds.net.serialize.SerializerUtils;
import uk.co.octatec.scdds.net.socket.BlockIoImpl;

import java.util.Properties;

/**
 * Created by Jeromy Drake on 04/05/2016.
 */
public class CachePublisherTest {

    // test the serevr-side cache-publisher handles all requests correctly -
    // the tcp/ip session is mocked-out

    private final Logger log = LoggerFactory.getLogger(CachePublisherTest.class);

    @BeforeClass
    public static void setup() {
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");
    }


    public static class MyCacheFilter implements CacheFilter<String, SimpleData> {

        private static final Logger log = LoggerFactory.getLogger(MyCacheFilter.class);

        static MyCacheFilter theFilter;
        volatile boolean filterUsed;
        volatile String filteredOut;

        @Override
        public void init(String data) {
            theFilter = this;
        }

        @Override
        public boolean accept(String key, SimpleData value) {
            filterUsed = true;
            if( key.equals("d1")) {
                filteredOut = key;
                return false;
            }
            filteredOut = null;
            return true;
        }

        static void awaitFilterBeingUsed() throws InterruptedException {
            for (int i = 0; i < AwaitParams.AWAIT_LOOP_COUNT; i++) {
                if (MyCacheFilter.theFilter != null && MyCacheFilter.theFilter.filterUsed) {
                    break;
                }
                Thread.sleep(AwaitParams.AWAIT_LOOP_COUNT);
            }
            if( MyCacheFilter.theFilter == null ) {
                log.warn("*** awaitFilterBeingUsed: wait failed, filter not created");
            }
            else if( MyCacheFilter.theFilter.filterUsed ) {
                log.warn("*** awaitFilterBeingUsed: wait failed, filter creted but not used");
            }
        }
    }

    @Test
    public void cachePublisherFactoryTest() {

        log.info("##cachePublisherFactoryTest");

        SerializerFactoryDefaultImpl<String, SimpleData> serializerFactory = new SerializerFactoryDefaultImpl<>();
        CachePublisherFactoryDefaultImpl<String, SimpleData> publisherFactory = new CachePublisherFactoryDefaultImpl<>();
        Cache<String,SimpleData> cache = CacheImpl.createLocalCache("TestCache");
        Assert.assertNotNull("cache not null", cache);
        CachePublisher<String,SimpleData> cachePublisher = publisherFactory.create((CacheImpl<String,SimpleData>)cache,
                                                 serializerFactory, new MockGeneralRequestHandler(), new ThreaderImpl());
        Assert.assertNotNull("cachePublisher not null", cachePublisher);
    }

    @Test
    public void cachePublisherInitializationOkTest() throws InterruptedException {

        // test the cache publisher at initialization

        log.info("## cachePublisherInitializationOkTest");

        MockServerSession mockServerSession = new MockServerSession();

        SerializerFactoryDefaultImpl<String, SimpleData> serializerFactory = new SerializerFactoryDefaultImpl<>();
        Cache<String, SimpleData> cache = CacheImpl.createLocalCache("TestCache");
        CachePublisherImpl<String, SimpleData> cachePublisher = new CachePublisherImpl<String, SimpleData>(
                (CacheImpl<String, SimpleData>) cache, serializerFactory,
                new MockGeneralRequestHandler(),
                mockServerSession);

        mockServerSession.mockSession.initialStringRead = "request=subscribe:cache-name=TestCache:filter=null";
                                // this is what the cache-publisher reads first -
                                // a mock subscription request
                                // NB: the cache name must match the cache being published

        cachePublisher.start();

        mockServerSession.mockSession.awaitLastStringWrite(); // wait for the cache publisher to write a reply

        // check the response of the publisher to the client

        log.info("publisher wrote to new client: [{}]", mockServerSession.mockSession.lastStringWrite);
                                                        // this is the response of the publisher to the client

        if( mockServerSession.mockSession.lastStringWrite == null ) {
            log.warn("the cache publisher doesn't seem to have written anything");
        }
        Assert.assertNotNull("last write is not null", mockServerSession.mockSession.lastStringWrite);

        Properties properties = PropertyUtils.getPropertiesFromString(mockServerSession.mockSession.lastStringWrite);

        String errorTest = properties.getProperty(CachePublisher.ARG_ERROR);
        Assert.assertNull("errorTest is null - i.e. no error occured", errorTest);

        String sessionId = properties.getProperty(CachePublisher.ARG_SESSION_ID);
        log.info("session-id = {}", sessionId);
        Assert.assertNotNull("session-id is not null", sessionId);
        Assert.assertTrue("session-id is not 0", Integer.parseInt(sessionId) >= 1);

        // the publisher tells the client how to create a serializer with a factory class
        // check that is a valid class that can be created

        String factoryClassName = properties.getProperty(CachePublisher.ARG_SERIALIZER_FACTORY);
        Assert.assertNotNull("factory class name passed", factoryClassName);
        Class factoryClass = null;
        try {
            factoryClass = Class.forName(factoryClassName);
        } catch (Exception e) {
            log.error("no class [{}]", factoryClassName);
        }
        log.info("factory class [{}] [{}]", factoryClass, factoryClassName);

        Assert.assertNotNull("factory class available", factoryClass);
    }

    @Test
    public void cachePublisherInitializationWithWrongCacheNameTest() throws InterruptedException{

        log.info("## cachePublisherInitializationWithWrongCacheNameTest");
        cachePublisherInitializationWithError(false/*wrong cache name*/);
    }

    @Test
    public void cachePublisherInitializationWithWrongRequestTest() throws InterruptedException{

        log.info("## cachePublisherInitializationWithWrongRequestTest");
        cachePublisherInitializationWithError(true/*wrong request*/);
    }

    private void cachePublisherInitializationWithError(boolean missingRequest) throws InterruptedException{

        // check the publisher sends a correct error message when asked for the wrong cache

        log.info("## cachePublisherInitializationWithErrorTest missingRequest={}", missingRequest);

        MockServerSession mockServerSession = new MockServerSession();

        SerializerFactoryDefaultImpl<String, SimpleData> serializerFactory = new SerializerFactoryDefaultImpl<>();
        Cache<String,SimpleData> cache = CacheImpl.createLocalCache("TestCache");
        CachePublisherImpl<String,SimpleData> cachePublisher  = new CachePublisherImpl<String,SimpleData>(
                                                                            (CacheImpl<String,SimpleData>)cache,
                                                                            serializerFactory,
                                                                            new MockGeneralRequestHandler(),
                                                                            mockServerSession);

        if( missingRequest ) {
            mockServerSession.mockSession.initialStringRead = "request=xxx:cache-name=TestCache:filter=null";
        }
        else {
            mockServerSession.mockSession.initialStringRead = "request=subscribe:cache-name=AnotherCache:filter=null";
        }
                                    // this is what the cache-publisher reads first -
                                    // a mock subscription request
                                    // NB: the cache name is wrong, so an error will be generated

        cachePublisher.start();

        mockServerSession.mockSession.awaitLastStringWrite(); // give the publisher chance to actually start

        log.info("publisher wrote to new client: [{}]", mockServerSession.mockSession.lastStringWrite);
                                                        // this is the response of the publisher to the client

        if( mockServerSession.mockSession.lastStringWrite == null ) {
            log.warn("the cache publisher doesn't seem to have written anything");
        }
        else {
            log.info("reply from publisher [{}]", mockServerSession.mockSession.lastStringWrite);
        }
        Assert.assertNotNull("last write is not null", mockServerSession.mockSession.lastStringWrite);

        Properties properties = PropertyUtils.getPropertiesFromString(mockServerSession.mockSession.lastStringWrite);
        log.info("reply-properties {}", properties);

        String errorTest = properties.getProperty(CachePublisher.ARG_ERROR);
        Assert.assertNotNull("errorTest is not null - i.e. error occurred", errorTest);
        if( missingRequest ) {
            Assert.assertEquals("errorTest is corect", errorTest, "wrong-or-missing-request");
        }
        else {
            Assert.assertEquals("errorTest is corect", errorTest, "wrong-cache-name");
        }

        String sessionId = properties.getProperty(CachePublisher.ARG_SESSION_ID);
        log.info("session-id = {}", sessionId);
        Assert.assertNull("session-id is  null", sessionId);
     }


    // the next series of tests use a standard method   doCachePublisherRunningTest()
    // that changes its behaviour accoring to parameters...

    @Test
    public void cachePublisherRunningTestWithInitialLoad() throws InterruptedException {
        log.info("## cachePublisherRunningTestWithInitialLoad");
        doCachePublisherRunningTest(false/*do not use a filter*/, true/*initial data  load will contain data*/);
    }

    @Test
    public void cachePublisherFilterRunningTest() throws InterruptedException {
        log.info("## cachePublisherFilterRunningTest");
        doCachePublisherRunningTest(true/*apply filter*/, false /*initial data load will NOT contain data*/);
    }

    @Test
    public void cachePublisherFilterRunningTestWithInitialLoad() throws InterruptedException {
        log.info("## cachePublisherFilterRunningTestWithInitialLoad");
        doCachePublisherRunningTest(true/*apply filter*/, true/*initial data  loda will contain data*/);
    }

    @Test
    public void cachePublisherRunningTest() throws InterruptedException {
        log.info("## cachePublisherIRunningTest");
        doCachePublisherRunningTest(false/*do not use a filter*/, false/*initial data load will NOT contain data*/);
    }


    private void doCachePublisherRunningTest(boolean usingFilter, boolean initialLoadHasData) throws InterruptedException {

        // a mock client-connection subscribes and some data is added to the cache -
        // it should get published (i.e. written into the MockSession )

        log.info("## doCachePublisherIRunningTest usingFilter=[{}] initialLoad=[{}]", usingFilter, initialLoadHasData);

        MockServerSession mockServerSession = new MockServerSession();

        SerializerFactoryDefaultImpl<String, SimpleData> serializerFactory = new SerializerFactoryDefaultImpl<>();
        Cache<String, SimpleData> cache = CacheImpl.createLocalCache("TestCache");
        CachePublisherImpl<String, SimpleData> cachePublisher = new CachePublisherImpl<String, SimpleData>(
                (CacheImpl<String, SimpleData>) cache, serializerFactory,
                new MockGeneralRequestHandler(),
                mockServerSession);

        if( initialLoadHasData ) {
            log.info("ensure cache has some data that it can send in an initial load");
            SimpleCacheListener<String, SimpleData> localListener = new  SimpleCacheListener<>();
            ((CacheImpl<String, SimpleData>) cache).addListener(localListener);
            SimpleData d1 = new SimpleData("d1", -1);
            cache.put("d1", d1);
            SimpleData d2 = new SimpleData("d2", -2);
            cache.put("d2", d2);
            // we add some data to the cache -
            // we don't want the publisher to see data being added like this - that would confuse the test
            // so we wait until all update-notifications have been sent, i.e. our listener has been notified
            // then we have a clean start from where to set the publisher
            localListener.awaitOnUpdateCount(2);
        }

        ((CacheImpl) cache).setCachePublisher(cachePublisher);

        // now simulate a subscription request from a client
        // this is what the cache-publisher reads first -  a mock subscription request...

        if (usingFilter) {
            // add the name of a filter to the subscription request message
            CacheFilter<String, SimpleData> filter = new MyCacheFilter();
            mockServerSession.mockSession.initialStringRead = "request=subscribe:cache-name=TestCache:filter=" + filter.getClass().getName();
        } else {
            mockServerSession.mockSession.initialStringRead = "request=subscribe:cache-name=TestCache:filter=null";
        }

        log.info("initialized initialStringRead [{}]", mockServerSession.mockSession.initialStringRead);

        mockServerSession.mockSession.prevLastBytetWrite = null; // initialize data in the mock session so we can
        mockServerSession.mockSession.lastStringWrite = null;    // clearly see whats been written

        cachePublisher.start();

        mockServerSession.mockSession.enableNetDisconnectDelay();
                // this just means that anyone trying to read from the MockSession will wait -
                // actually in this test, no one will be trying to read

        mockServerSession.mockSession.awaitLastStringWrite(); // wait for the publisher to write its data to the MockSession

        // this is the response of the publisher to the client

        log.info("publisher wrote to new client: [{}]", mockServerSession.mockSession.lastStringWrite);

        if (mockServerSession.mockSession.lastStringWrite == null) {
            log.warn("the cache publisher doesn't seem to have written anything");
        }

        // check the response is as expected ...

        Assert.assertNotNull("last write is not null", mockServerSession.mockSession.lastStringWrite);

        Properties properties = PropertyUtils.getPropertiesFromString(mockServerSession.mockSession.lastStringWrite);

        String errorTest = properties.getProperty(CachePublisher.ARG_ERROR);
        Assert.assertNull("errorTest is null - i.e. no error occured", errorTest);

        String sessionId = properties.getProperty(CachePublisher.ARG_SESSION_ID);
        log.info("session-id = {}", sessionId);
        Assert.assertNotNull("session-id is not null", sessionId);
        Assert.assertTrue("session-id is not 0", Integer.parseInt(sessionId) >= 1);

        // ... the server tells the client the serializer factory class to use, it must be available to the client to create...

        String factoryClassName = properties.getProperty(CachePublisher.ARG_SERIALIZER_FACTORY);
        Assert.assertNotNull("factory class name passed", factoryClassName);
        Class factoryClass = null;
        try {
            factoryClass = Class.forName(factoryClassName);
        } catch (Exception e) {
            log.error("no class [{}]", factoryClassName);
        }
        log.info("factory class [{}] [{}]", factoryClass, factoryClassName);

        Assert.assertNotNull("factory class available", factoryClass);

        mockServerSession.mockSession.awaitLastByteWrite();

        Assert.assertNotNull("message has been been published", mockServerSession.mockSession.lastBytetWrite);

        log.info("(1)published msg flag [{}]", ((char) mockServerSession.mockSession.lastBytetWrite[BlockIoImpl.HEADER_FLAG_POS]));

        // check that the message sent by the publisher was as expected, i.e. an INITIAL_UPDATE_COMPLETED message

        Assert.assertEquals("Initial Update Complete Was Last Message",
                mockServerSession.mockSession.lastBytetWrite[BlockIoImpl.HEADER_FLAG_POS], BlockIoImpl.INITIAL_UPDATE_COMPLETED_FLAG);

        if( initialLoadHasData )  {

            // check the data in the initial load is correct - thwe entries will be different dependiong on
            // whether a filter has been used

             int expectedNumberOfEntries = usingFilter?1:2;

            int numDataEntries = SerializerUtils.readIntFromBytes(mockServerSession.mockSession.lastBytetWrite, BlockIoImpl.HEADER_LENGTH);
            log.info("(1)number of data entries [{}], expected-number=[{}]", numDataEntries,expectedNumberOfEntries);
            Assert.assertEquals("number of data entries", expectedNumberOfEntries, numDataEntries);

            int posOfEntry1 =  BlockIoImpl.HEADER_LENGTH+BlockIoImpl.OFFSET_PAST_BLOCK_SIZE ;
            int lengthOfEntry1  =  SerializerUtils.readIntFromBytes(mockServerSession.mockSession.lastBytetWrite, posOfEntry1);
            int posOfData1 =  posOfEntry1 +  BlockIoImpl.HEADER_LENGTH;
            log.info("posOfEntry1 [{}] lengthOfEntry1 [{}] posOfData1 [{}]", posOfEntry1, lengthOfEntry1, posOfData1);
            Serializer<String, SimpleData> serializer = serializerFactory.create();
            Serializer.Pair pair = serializer.deserialize(mockServerSession.mockSession.lastBytetWrite, posOfData1);
            log.info("deserialized 1st entry [{}] [{}]", pair.key, pair.value);
            if( usingFilter ) {
                Assert.assertEquals("correct key d2 (using-filter)", "d2", pair.key);
            }
            else {
                Assert.assertEquals("correct key d1", "d1", pair.key);
            }

            if( !usingFilter ) {
                int posOfEntry2 = posOfData1 + lengthOfEntry1;
                int lengthOfEntry2 = SerializerUtils.readIntFromBytes(mockServerSession.mockSession.lastBytetWrite, posOfEntry2);
                int posOfData2 = posOfEntry2 + BlockIoImpl.HEADER_LENGTH;
                log.info("posOfEntry2 [{}] lengthOfEntry2 [{}] posOfData1 [{}]", posOfEntry2, lengthOfEntry2, posOfData2);
                pair = serializer.deserialize(mockServerSession.mockSession.lastBytetWrite, posOfData2);
                log.info("deserialized 2nd entry [{}] [{}]", pair.key, pair.value);
                Assert.assertEquals("correct key d2", "d2", pair.key);
            }

        }

        subTest_d1(mockServerSession, cache, usingFilter, serializerFactory, false);

        subTest_d2(mockServerSession, cache, usingFilter, serializerFactory);

        subTest_stale(mockServerSession, cache, usingFilter, serializerFactory);

        subTest_active(mockServerSession, cache, usingFilter, serializerFactory);

        cachePublisher.removeSubscription(Long.parseLong(sessionId));

        log.info("test publisher after subscription removed");

        subTest_d1(mockServerSession, cache, usingFilter, serializerFactory, true/* subscription removed*/);

        mockServerSession.mockSession.triggeNetDisconnectDelay();

        ((CacheImpl)cache).dispose();

    }

    void subTest_d1(MockServerSession mockServerSession, Cache<String, SimpleData> cache, boolean usingFilter, SerializerFactory<String, SimpleData> serializerFactory, boolean subscriptionRemoved) throws InterruptedException {

        // now watch the publisher publish some data

        mockServerSession.mockSession.lastBytetWrite = null;
        if (usingFilter) {
            MyCacheFilter.theFilter.filterUsed = false;
            MyCacheFilter.theFilter.filteredOut = null;
        }

        // put an entry in the cache, it should be published of filtered-out

        log.info("subTest_d1 = TESTING ADDMIN ITEM d1 subscriptionRemoved={}", subscriptionRemoved);

        SimpleData d1 = new SimpleData("d1", 1);
        cache.put("d1", d1);

        if (usingFilter) {
            MyCacheFilter.awaitFilterBeingUsed(); // wait for something to be filtered-out
        } else {
            // wait for something to be written
            mockServerSession.mockSession.awaitLastByteWrite();
        }

        log.info("published msg length=[{}]", mockServerSession.mockSession.lastBytetWrite == null ? -1 : mockServerSession.mockSession.lastBytetWrite.length);

        if( subscriptionRemoved ) {
            Assert.assertNull("(pub-removed)no message has been published", mockServerSession.mockSession.lastBytetWrite);
            if (usingFilter) {
                Assert.assertNull("(pub-removed)no message filtered out", MyCacheFilter.theFilter.filteredOut);
                Assert.assertFalse("(pub-removed)filter was not used", MyCacheFilter.theFilter.filterUsed);
            }
            return;
        }

        if (usingFilter) {
            // nothing should be published as the filter should filter ouy the data just added to the cache during the publication phase...

            log.info("(1)myCacheFilter.filterInitCalled=[{}] myCacheFilter.filterUsed=[{}], myCacheFilter.filteredOut=[{}]", MyCacheFilter.theFilter != null, MyCacheFilter.theFilter.filterUsed, MyCacheFilter.theFilter.filteredOut);
            log.info("data: [{}]", mockServerSession.mockSession.lastBytetWrite);
            Assert.assertTrue("(1)filter was initialized", MyCacheFilter.theFilter != null);
            Assert.assertTrue("(1)filter was used", MyCacheFilter.theFilter.filterUsed);
            Assert.assertNotNull("(1)message filtered out", MyCacheFilter.theFilter.filteredOut);
            Assert.assertNull("no data published", mockServerSession.mockSession.lastBytetWrite);
            Assert.assertEquals("correct item was filtered out", "d1", MyCacheFilter.theFilter.filteredOut);
            MyCacheFilter.theFilter.filterUsed = false;
            MyCacheFilter.theFilter.filteredOut = null;
        } else {
            Assert.assertNotNull("(2)message has been published again", mockServerSession.mockSession.lastBytetWrite);
            log.info("(2)published msg flag [{}]", ((char) mockServerSession.mockSession.lastBytetWrite[BlockIoImpl.HEADER_FLAG_POS]));
            Assert.assertEquals("(2)Data Was Last Message",
                    mockServerSession.mockSession.lastBytetWrite[BlockIoImpl.HEADER_FLAG_POS], BlockIoImpl.DATA_FLAG);
            log.info("(2)data: [{}]", mockServerSession.mockSession.lastBytetWrite);
            Serializer<String, SimpleData> serializer = serializerFactory.create();
            Serializer.Pair<String, SimpleData> pair = serializer.deserialize(mockServerSession.mockSession.lastBytetWrite, BlockIoImpl.HEADER_LENGTH);
            log.info("(2)key read: [{}] dat-read [{}]", pair.key, pair.value);
            Assert.assertEquals("(2)key is correct", "d1", pair.key);
            Assert.assertEquals("(2)value is correct", d1, pair.value);
        }

        mockServerSession.mockSession.lastBytetWrite = null;
    }


    void subTest_d2(MockServerSession mockServerSession, Cache<String, SimpleData> cache, boolean usingFilter, SerializerFactory<String, SimpleData> serializerFactory) throws InterruptedException {

        // put a 2nd entry in the cache and check it (this should NOT be filtered out)

        log.info("subTest_d2 - TESTING ADDMIN ITEM d2");

        mockServerSession.mockSession.lastBytetWrite = null;
        if (usingFilter) {
            MyCacheFilter.theFilter.filterUsed = false;
            MyCacheFilter.theFilter.filteredOut = null;
        }

        SimpleData d2 = new SimpleData("d2", 2);
        cache.put("d2", d2);

        mockServerSession.mockSession.awaitLastByteWrite();

        Assert.assertNotNull("message has been published again", mockServerSession.mockSession.lastBytetWrite);

        if (usingFilter) {
            log.info("(3)myCacheFilter.filetredUsed=[{}], myCacheFilter.filteredOut=[{}]", MyCacheFilter.theFilter.filterUsed, MyCacheFilter.theFilter.filteredOut);
            Assert.assertTrue("(3)filter was used", MyCacheFilter.theFilter.filterUsed);
            Assert.assertNull("(3)no item was filtered out", MyCacheFilter.theFilter.filteredOut);
        }

        log.info("(4)published msg flag [{}]", ((char) mockServerSession.mockSession.lastBytetWrite[BlockIoImpl.HEADER_FLAG_POS]));
        Assert.assertEquals("(4)Data Was Last Message",
                mockServerSession.mockSession.lastBytetWrite[BlockIoImpl.HEADER_FLAG_POS], BlockIoImpl.DATA_FLAG);
        log.info("(4)data: [{}]", mockServerSession.mockSession.lastBytetWrite);
        Serializer<String, SimpleData> serializer = serializerFactory.create();
        Serializer.Pair<String, SimpleData> pair = serializer.deserialize(mockServerSession.mockSession.lastBytetWrite, BlockIoImpl.HEADER_LENGTH);
        log.info("(4)key read: [{}] dat-read [{}]", pair.key, pair.value);
        Assert.assertEquals("(4)key is correct", "d2", pair.key);
        Assert.assertEquals("(4)value is correct", d2, pair.value);

        // now remove an item

        log.info("TESTING REMOVING ITEM d1");

        mockServerSession.mockSession.lastBytetWrite = null;
        if (usingFilter) {
            MyCacheFilter.theFilter.filterUsed = false;
            MyCacheFilter.theFilter.filteredOut = null;
        }
        SimpleData dr = cache.remove("d1");
        log.info("item removed: [{}]", dr);
        Assert.assertNotNull("item removed from cache", dr);


        if (usingFilter) {

            MyCacheFilter.awaitFilterBeingUsed();

            log.info("(5)myCacheFilter.filetredUsed=[{}], myCacheFilter.filteredOut=[{}]", MyCacheFilter.theFilter.filterUsed, MyCacheFilter.theFilter.filteredOut);
            log.info("(5)data: [{}]", mockServerSession.mockSession.lastBytetWrite);
            Assert.assertTrue("(5)filter was initialized", MyCacheFilter.theFilter != null);
            Assert.assertTrue("(5)filter was used", MyCacheFilter.theFilter.filterUsed);
            Assert.assertNotNull("(5)message filtered out", MyCacheFilter.theFilter.filteredOut);
            Assert.assertNull("(5)no data published", mockServerSession.mockSession.lastBytetWrite);
            Assert.assertEquals("(5)correct item was filtered out", "d1", MyCacheFilter.theFilter.filteredOut);
        } else {

            mockServerSession.mockSession.awaitLastByteWrite();

            Assert.assertNotNull("(6)message has been published again", mockServerSession.mockSession.lastBytetWrite);
            log.info("(6)published msg flag [{}]", ((char) mockServerSession.mockSession.lastBytetWrite[BlockIoImpl.HEADER_FLAG_POS]));
            Assert.assertEquals("Removed Was Last Message",
                    mockServerSession.mockSession.lastBytetWrite[BlockIoImpl.HEADER_FLAG_POS], BlockIoImpl.DATA_REMOVAL_FLAG);
            log.info("(6) data: [{}]", mockServerSession.mockSession.lastBytetWrite);
            pair = serializer.deserialize(mockServerSession.mockSession.lastBytetWrite, BlockIoImpl.HEADER_LENGTH);
            log.info("(6)removal: key read [{}] data read [{}]", pair.key, pair.value);
            Assert.assertEquals("(6)key is correct", "d1", pair.key);
            Assert.assertNull("(6)value is null", pair.value);

        }
    }

    void subTest_stale(MockServerSession mockServerSession, Cache<String, SimpleData> cache, boolean usingFilter, SerializerFactory<String, SimpleData> serializerFactory) throws InterruptedException {

        // set the cache stale

        log.info("subTest_stale - TESTING SETTING CACHE STALE");

        mockServerSession.mockSession.lastBytetWrite = null;
        if (usingFilter) {
            MyCacheFilter.theFilter.filterUsed = false;
            MyCacheFilter.theFilter.filteredOut = null;
        }

        cache.notifyStale();
        log.info("cache stale state: [{}]", cache.isStale());
        Assert.assertTrue("cache is now stale", cache.isStale());

        mockServerSession.mockSession.awaitLastByteWrite();

        Assert.assertNotNull("(7)message has been published again", mockServerSession.mockSession.lastBytetWrite);
        log.info("(7)published msg flag [{}]", ((char) mockServerSession.mockSession.lastBytetWrite[BlockIoImpl.HEADER_FLAG_POS]));
        Assert.assertEquals("Stale Was Last Message",
                mockServerSession.mockSession.lastBytetWrite[BlockIoImpl.HEADER_FLAG_POS], BlockIoImpl.STALE_FLAG);
        log.info("(7) data: [{}]", mockServerSession.mockSession.lastBytetWrite);

        if (usingFilter) {
            // filter not used for stale message
            Assert.assertFalse("(7)filter was not used", MyCacheFilter.theFilter.filterUsed);
            Assert.assertNull("(7)no message filtered out", MyCacheFilter.theFilter.filteredOut);
        }

    }

    void subTest_active(MockServerSession mockServerSession, Cache<String, SimpleData> cache, boolean usingFilter, SerializerFactory<String, SimpleData> serializerFactory) throws InterruptedException {

        // set the cache active

        log.info("TESTING SETTING CACHE ACTIVE");

        mockServerSession.mockSession.lastBytetWrite = null;
        if( usingFilter ) {
            MyCacheFilter.theFilter.filterUsed = false;
            MyCacheFilter.theFilter.filteredOut = null;
        }

        cache.notifyUnStale();
        log.info("cache stale state: [{}]", cache.isStale());
        Assert.assertFalse("cache is now not-stale", cache.isStale());

        mockServerSession.mockSession.awaitLastByteWrite();

        Assert.assertNotNull("(8)message has been published again", mockServerSession.mockSession.lastBytetWrite);
        log.info("(8)published msg flag [{}]", ((char) mockServerSession.mockSession.lastBytetWrite[BlockIoImpl.HEADER_FLAG_POS]));
        Assert.assertEquals("Active Was Last Message",
                mockServerSession.mockSession.lastBytetWrite[BlockIoImpl.HEADER_FLAG_POS], BlockIoImpl.UN_STALE_FLAG);
        log.info("(8) data: [{}]", mockServerSession.mockSession.lastBytetWrite);

        if( usingFilter ){
            // filter not used for stale message
            Assert.assertFalse("(8)filter was not used", MyCacheFilter.theFilter.filterUsed);
            Assert.assertNull("(8)no message filtered out", MyCacheFilter.theFilter.filteredOut);
        }


    }

}
