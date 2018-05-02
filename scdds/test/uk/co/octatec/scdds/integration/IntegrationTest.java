package uk.co.octatec.scdds.integration;
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

/* =======================================================

these integration tests are very sensitive to timings
(so possibly not very good unit tests, but they are quite useful)

This means they could fail in some (slower) environments
in particular, if debug logging is on, this will slow down
the test and mean the expected events don't happen in time

======================================================= */

import junit.framework.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.GlobalDefaults;
import uk.co.octatec.scdds.GlobalProperties;
import uk.co.octatec.scdds.cache.Cache;
import uk.co.octatec.scdds.cache.PublishingCacheBuilder;
import uk.co.octatec.scdds.cache.SubscriptionCacheBuilder;
import uk.co.octatec.scdds.cache.SubscriptionStreamBuilder;
import uk.co.octatec.scdds.cache.subscribe.CacheSubscriberImpl;
import uk.co.octatec.scdds.cache.subscribe.ImmutableCache;
import uk.co.octatec.scdds.instrumentation.InstrumentedBlockIO;
import uk.co.octatec.scdds.instrumentation.InstrumentedCachePublisherFactory;
import uk.co.octatec.scdds.instrumentation.InstrumentedCacheSubscriberFactory;
import uk.co.octatec.scdds.net.registry.Registry;
import uk.co.octatec.scdds.net.registry.RegistryServer;
import uk.co.octatec.scdds.net.router.Republisher;
import uk.co.octatec.scdds.utilities.AwaitParams;
import uk.co.octatec.scdds.utilities.EvenSimpleDataFilter;
import uk.co.octatec.scdds.utilities.SimpleCacheListener;
import uk.co.octatec.scdds.utilities.SimpleData;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by Jeromy Drake on 07/05/16
 */
public class IntegrationTest {

    final static boolean RUN_INTEGRATION_TESTS = true;
        // in slower environm,ents these tests may fail as they are vety timing dependant, so
        // it may be usefull just to switch them off

    private final Logger log = LoggerFactory.getLogger(IntegrationTest.class);

    enum StartupOrder {
        registry_server_client,
        registry_client_server,
        server_client_registry,
        client_server_registry,

    }

    @BeforeClass
    public static void setup() {

        //System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO");
        System.setProperty(org.slf4j.impl.SimpleLogger.SHOW_DATE_TIME_KEY, "true");
        GlobalProperties.exposeHttpServer = false;
    }


    static class ServerSideCacheStarter implements  Runnable {

        // just start a server-side cache in a thread
        // we start in in a thread so that we can go on and start a registry and a client
        // (normally, you would not need to start it in a thread)

        private final Logger log = LoggerFactory.getLogger(ServerSideCacheStarter.class);

        private static volatile int counter;

        private final String cacheName;
        private final List<InetSocketAddress> registries;

        private Thread thread;

        volatile Cache<String, SimpleData> serverCache = null;

        private ServerSideCacheStarter(String cacheName, List<InetSocketAddress> registries) {
            this.cacheName = cacheName;
            this.registries = registries;
        }

        static ServerSideCacheStarter start(String cacheName, List<InetSocketAddress> registries) throws InterruptedException {
            ++counter;
            ServerSideCacheStarter srv = new ServerSideCacheStarter(cacheName, registries);
            srv.thread = new Thread(srv);
            srv.thread.setDaemon(true);
            srv.thread.setName("ServerSideCacheStarter_"+counter);
            srv.thread.start();
            return srv;
        }

        public void stop() {
            log.info("stop server cache");
            PublishingCacheBuilder.stop(serverCache);
        }

        public void run(){
            log.info("*** START THE SERVER-SIZE CACHE ***");
            PublishingCacheBuilder<String, SimpleData> server = new PublishingCacheBuilder<String, SimpleData>(registries);
            serverCache = server.build(cacheName);
            log.info("*** server build returned ***") ;
            serverCache.put("S1", new SimpleData("S1", 1));
            log.info("*** server side started, one item added ***") ;
            // we now have a cache with 1 item of data, it is registered in the registry so it can be found by a client
        }

        void awaitStartup() throws InterruptedException{
            for(int i=0; i< AwaitParams.AWAIT_LOOP_COUNT; i++) {
                if( serverCache != null ) {
                    break;
                }
                Thread.sleep(AwaitParams.AWAIT_SLEEP_TIME);

            }
            Thread.sleep(AwaitParams.AWAIT_SLEEP_TIME);
        }
    }

    static class ClientSideCacheStarter implements Runnable {

        // just start a client in a thread
        // start the client in a thread so the test can go on and do other things, you
        // woukd not normally need to start a client-cache in its own thread

        private final Logger log = LoggerFactory.getLogger(ClientSideCacheStarter.class);

        private final String cacheName;
        private final List<InetSocketAddress> registries;

        volatile ImmutableCache<String, SimpleData> clientCache = null;
        volatile SimpleCacheListener<String, SimpleData> clientListener = null;

        static int heartBeatSeconds = 15;

        static ClientSideCacheStarter start(String cacheName, List<InetSocketAddress> registries) throws InterruptedException{
            return start(cacheName, registries, 15);
        }

        static ClientSideCacheStarter start(String cacheName, List<InetSocketAddress> registries, int htBt) throws InterruptedException{
            heartBeatSeconds = htBt;
            ClientSideCacheStarter cli = new ClientSideCacheStarter(cacheName, registries);
            Thread t = new Thread(cli);
            t.setDaemon(true);
            t.setName("ClientSideCacheStarter");
            t.start();
            return cli;
        }

        private ClientSideCacheStarter(String cacheName, List<InetSocketAddress> registries) {
            this.cacheName = cacheName;
            this.registries = registries;
        }

        public void run() {
            log.info("*** START THE CLIENT-SIDE CACHE ***");
            SubscriptionCacheBuilder<String, SimpleData> client = new SubscriptionCacheBuilder<>(registries);
            clientCache = client.subscribe(cacheName, null, heartBeatSeconds);
            log.info("*** client subscribe returned ***") ;
            clientListener = new SimpleCacheListener<String, SimpleData>();
            clientCache.addListener(clientListener);
            log.info("*** client side started ***") ;
        }

        void awaitStartup() throws InterruptedException{
            for(int i=0; i< AwaitParams.AWAIT_LOOP_COUNT; i++) {
                if( clientCache != null ) {
                    break;
                }
                Thread.sleep(AwaitParams.AWAIT_SLEEP_TIME);

            }
            Thread.sleep(AwaitParams.AWAIT_SLEEP_TIME);
        }

    }

    private int getFreePort() {
        try {
            InetSocketAddress addr = new InetSocketAddress(0);
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.socket().setReuseAddress(true);
            serverChannel.socket().bind(addr);
            addr = (InetSocketAddress) serverChannel.getLocalAddress();
            int port = addr.getPort();
            serverChannel.close();
            log.info("free-port chosen: [{}]", port);
            return port;
        }
        catch( IOException x ) {
            log.error("can't find a free port");
            throw new RuntimeException("can't find a free port") ;
        }
    }

    @Test
    public void basicTest_Startup_registry_server_client() throws InterruptedException {
        // test the the normal startup sequence
        log.info("## basicTest_Startup_registry_server_client");
        if( !  RUN_INTEGRATION_TESTS ) {
            log.info("integration tests not running");
            return;
        }
        doBasicTest(StartupOrder.registry_server_client);
    }

    @Test
    public void basicTest_Startup_server_client_registry() throws InterruptedException {
        // test that there is no dependency on startup order by starting the registry last
        log.info("## basicTest_Startup_server_client_registry");
        if( !  RUN_INTEGRATION_TESTS ) {
            log.info("integration tests not running");
            return;
        }
        doBasicTest(StartupOrder.server_client_registry);
    }

    private void doBasicTest(StartupOrder startupOrder) throws InterruptedException {

        // start a Registry, and Server and a Client and then put entries into the server-cache
        // and watch them arrive in the client-cache and monitor the client-side listener events

        log.info("basicTest startupOrder=[{}]", startupOrder);

        int registryPort = getFreePort();
        List<InetSocketAddress> registries = new ArrayList<>();
        registries.add(new InetSocketAddress("localhost", registryPort));

        String cacheName = "IntegrationCacheA";

        RegistryServer registry = null;
        if( startupOrder == StartupOrder.registry_server_client || startupOrder == StartupOrder.registry_client_server) {

            // start a registry - normally this will be a separate process on a different box, ideally multiple instances
            // each on a different box, but for the tests the registry will run in the same process

            log.info("*** START THE REGISTRY registryPort={} ***", registryPort);
            registry = RegistryServer.startInThread(registryPort);
            // the clients and servers can be freely moved around to any network location,
            // the only requirement is that the registry(s) be in a known location on a known port

        }
        else {
            GlobalDefaults.CACHE_REGISTRY_RETRY_WAIT = 50; // this will not normally be changed, but it is useful to
            // reduce the period for this test
        }

        // create the server cache

        ServerSideCacheStarter srv = null;
        if( startupOrder == StartupOrder.registry_server_client || startupOrder == StartupOrder.server_client_registry) {
            srv = ServerSideCacheStarter.start(cacheName, registries);
        }

        if( startupOrder == StartupOrder.registry_server_client ) {
            log.info("*** CHECK SERVER HAS REGISTERED (NORMAL STARTUP ORDER) ***");
            srv.awaitStartup(); // registry and server has been started so this should return straight away
            // check the registry to see if the entry is there
            Registry.Entry entry = registry.find(cacheName);
            log.info("registry-entry found [{}]", entry);
            Assert.assertNotNull("registry-entry found", entry);
        }

        ClientSideCacheStarter cli = null;
        if( startupOrder == StartupOrder.registry_server_client || startupOrder == StartupOrder.server_client_registry) {
            // now create a client
            // in reality this would be in a different processes probably on a different machine
            //(if you want to use caches in the same process, just use CacheImpl.createLocalCache()
            cli = ClientSideCacheStarter.start(cacheName, registries);
        }


        if( startupOrder == StartupOrder.server_client_registry || startupOrder == StartupOrder.client_server_registry) {
            Thread.sleep(1500); // make sure client and server will both re-try at least once
            log.info("*** START THE REGISTRY (delayed) registryPort={} ***", registryPort);
            registry = RegistryServer.startInThread(registryPort);
            Thread.sleep(100); // time for server and client to connect to registrt once its running
        }

        // now wait for SERVER and CLIENT to start

        srv.awaitStartup();
        cli.awaitStartup();

        Assert.assertNotNull("server started", srv.serverCache);
        Assert.assertNotNull("client started", cli.clientCache);

        log.info("*** CHECK SERVER HAS REGISTERED - after delayed registry start ***");
        Registry.Entry entry = registry.find(cacheName);
        log.info("registry-entry found [{}]", entry);
        Assert.assertNotNull("registry-entry found", entry);

        AwaitParams.awaitCacheSizeGte(cli.clientCache, 1);

        log.info("(1)client-cache stale?{} key-set {}", cli.clientCache.isStale(), cli.clientCache.keySet());


        Assert.assertFalse("client-cache is not stale", cli.clientCache.isStale());
        Assert.assertEquals("client-cache has 1 item", 1, cli.clientCache.size());
        Assert.assertNotNull("client cache has S1", cli.clientCache.get("S1"));

        SimpleData d1 =  cli.clientCache.get("S1");
        Assert.assertEquals("D1 Value Correct", 1, d1.data2);

        // add a new item in the server and see that it is received in the client cache

        int nextUpdateCount =  cli.clientListener.onUpdateCount+1;
        srv.serverCache.put("S2", new SimpleData("S2", 2));

        cli.clientListener.awaitOnUpdateCount(nextUpdateCount);
        AwaitParams.awaitCacheSizeGte(cli.clientCache, 2);

        log.info("(2)client-cache stale?{} key-set {}", cli.clientCache.isStale(), cli.clientCache.keySet());

        Assert.assertEquals("client-cache has 2 items", 2, cli.clientCache.size());
        Assert.assertNotNull("client cache has S1", cli.clientCache.get("S1"));
        Assert.assertNotNull("client cache has S2", cli.clientCache.get("S2"));

        // add a 3rd item in the server and see that it is received in the client cache

        nextUpdateCount =  cli.clientListener.onUpdateCount+1;
        srv.serverCache.put("S3", new SimpleData("S3", 3));

        cli.clientListener.awaitOnUpdateCount(nextUpdateCount);
        AwaitParams.awaitCacheSizeGte(cli.clientCache, 3);

        log.info("(3)client-cache stale?{} key-set {}", cli.clientCache.isStale(), cli.clientCache.keySet());

        Assert.assertEquals("client-cache has 3 items", 3, cli.clientCache.size());
        Assert.assertNotNull("client cache has S1", cli.clientCache.get("S1"));
        Assert.assertNotNull("client cache has S2", cli.clientCache.get("S2"));
        Assert.assertNotNull("client cache has S3", cli.clientCache.get("S3"));

        SimpleData d2 =  cli.clientCache.get("S2");
        Assert.assertEquals("D2 Value Correct", 2, d2.data2);
        SimpleData d3 =  cli.clientCache.get("S3");
        Assert.assertEquals("D3 Value Correct", 3, d3.data2);

        // update a value

        nextUpdateCount =  cli.clientListener.onUpdateCount+1;
        srv.serverCache.put("S2", new SimpleData("S2", 2000));
        cli.clientListener.awaitOnUpdateCount(nextUpdateCount);
        d2 =  cli.clientCache.get("S2");
        Assert.assertEquals("D2 updated to correct value", 2000, d2.data2);

        // remove an item

        int nextRemovalCount =  cli.clientListener.onRemoveCount+1;
        srv.serverCache.remove("S2");

        cli.clientListener.awaitOnRemoveCount(nextRemovalCount);

        log.info("(4:removal)client-cache stale?{} key-set {}", cli.clientCache.isStale(), cli.clientCache.keySet());
        Assert.assertNotNull("client cache has S1", cli.clientCache.get("S1"));
        Assert.assertNull("client cache does not have S2", cli.clientCache.get("S2"));
        Assert.assertNotNull("client cache has S3", cli.clientCache.get("S3"));

        // server cache notifies stale

        int nextStaleCount =  cli.clientListener.onDataStaleCount+1;
        srv.serverCache.notifyStale();

        cli.clientListener.awaitOnDataStaleCount(nextStaleCount);

        log.info("(5:stale)client-cache stale?{} key-set {}", cli.clientCache.isStale(), cli.clientCache.keySet());
        Assert.assertNotNull("client cache has S1", cli.clientCache.get("S1"));
        Assert.assertNull("client cache does not have S2", cli.clientCache.get("S2"));
        Assert.assertNotNull("client cache has S3", cli.clientCache.get("S3"));
        Assert.assertTrue("client-cache is stale", cli.clientCache.isStale());

        // server cache notifies un-stale

        int nextActiveCount =  cli.clientListener.onActiveCount+1;
        srv.serverCache.notifyUnStale();

        cli.clientListener.awaitOnActiveCount(nextActiveCount);

        log.info("(6:un-stale)client-cache stale?{} key-set {}", cli.clientCache.isStale(), cli.clientCache.keySet());
        Assert.assertNotNull("client cache has S1", cli.clientCache.get("S1"));
        Assert.assertNull("client cache does not have S2", cli.clientCache.get("S2"));
        Assert.assertNotNull("client cache has S3", cli.clientCache.get("S3"));
        Assert.assertFalse("client-cache is not stale", cli.clientCache.isStale());

        // add a 4th item in the server and see that it is added to the  client cache

        nextUpdateCount =  cli.clientListener.onUpdateCount+1;
        srv.serverCache.put("S4", new SimpleData("S4", 4));

        cli.clientListener.awaitOnUpdateCount(nextUpdateCount);

        log.info("(7)client-cache stale?{} key-set {}", cli.clientCache.isStale(), cli.clientCache.keySet());

        Assert.assertEquals("client-cache has 3 items", 3,cli.clientCache.size());
        Assert.assertNotNull("client cache has S1", cli.clientCache.get("S1"));
        Assert.assertNull("client cache does not have S2", cli.clientCache.get("S2"));
        Assert.assertNotNull("client cache has S3", cli.clientCache.get("S3"));
        Assert.assertNotNull("client cache has S4", cli.clientCache.get("S4"));


        // unsubscibe and verify we receive no more messages

        SubscriptionCacheBuilder.unSubscribe(cli.clientCache);

        srv.serverCache.put("S5", new SimpleData("S5", 5));

        cli.clientListener.awaitOnUpdateCount(4);
        cli.clientListener.awaitOnDataStaleCount(2);

        log.info("(7)client-cache stale?{} key-set {}", cli.clientCache.isStale(), cli.clientCache.keySet());

        Assert.assertTrue("client-cache is stale", cli.clientCache.isStale());
        Assert.assertEquals("client-cache has 3 items", 3, cli.clientCache.size());
        Assert.assertNotNull("client cache has S1", cli.clientCache.get("S1"));
        Assert.assertNull("client does not have S2", cli.clientCache.get("S2"));
        Assert.assertNotNull("client cache has S3", cli.clientCache.get("S3"));
        Assert.assertNotNull("client cache has S4", cli.clientCache.get("S4"));
        Assert.assertNull("client cache does not have S5", cli.clientCache.get("S5"));

        // Tidy Up

        RegistryServer.stopInThread(registry);
    }

    @Test
    public void restartServerTest() throws InterruptedException {

        // test that the client can detect the death of a server and can re-connect
        // automatically when the server is re-started

        GlobalDefaults.CACHE_REGISTRY_RETRY_WAIT = 50; // so we don't wait too long for a retry

        log.info("##restartServerTest");
        if( !  RUN_INTEGRATION_TESTS ) {
            log.info("integration tests not running");
            return;
        }

        int registryPort = getFreePort();
        List<InetSocketAddress> registries = new ArrayList<>();
        registries.add(new InetSocketAddress("localhost", registryPort));

        String cacheName = "IntegrationCacheR";

        RegistryServer registry = null;

        // start a registry - normally this will be a separate process on a different box, ideally multiple instances
        // each on a different box, but for the tests the registry will run in the same process

        log.info("*** START THE REGISTRY registryPort={} ***", registryPort);
        registry = RegistryServer.startInThread(registryPort);
        // the clients and servers can be freely moved around to any network location,
        // the only requirement is that the registry(s) be in a known location on a known port

        // create the server cache

        ServerSideCacheStarter srv = null;
        log.info("*** START SERVER ***");
        srv = ServerSideCacheStarter.start(cacheName, registries);
        srv.awaitStartup(); // registry and server has been started so this should return straight away

        // check the registry to see if the entry is there
        Registry.Entry entry = registry.find(cacheName);
        log.info("registry-entry found<1> [{}]", entry);
        Assert.assertNotNull("registry-entry found", entry);

        ClientSideCacheStarter cli = null;

        log.info("*** START CLIENT ***");
        // now create a client
        // in reality this would be in a different processes probably on a different machine
        //(if you want to use caches in the same process, just use CacheImpl.createLocalCache()

        int heartBeatSeconds = 1; // lowest possible heartbeat to detect the missing server as soon as possible
        cli = ClientSideCacheStarter.start(cacheName, registries, heartBeatSeconds);
        cli.awaitStartup();

        Assert.assertNotNull("server started", srv.serverCache);
        Assert.assertNotNull("client started", cli.clientCache);

        AwaitParams.awaitCacheSizeGte(cli.clientCache, 1);

        log.info("(1)client-cache stale?{} key-set {}", cli.clientCache.isStale(), cli.clientCache.keySet());

        Assert.assertFalse("client-cache is not stale", cli.clientCache.isStale());
        Assert.assertEquals("client-cache has 1 item", 1, cli.clientCache.size());
        Assert.assertNotNull("client cache has S1", cli.clientCache.get("S1"));

        // add a new item in the server and see that it is received in the client cache

        srv.serverCache.put("S2", new SimpleData("S2", 2));

        cli.clientListener.awaitOnUpdateCount(1);

        AwaitParams.awaitCacheSizeGte(cli.clientCache, 2);

        log.info("(2)client-cache stale?{} key-set {}", cli.clientCache.isStale(), cli.clientCache.keySet());

        Assert.assertEquals("client-cache has 2 items", 2, cli.clientCache.size());
        Assert.assertNotNull("client cache has S1", cli.clientCache.get("S1"));
        Assert.assertNotNull("client cache has S2", cli.clientCache.get("S2"));

        // NOW STOP THE SERVER

        int staleCount = cli.clientListener.onDataStaleCount;

        srv.stop();

        Thread.sleep(1000); // give the client time to detect the missing server

        // check the registry to see if the entry is there - it should now be removed as the
        // server has 'died', it will get removed when the client asks the registry for the location
        // of the cache and the registry validates its location information (finding the server is dead)
        entry = registry.find(cacheName);
        if( entry != null) {
            Thread.sleep(500);
            entry = registry.find(cacheName);
        }
        log.info("registry-entry found<2> (expect null) [{}]", entry);
        Assert.assertNull("registry-entry NOT found  AFTER SERVER STOP", entry);

        cli.clientListener.awaitOnDataStaleCount(staleCount+1);
        Assert.assertTrue("client-cache is NOW stale AFTER SERVER STOP", cli.clientCache.isStale());

        log.info("*** RESTART SERVER ***");

        srv = ServerSideCacheStarter.start(cacheName, registries);
        srv.awaitStartup(); //  server has been started so this should return straight away

        // check the registry to see if the entry is there
        entry = registry.find(cacheName);
        if( entry == null ) {
            Thread.sleep(500);
            entry = registry.find(cacheName);
        }
        log.info("registry-entry found<3> [{}]", entry);
        Assert.assertNotNull("registry-entry found", entry);

        Thread.sleep(500); // give the client chance to re-connect

        AwaitParams.awaitCacheStaleValue(cli.clientCache, false);

        AwaitParams.awaitCacheSizeGte(cli.clientCache, 1);

        log.info("(2)client-cache AFTER RESTART stale?{} key-set {}", cli.clientCache.isStale(), cli.clientCache.keySet());

        // the Server has re-starteda and automatically gets 1 item in its cache as part of its re-start

        Assert.assertFalse("client-cache is NOT stale AFTER RE_CONNECT", cli.clientCache.isStale());
        Assert.assertEquals("client-cache has 2 items AFTER RE_CONNECT", 1, cli.clientCache.size());
        Assert.assertNotNull("client cache has S1 AFTER RE_CONNECT", cli.clientCache.get("S1"));


        // Tidy Up

        log.info("*** Tidy Up Registry ***");
        RegistryServer.stopInThread(registry);
    }


    @Test
    public void largeInitialLoadWithPureInitialUpdate() throws InterruptedException {
        log.info("##largeInitialLoadWithPureInitialUpdate");
        if( !  RUN_INTEGRATION_TESTS ) {
            log.info("integration tests not running");
            return;
        }
        doLargeInitialLoad(true);
    }

    @Test
    public void largeInitialLoadWithInterleavedUpdates() throws InterruptedException {
        log.info("##largeInitialLoadWithInterleavedUpdates");
        if( !  RUN_INTEGRATION_TESTS ) {
            log.info("integration tests not running");
            return;
        }
        doLargeInitialLoad(false);
    }

    private void doLargeInitialLoad(boolean pureInitialLoad) throws InterruptedException {

        log.info("##largeInitialLoad pureInitialLoad=[{}]", pureInitialLoad);

        String cacheName = "IntegrationCacheC";

        // start a registry - normally this will be a separate procvess on a different box, idealy multiple instances
        // each on a different box, but for the tests the registry will run in the same process

        RegistryServer registry = RegistryServer.startInThread();
        int registryPort = RegistryServer.awaitPortAllocation(registry);


        // create the server cache

        List<InetSocketAddress> registries = new ArrayList<>();
        registries.add(new InetSocketAddress("localhost", registryPort));

        PublishingCacheBuilder<String, SimpleData> server = new PublishingCacheBuilder<String, SimpleData>(registries);
        Cache<String, SimpleData> serverCache = server.build(cacheName);

        int twoAndHalfBatches = (500*2) + 250;
        for(int i=0; i<twoAndHalfBatches; i++) {
            serverCache.put("S"+i, new SimpleData("S_"+i, i));
        }

        if( pureInitialLoad ) {
            log.info("let event queue drain...");
            Thread.sleep(250); // give the event queue time to drain so we get an initial update and no
            // delayed 'update' events -- its not a problem if we do get them
            // and another option of the tests allows that to happen
        }

        // we now have a cache with enough items for 2.5 initial-update batches

        // now create a client

        SubscriptionCacheBuilder<String, SimpleData> client = new SubscriptionCacheBuilder<String, SimpleData>(registries);
        ImmutableCache<String, SimpleData> clientCache = client.subscribe(cacheName);

        SimpleCacheListener<String, SimpleData> clientListener = new SimpleCacheListener<String, SimpleData>();
        if( !pureInitialLoad ) {
            // just to reduce the logging
            clientListener.suppressOnUpdateLogging();
        }
        clientCache.addListener(clientListener);

        for(int i=0; i<100; i++) {
            if( clientCache.size() < twoAndHalfBatches ) {
                Thread.sleep(20);
            }
        }
        clientListener.awaitOnInitialUpdateCount(1);


        log.info("(1)client-cache stale?{} cache-size={}", clientCache.isStale(), clientCache.size());

        Assert.assertFalse("client-cache is not stale", clientCache.isStale());
        Assert.assertEquals("client-cache has twoAndHalfBatches of items", twoAndHalfBatches, clientCache.size());

        for(int i=0; i<twoAndHalfBatches; i++) {
            String key = "S"+i;
            SimpleData value = clientCache.get(key);
            Assert.assertNotNull("entry exists in client cache "+key, value);
        }

        // add a new item in the server and see that it is received in the client cache

        serverCache.put("SX", new SimpleData("SX", 2));

        clientListener.awaitOnUpdateCount(1);

        log.info("(2)client-cache stale?{} cache-size={}", clientCache.isStale(), clientCache.size());

        Assert.assertEquals("client-cache has twoAndHalfBatches+1", twoAndHalfBatches+1, clientCache.size());
        Assert.assertNotNull("client cache has SX", clientCache.get("SX"));


        // Tidy Up

        RegistryServer.stopInThread(registry);
        SubscriptionCacheBuilder.unSubscribe(clientCache);
    }

    @Test
    public void largeUpdateTestNoFiltered2dSub() throws InterruptedException {
        log.info("##largeUpdateTestNoFiltered2dSub");
        if( !  RUN_INTEGRATION_TESTS ) {
            log.info("integration tests not running");
            return;
        }
        doLargeUpdateTest(false);
    }

    @Test
    public void largeUpdateTestWithFiltered2dSub() throws InterruptedException {
        log.info("##largeUpdateTestWithFiltered2dSub");
        if( !  RUN_INTEGRATION_TESTS ) {
            log.info("integration tests not running");
            return;
        }
        doLargeUpdateTest(true);
    }

    private void doLargeUpdateTest(boolean filtered2ndSub) throws InterruptedException {

        log.info("##doLargeUpdateTest filtered2ndSub={}");

        String cacheName = "IntegrationCacheLU";

        // start a registry - normally this will be a separate procvess on a different box, idealy multiple instances
        // each on a different box, but for the tests the registry will run in the same process

        RegistryServer registry = RegistryServer.startInThread();
        int registryPort = RegistryServer.awaitPortAllocation(registry);

        // create the server cache

        List<InetSocketAddress> registries = new ArrayList<>();
        registries.add(new InetSocketAddress("localhost", registryPort));

        PublishingCacheBuilder<String, SimpleData> server = new PublishingCacheBuilder<String, SimpleData>(registries);
        Cache<String, SimpleData> serverCache = server.build(cacheName);

        // now create a client

        SubscriptionCacheBuilder<String, SimpleData> client1 = new SubscriptionCacheBuilder<String, SimpleData>(registries);
        ImmutableCache<String, SimpleData> clientCache1 = client1.subscribe(cacheName);

        SimpleCacheListener<String, SimpleData> clientListener1 = new SimpleCacheListener<String, SimpleData>();
        clientCache1.addListener(clientListener1);

        SimpleCacheListener<String, SimpleData> clientListener2 = null;
        ImmutableCache<String, SimpleData> clientCache2 = null;
        SubscriptionCacheBuilder<String, SimpleData> client2 = null;
        if( filtered2ndSub ) {
            // create a 2nd client to test the filter mechanism and the alternate code path
            // in the server when sending updates to a client that has a filter installed

            client2 = new SubscriptionCacheBuilder<String, SimpleData>(registries);
            clientCache2 = client1.subscribe(cacheName, new EvenSimpleDataFilter());
            //clientCache2 = client1.subscribe(cacheName);

            clientListener2 = new SimpleCacheListener<String, SimpleData>();
            clientCache2.addListener(clientListener2);

            clientListener1.suppressOnUpdateLogging();
            clientListener2.suppressOnUpdateLogging();
        }


        int startOnUpdateCount = clientListener1.onUpdateCount;
        int numOfUpdates = 500; //2000;  // 100
        for(int i=1; i<=numOfUpdates; i++) {
            String key =  "L_"+i;
            SimpleData value = new SimpleData(key, i);
            serverCache.put(key, value);
        }


        clientListener1.awaitOnUpdateCount(numOfUpdates+startOnUpdateCount);
        AwaitParams.awaitCacheSizeGte(clientCache1, numOfUpdates);
        if(  clientCache1.size() < numOfUpdates ) {
            log.warn("clientCache1 too small will wait again [{}]", clientCache1.size());
            Thread.sleep(1000);
            log.info("clientCache1 size check  [{}]", clientCache1.size());
        }
        AwaitParams.awaitCacheSizeGte(clientCache1, numOfUpdates);
        if(  clientCache1.size() < numOfUpdates ) {
            log.warn("clientCache1 still too small  [{}]", clientCache1.size());
            log.info("Assuming slow system will sleep longer clientCache1 [{}]", clientCache1.size());
            Thread.sleep(3000);
            log.info("clientCache1 size check  [{}]", clientCache1.size());
        }


        if( filtered2ndSub ) {
            clientListener1.awaitOnUpdateCount(numOfUpdates + (startOnUpdateCount / 2));
            AwaitParams.awaitCacheSizeGte(clientCache1, numOfUpdates / 2);
            if(  clientCache2.size() < (numOfUpdates/2) ) {
                log.warn("clientCache2 too small will wait again [{}]", clientCache2.size());
                Thread.sleep(1000);
                log.info("clientCache2 size check [{}]", clientCache2.size());
            }
            AwaitParams.awaitCacheSizeGte(clientCache1, numOfUpdates/2);

            if(  clientCache2.size() < (numOfUpdates/2) ) {
                log.warn("clientCache2 still too small [{}]", clientCache2.size());
                log.info("Assuming slow system will sleep longer waiting for clientCache2 [{}]", clientCache2.size());
                Thread.sleep(3000);
                log.info("clientCache2 size check[{}]", clientCache2.size());
            }
        }

        log.info("(1)client-cache-1 stale?{} cache-size={}", clientCache1.isStale(), clientCache1.size());
        Assert.assertFalse("client-cache-1 is not stale", clientCache1.isStale());
        Assert.assertEquals("client-cache-1 has correct of items", numOfUpdates, clientCache1.size());
        if( filtered2ndSub  ) {
            log.info("(1)client-cache-2 stale?{} cache-size={}", clientCache2.isStale(), clientCache2.size());
            Assert.assertFalse("client-cache-2 is not stale", clientCache1.isStale());
            Assert.assertEquals("client-cache-2 has correct of items", (numOfUpdates/2), clientCache2.size());

        }

        // add a new item in the server and see that it is received in the client cache

        int nextOnUpdateCount1 = clientListener1.onUpdateCount+1;
        int nextOnUpdateCount2 = clientListener2==null ? 0 : clientListener2.onUpdateCount+1;
        serverCache.put("S1", new SimpleData("S1", 10));

        clientListener1.awaitOnUpdateCount(nextOnUpdateCount1);
        if( clientCache1.size() < numOfUpdates+1 ) {
            log.warn("Assuming slow system (2) will sleep longer for clientListener1 [{}]", clientCache1.size());
            Thread.sleep(500);
            clientListener1.awaitOnUpdateCount(nextOnUpdateCount1);
            log.info("clientListener1, size check [{}]", clientCache1.size());
        }
        log.info("(2)client-cache-1 stale?{} cache-size={}", clientCache1.isStale(), clientCache1.size());
        Assert.assertEquals("client-cache-1 has all updates", numOfUpdates+1, clientCache1.size());
        Assert.assertNotNull("client cache-1 has S1", clientCache1.get("S1"));

        if( filtered2ndSub  ) {
            clientListener2.awaitOnUpdateCount(nextOnUpdateCount2);
            if( clientCache2.size() < ((numOfUpdates / 2)+1) ) {
                log.warn("Assuming slow system (2) will sleep longer for clientListener2 [{}]", clientCache2.size());
                Thread.sleep(500);
                log.info("clientListener2, size check [{}]", clientCache2.size());
                clientListener2.awaitOnUpdateCount(nextOnUpdateCount1);
            }
            log.info("(2)client-cache-2 stale?{} cache-size={}", clientCache2.isStale(), clientCache2.size());
            Assert.assertEquals("client-cache-2 has updates", (numOfUpdates / 2) + 1, clientCache2.size());
            Assert.assertNotNull("client cache-2 has S1", clientCache2.get("S1"));
        }

        // check caches have all the correct values

        for(int i=1; i<=numOfUpdates; i++) {
            String key =  "L_"+i;
            SimpleData expected = new SimpleData(key, i);
            SimpleData value = clientCache1.get(key);
            Assert.assertNotNull("value in cache-1 "+key, value);
            Assert.assertEquals("value correct in cache-1 "+key, expected, value);
            if( filtered2ndSub && i % 2 == 0 ) {
                Assert.assertNotNull("value in cache-2 "+key, value);
                Assert.assertEquals("value correct in cache-2 "+key, expected, value);
            }
        }

        // Tidy Up

        RegistryServer.stopInThread(registry);
        SubscriptionCacheBuilder.unSubscribe(clientCache1);
        if( filtered2ndSub ) {
            SubscriptionCacheBuilder.unSubscribe(clientCache2);
        }
    }



    @Test
    public void heartbeatNeededButNotReceivedTest() throws InterruptedException {
        log.info("##heartbeatNeededButNotReceivedTest");
        if( !  RUN_INTEGRATION_TESTS ) {
            log.info("integration tests not running");
            return;
        }
        doHeartbeaatTest(false);
    }

    @Test
    public void heartbeatNeededAndReceivedTest() throws InterruptedException {
        log.info("##heartbeatNeededAndReceivedTest");
        if( !  RUN_INTEGRATION_TESTS ) {
            log.info("integration tests not running");
            return;
        }
        doHeartbeaatTest(true);
    }

    private void doHeartbeaatTest(boolean heatbeatWillBeReceived) throws InterruptedException  {

        log.info("doHeartbeaatTest heatbeatWillBeReceived={}", heatbeatWillBeReceived);

        int heartbeatInterval = 1; /* 1 second */

        String cacheName = "IntegrationCacheB";

        // start a registry - normally this will be a separate procvess on a different box, idealy multiple instances
        // each on a different box, but for the tests the registry will run in the same process

        RegistryServer registry = RegistryServer.startInThread();
        int registryPort = RegistryServer.awaitPortAllocation(registry);


        // create the server cache

        List<InetSocketAddress> registries = new ArrayList<>();
        registries.add(new InetSocketAddress("localhost", registryPort));

        InstrumentedCachePublisherFactory<String, SimpleData> cachePublisherFactory = new   InstrumentedCachePublisherFactory<String, SimpleData>(); // so we can control the server sending heartbeats
        PublishingCacheBuilder<String, SimpleData> server = new PublishingCacheBuilder<String, SimpleData>(registries, null, null, null, null, cachePublisherFactory, null, null, null, null);
        Cache<String, SimpleData> serverCache = server.build(cacheName);

        serverCache.put("S1", new SimpleData("S1", 1));

        // we now have a cache with 1 item of data, it is registered in the registry so it can be found by a client

        Registry.Entry entry = registry.find(cacheName);
        log.info("registry-entry found [{}]", entry);

        // now create a client
        // in reality this would be in a different processes probably on a different machine
        //(if yiu want to use caches in the same process, just use CacheImpl.createLocalCache()

        InstrumentedCacheSubscriberFactory<String, SimpleData> cacheSubscriberFactor = new InstrumentedCacheSubscriberFactory<>();
        // use the instrumented factory so we can check heartbeats are received

        SubscriptionCacheBuilder<String, SimpleData> client = new SubscriptionCacheBuilder<String, SimpleData>(registries, null, cacheSubscriberFactor, null);
        ImmutableCache<String, SimpleData> clientCache = client.subscribe(cacheName, null/*no-filter*/, heartbeatInterval/*heartbeat every second*/);

        SimpleCacheListener<String, SimpleData> clientListener = new SimpleCacheListener<String, SimpleData>();
        clientCache.addListener(clientListener);

        clientListener.awaitOnInitialUpdateCount(1);

        Thread.sleep(heartbeatInterval*1000); // enough time for a heartbeat to be requested...

        if( !heatbeatWillBeReceived ) {
            // make sure the server won't sent any heartbeats,
            // i.e. the server will just not send heartbeats
            cachePublisherFactory.getInstrumentedServerSession().getInstrumentedSession().getInstrumentedBlockIO().setDropHeartbeats(true);

            // wait for the heartbeat to be missed and re-connection to occur

            Thread.sleep(heartbeatInterval*1000); // enough time for a 2nd heartbeat to be requested...
            // the 2nd heartbeat request will also be missed, which will trigger a network re-connect

            CacheSubscriberImpl<String, SimpleData> cacheSubscriberImpl = cacheSubscriberFactor.getCacheSubscriberImpl();
            for(int i=0; i<200; i++) {
                if( cacheSubscriberImpl.getCconnectionCount() >= 2 ) {
                    break;
                }
                Thread.sleep(50);
            }
            log.info("Heartbeat-Not-Received-Test: {} {} {} ", cacheSubscriberImpl.getMissedHeartbeatCount(), cacheSubscriberImpl.getConnectionErrorCount(), cacheSubscriberImpl.getCconnectionCount());

            Assert.assertEquals("missed heartbeat count is 1", 1, cacheSubscriberImpl.getMissedHeartbeatCount());
            Assert.assertEquals("connection error`t count is 1", 1, cacheSubscriberImpl.getConnectionErrorCount());
            Assert.assertEquals("connection count is 2", 2, cacheSubscriberImpl.getCconnectionCount());
        }

        Thread.sleep(heartbeatInterval*1000); // enough time for a heartbeat to be requested...
        // if this is the Heartbeat-Not-Received-Test then we have re-connected to the server and
        // heart beats will flow again...

        InstrumentedBlockIO iBio = cacheSubscriberFactor.getInstrumentedConnector().getInstrumentedSession().getInstrumentedBlockIO();

        iBio.awaitHeartbeatCount(1);

        log.info("heartbeat count = {}", iBio.getHeartbeatCount());

        // even if this is the heartbet-not-received test, after re-connection, heartbeats will be received!

        Assert.assertTrue("heartbeat count correct", iBio.getHeartbeatCount() >= 1);


        // Tidy Up


        SubscriptionCacheBuilder.unSubscribe(clientCache);
        RegistryServer.stopInThread(registry);
    }

    @Test
    public void streamModeTest() throws InterruptedException {

        // In stream-mode, the client-side subscribes as normal, but does not get a local copy of the
        // cache - it gets updates through the Cache-Listener, but the listener is listening exclusively
        // to the remote cache, there is no local copy of the cache (so the on-initial-load cache-parameter
        // will ALWAYS be empty in stream-mode)

        log.info("##streamModeTest");
        if( !  RUN_INTEGRATION_TESTS ) {
            log.info("integration tests not running");
            return;
        }

        String cacheName = "IntegrationCacheS";

        // start a registry - normally this will be a separate process on a different box, ideally multiple instances
        // each on a different box, but for the tests the registry will run in the same process


        RegistryServer registry = RegistryServer.startInThread();
        int registryPort = RegistryServer.awaitPortAllocation(registry);
        // the clients and servers can be freely moved around to any network location,
        // the only requirement is that the registry(s) be in a known location on a known port


        // create the server cache

        List<InetSocketAddress> registries = new ArrayList<>();
        registries.add(new InetSocketAddress("localhost", registryPort));

        PublishingCacheBuilder<String, SimpleData> server = new PublishingCacheBuilder<String, SimpleData>(registries);
        Cache<String, SimpleData> serverCache = server.build(cacheName);

        serverCache.put("S1", new SimpleData("S1", 1));

        // we now have a cache with 1 item of data, it is registered in the registry so it can be found by a client

        { // check the registry to see if the entry is there
            Registry.Entry entry = registry.find(cacheName);
            log.info("registry-entry found [{}]", entry);
            Assert.assertNotNull("registry-entry found", entry);
        }

        // now create a client
        // in reality this would be in a different processes probably on a different machine
        //(if you want to use caches in the same process, just use CacheImpl.createLocalCache()

        SimpleCacheListener<String, SimpleData> clientListener = new SimpleCacheListener<String, SimpleData>();
        SubscriptionStreamBuilder<String, SimpleData> client = new SubscriptionStreamBuilder<>(registries);
        int streamRef = client.subscribe(cacheName, clientListener);
        Thread.sleep(100); // let events move onto the queue
        clientListener.awaitOnInitialUpdateCount(1);

        Assert.assertEquals("on initial update, the cache is empty", 0, clientListener.initialUpdate.size());

        clientListener.awaitOnUpdateCount(1);

        Assert.assertTrue("received update for S1", clientListener.updateAccumulation.containsKey("S1"));

        // add a new item in the server and see that it is received in the client cache

        int nextUpdateCount =  clientListener.onUpdateCount+1;
        serverCache.put("S2", new SimpleData("S2", 2));

        clientListener.awaitOnUpdateCount(nextUpdateCount);
        Assert.assertTrue("received update for S2", clientListener.updateAccumulation.containsKey("S2"));

        // add a 3rd item in the server and see that it is received in the client cache

        nextUpdateCount =  clientListener.onUpdateCount+1;
        serverCache.put("S3", new SimpleData("S3", 3));

        clientListener.awaitOnUpdateCount(nextUpdateCount);
        Assert.assertTrue("received update for S3", clientListener.updateAccumulation.containsKey("S3"));

        // check s2 value correct

        SimpleData d2 = clientListener.updateAccumulation.get("S2");
        Assert.assertEquals("S2 val;ue correct", 2, d2.data2);

        // update value for S2

        nextUpdateCount =  clientListener.onUpdateCount+1;
        serverCache.put("S2", new SimpleData("S2", 2000));
        clientListener.awaitOnUpdateCount(nextUpdateCount);
        d2 = clientListener.updateAccumulation.get("S2");
        Assert.assertEquals("S2 value correct after update", 2000, d2.data2);

        // remove an item

        int nextRemoveCount =  clientListener.onRemoveCount+1;
        serverCache.remove("S2");
        clientListener.awaitOnRemoveCount(nextRemoveCount);
        Assert.assertTrue("received removal-notification for S2", clientListener.removedAccumulation.containsKey("S2"));

        // server cache notifies stale

        int nextStaleCount =  clientListener.onDataStaleCount+1;
        serverCache.notifyStale();
        clientListener.awaitOnDataStaleCount(nextStaleCount);
        Assert.assertTrue("stale staus = true", clientListener.isStale);

        // server cache notifies un-stale

        int nextActiveCount =  clientListener.onActiveCount+1;
        serverCache.notifyUnStale();
        clientListener.awaitOnActiveCount(nextActiveCount);
        Assert.assertFalse("stale staus = false", clientListener.isStale);

        // add a 4th item in the server and see that it is added to the  client cache

        nextUpdateCount =  clientListener.onUpdateCount+1;
        serverCache.put("S4", new SimpleData("S4", 4));

        clientListener.awaitOnUpdateCount(nextUpdateCount);
        Assert.assertTrue("received update for S4", clientListener.updateAccumulation.containsKey("S4"));

        // unsubscibe and verify we receive no more messages

        SubscriptionStreamBuilder.unSubscribe(streamRef);
        clientListener.awaitOnDataStaleCount(1);

        Assert.assertTrue("stale staus = true", clientListener.isStale);

        serverCache.put("S5", new SimpleData("S5", 5));

        clientListener.awaitOnUpdateCount(5);
        Assert.assertFalse("not received update for S5", clientListener.updateAccumulation.containsKey("S5"));

        // Tidy Up

        RegistryServer.stopInThread(registry);
    }

    @Test
    public void multipleClientTest() throws InterruptedException {

        // start a Registry, and Server and a Client and then put entries into the server-cache
        // and watch them arrive in the client-cache and monitor the client-side listener events

        log.info("##multipleClientTest");
        if( !  RUN_INTEGRATION_TESTS ) {
            log.info("integration tests not running");
            return;
        }

        int registryPort = getFreePort();
        List<InetSocketAddress> registries = new ArrayList<>();
        registries.add(new InetSocketAddress("localhost", registryPort));

        String cacheName = "IntegrationCacheA";

        RegistryServer registry = null;
        log.info("*** START THE REGISTRY registryPort={} ***", registryPort);
        registry = RegistryServer.startInThread(registryPort);
        // the clients and servers can be freely moved around to any network location,
        // the only requirement is that the registry(s) be in a known location on a known port


        // create the server cache

        ServerSideCacheStarter srv = ServerSideCacheStarter.start(cacheName, registries);;

        srv.awaitStartup(); // registry and server has been started so this should return straight away
        // check the registry to see if the entry is there
        Registry.Entry entry = registry.find(cacheName);
        log.info("registry-entry found [{}]", entry);
        Assert.assertNotNull("registry-entry found", entry);


        ClientSideCacheStarter cli1 = ClientSideCacheStarter.start(cacheName, registries);
        ClientSideCacheStarter cli2 = ClientSideCacheStarter.start(cacheName, registries);
        ClientSideCacheStarter cli3 = ClientSideCacheStarter.start(cacheName, registries);
        ArrayList<ClientSideCacheStarter> cliList = new  ArrayList<ClientSideCacheStarter>();
        cliList.add(cli1);
        cliList.add(cli2);
        cliList.add(cli3);


        // now wait for SERVER and CLIENT to start

        for(int i=0; i<cliList.size(); i++) {
            cliList.get(i).awaitStartup();
        }

        Assert.assertNotNull("server started", srv.serverCache);
        for(int i=0; i<cliList.size(); i++) {
            cliList.get(i).awaitStartup();
            Assert.assertNotNull("client ["+i+"] started", cliList.get(i).clientCache);
            AwaitParams.awaitCacheSizeGte(cliList.get(i).clientCache, 1);
            log.info("(1)client-["+i+"]-cache stale?{} key-set {}", cliList.get(i).clientCache.isStale(), cliList.get(i).clientCache.keySet());
            Assert.assertFalse("client-cache-["+i+"] is not stale", cliList.get(i).clientCache.isStale());
            Assert.assertEquals("client-cache-["+i+"] has 1 item", 1, cliList.get(i).clientCache.size());
            Assert.assertNotNull("client cache-["+i+"] has S1", cliList.get(i).clientCache.get("S1"));
        }


        // add a new item in the server and see that it is received in the client cache

        int nextUpdateCount = cli1.clientListener.onUpdateCount+1;
        srv.serverCache.put("S2", new SimpleData("S2", 2));
        srv.serverCache.put("S3", new SimpleData("S3", 3));


        for(int i=0; i<cliList.size(); i++) {
            cliList.get(i).clientListener.awaitOnUpdateCount(nextUpdateCount);
            AwaitParams.awaitCacheSizeGte(cliList.get(i).clientCache, 3);
            log.info("(2)client-cache-["+i+"] stale?{} key-set {}", cliList.get(i).clientCache.isStale(), cliList.get(i).clientCache.keySet());
            Assert.assertEquals("client-cache-["+i+"] has 3 items", 3, cliList.get(i).clientCache.size());
            Assert.assertNotNull("client cache-["+i+"] has S1", cliList.get(i).clientCache.get("S1"));
            Assert.assertNotNull("client cache-["+i+"] has S2", cliList.get(i).clientCache.get("S2"));
            Assert.assertNotNull("client cache-["+i+"] has S3", cliList.get(i).clientCache.get("S3"));
        }


        // change a value
        nextUpdateCount = cli1.clientListener.onUpdateCount+1;
        srv.serverCache.put("S2", new SimpleData("S2", 2000));
        for(int i=0; i<cliList.size(); i++) {
            cliList.get(i).clientListener.awaitOnUpdateCount(nextUpdateCount);
            log.info("(2)client-cache-["+i+"] stale?{} key-set {}", cliList.get(i).clientCache.isStale(), cliList.get(i).clientCache.keySet());
            SimpleData d1 = cliList.get(i).clientCache.get("S1");
            SimpleData d2 = cliList.get(i).clientCache.get("S2");
            SimpleData d3 = cliList.get(i).clientCache.get("S3");
            Assert.assertEquals("client cache-["+i+"] has S1-VALUE", 1, d1.data2);
            Assert.assertEquals("client cache-["+i+"] has S2-VALUE", 2000, d2.data2);
            Assert.assertEquals("client cache-["+i+"] has S3-VALUE", 3, d3.data2);
        }

        // remove an item

        int nextRemovalCount =  cli1.clientListener.onRemoveCount+1;
        srv.serverCache.remove("S2");

        for(int i=0; i<cliList.size(); i++) {
            cliList.get(i).clientListener.awaitOnRemoveCount(nextRemovalCount);
            AwaitParams.awaitCacheSizeGte(cliList.get(i).clientCache, 2);
            log.info("(3:removal)client-cache-["+i+"] stale?{} key-set {}", cliList.get(i).clientCache.isStale(), cliList.get(i).clientCache.keySet());
            Assert.assertEquals("client-cache-["+i+"] has 2 items", 2, cliList.get(i).clientCache.size());
            Assert.assertNotNull("client cache-["+i+"] has S1", cliList.get(i).clientCache.get("S1"));
            Assert.assertNull("client cache-["+i+"] does not have S2", cliList.get(i).clientCache.get("S2"));
            Assert.assertNotNull("client cache-["+i+"] has S3", cliList.get(i).clientCache.get("S3"));
        }


        // server cache notifies stale

        int nextStaleCount =  cli1.clientListener.onDataStaleCount+1;
        srv.serverCache.notifyStale();

        for(int i=0; i<cliList.size(); i++) {
            cliList.get(i).clientListener.awaitOnDataStaleCount(nextStaleCount);
            AwaitParams.awaitCacheStaleValue(cliList.get(i).clientCache, true);
            log.info("(4:stale)client-cache-["+i+"] stale?{} key-set {}", cliList.get(i).clientCache.isStale(), cliList.get(i).clientCache.keySet());
            Assert.assertTrue("client-cache-["+i+"] is-stale", cliList.get(i).clientCache.isStale());
            Assert.assertEquals("client-cache-["+i+"] has 2 items", 2, cliList.get(i).clientCache.size());
            Assert.assertNotNull("client cache-["+i+"] has S1", cliList.get(i).clientCache.get("S1"));
            Assert.assertNull("client cache-["+i+"] does not have S2", cliList.get(i).clientCache.get("S2"));
            Assert.assertNotNull("client cache-["+i+"] has S3", cliList.get(i).clientCache.get("S3"));
        }

        // server cache notifies un-stale

        int nextActiveCount =  cli1.clientListener.onActiveCount+1;
        srv.serverCache.notifyUnStale();

        for(int i=0; i<cliList.size(); i++) {
            cliList.get(i).clientListener.awaitOnActiveCount(nextActiveCount);
            AwaitParams.awaitCacheStaleValue(cliList.get(i).clientCache, false);
            log.info("(5:un-stale)client-cache-["+i+"] stale?{} key-set {}", cliList.get(i).clientCache.isStale(), cliList.get(i).clientCache.keySet());
            Assert.assertFalse("client-cache-["+i+"] is-un-stale", cliList.get(i).clientCache.isStale());
            Assert.assertEquals("client-cache-["+i+"] has 2 items", 2, cliList.get(i).clientCache.size());
            Assert.assertNotNull("client cache-["+i+"] has S1", cliList.get(i).clientCache.get("S1"));
            Assert.assertNull("client cache-["+i+"] does not have S2", cliList.get(i).clientCache.get("S2"));
            Assert.assertNotNull("client cache-["+i+"] has S3", cliList.get(i).clientCache.get("S3"));
        }

        // add a 4th item in the server and see that it is added to the  client cache

        nextUpdateCount = cli1.clientListener.onUpdateCount+1;
        srv.serverCache.put("S4", new SimpleData("S4", 4));

        for(int i=0; i<cliList.size(); i++) {
            cliList.get(i).clientListener.awaitOnUpdateCount(nextUpdateCount);
            AwaitParams.awaitCacheSizeGte(cliList.get(i).clientCache, 3);
            log.info("(6:last-update)client-cache-["+i+"] stale?{} key-set {}", cliList.get(i).clientCache.isStale(), cliList.get(i).clientCache.keySet());
            Assert.assertEquals("client-cache-["+i+"] has 3 items", 3, cliList.get(i).clientCache.size());
            Assert.assertNotNull("client cache-["+i+"] has S1", cliList.get(i).clientCache.get("S1"));
            Assert.assertNull("client cache-["+i+"] does not have S2", cliList.get(i).clientCache.get("S2"));
            Assert.assertNotNull("client cache-["+i+"] has S3", cliList.get(i).clientCache.get("S3"));
            Assert.assertNotNull("client cache-["+i+"] has S4", cliList.get(i).clientCache.get("S4"));
        }


        // unsubscribe 1 client and verify we receive no more messages for that client
        // but other clients do get updates

        SubscriptionCacheBuilder.unSubscribe(cli2.clientCache);

        nextUpdateCount = cli1.clientListener.onUpdateCount+1;
        srv.serverCache.put("S5", new SimpleData("S5", 5));

        cli1.clientListener.awaitOnUpdateCount(nextUpdateCount);
        cli2.clientListener.awaitOnUpdateCount(nextUpdateCount); // should time-out
        cli3.clientListener.awaitOnUpdateCount(nextUpdateCount);

        AwaitParams.awaitCacheSizeGte(cli1.clientCache, 4);
        AwaitParams.awaitCacheSizeGte(cli3.clientCache, 4);

        log.info("(7:2/3 updated)client-cache-1  stale?{} key-set {}", cli1.clientCache.isStale(), cli1.clientCache.keySet());
        log.info("(7:2/3 updated)client-cache-2  stale?{} key-set {}", cli2.clientCache.isStale(), cli2.clientCache.keySet());
        log.info("(7:2/3 updated)client-cache-3  stale?{} key-set {}", cli3.clientCache.isStale(), cli3.clientCache.keySet());

        Assert.assertEquals("client-cache-[1] has 3 items", 4, cli1.clientCache.size());
        Assert.assertEquals("client-cache-[2] has 3 items", 3, cli2.clientCache.size());
        Assert.assertEquals("client-cache-[3] has 3 items", 4, cli3.clientCache.size());

        // Tidy Up

        RegistryServer.stopInThread(registry);
    }

    @Test
    public void routerTest() throws Exception{

        // NB: the router is an optional component that can be used to handle the (re)publication of
        // multiple caches to multiple subscribers

        log.info("## routerTest");
        if( !  RUN_INTEGRATION_TESTS ) {
            log.info("integration tests not running");
            return;
        }

        int registryPort = getFreePort();
        List<InetSocketAddress> registries = new ArrayList<>();
        registries.add(new InetSocketAddress("localhost", registryPort));

        String cacheName = "RouterTestCache";

        RegistryServer registry = null;
        log.info("*** START THE REGISTRY registryPort={} ***", registryPort);
        registry = RegistryServer.startInThread(registryPort);
        // the clients and servers can be freely moved around to any network location,
        // the only requirement is that the registry(s) be in a known location on a known port

        // create the server cache

        ServerSideCacheStarter srv = ServerSideCacheStarter.start(cacheName, registries);

        srv.awaitStartup(); // registry and server has been started so this should return straight away
        // check the registry to see if the entry is there
        Registry.Entry entry = registry.find(cacheName);
        log.info("registry-entry found [{}]", entry);
        Assert.assertNotNull("registry-entry found", entry);

        log.info("*** START THE ROUTER registryPort={} ***", registryPort);

        Properties properties = new Properties();
        String registriesStr = "localhost:"+registryPort;
        String inRegistriesProp = "republish."+cacheName+".in-registries";
        properties.setProperty(inRegistriesProp, registriesStr);
        String outRegistriesProp = "republish."+cacheName+".out-registries";
        properties.setProperty(outRegistriesProp, registriesStr);

        log.info("set properties {}", properties);
        Republisher republisher = new   Republisher(cacheName, properties);
        republisher.start();

        String routerCacheName =  "R."+cacheName;
        log.info("*** START CLIENT-1 registryPort={} routerCacheName=[{}] ***", registryPort, routerCacheName);
        ClientSideCacheStarter cli1 = ClientSideCacheStarter.start(routerCacheName, registries);
        cli1.awaitStartup();

        log.info("*** START CLIENT-2 registryPort={} routerCacheName=[{}] ***", registryPort, routerCacheName);
        ClientSideCacheStarter cli2 = ClientSideCacheStarter.start(routerCacheName, registries);
        cli2.awaitStartup();

        srv.serverCache.put("S2", new SimpleData("S2", 2));

        AwaitParams.awaitCacheSizeGte(cli1.clientCache, 2);
        AwaitParams.awaitCacheSizeGte(cli2.clientCache, 2);
        // the startup for the server automatically adds an item 'S1' and we have just added 'S2'

        log.info("client-1 cache ["+cli1.clientCache.getName()+"] keys {} ", cli1.clientCache.keySet());
        Assert.assertEquals("client-1  cache has item", cli1.clientCache.size(), 2);

        log.info("client-2 cache ["+cli2.clientCache.getName()+"] keys {} ", cli2.clientCache.keySet());
        Assert.assertEquals("client-2  cache has item", cli2.clientCache.size(), 2);

        Assert.assertEquals("client-1 subscribed to correct cache",  cli1.clientCache.getName(), routerCacheName);
        Assert.assertEquals("client-1 subscribed to correct cache",  cli2.clientCache.getName(), routerCacheName);

        SimpleData s1 = cli1.clientCache.get("S1");
        Assert.assertNotNull("S1 Item in client-1 Cache", s1);
        SimpleData s2 = cli1.clientCache.get("S2");
        Assert.assertNotNull("S2 Item in client-1 Cache", s2);

        s1 = cli2.clientCache.get("S1");
        Assert.assertNotNull("S1 Item in client-2 Cache", s1);
        s2 = cli2.clientCache.get("S2");
        Assert.assertNotNull("S2 Item in client-2 Cache", s2);


        log.info("*** CLIENT-1 UNSUBSCRIBES FROM ROUTER");

        SubscriptionCacheBuilder.unSubscribe(cli1.clientCache);

        srv.serverCache.put("S3", new SimpleData("S3", 2));

        AwaitParams.awaitCacheSizeGte(cli2.clientCache, 3);
        log.info("client-2 cache ["+cli2.clientCache.getName()+"] keys {} ", cli2.clientCache.keySet());
        Assert.assertEquals("client-2  cache has item", cli2.clientCache.size(), 3);

        SimpleData s3 = cli2.clientCache.get("S3");
        Assert.assertNotNull("S3 Item in client-2 Cache", s3);

        s3 = cli1.clientCache.get("S3");
        Assert.assertNull("S3 Item NOT in client-1 Cache", s3);



        // Tidy Up

        RegistryServer.stopInThread(registry);
    }

}