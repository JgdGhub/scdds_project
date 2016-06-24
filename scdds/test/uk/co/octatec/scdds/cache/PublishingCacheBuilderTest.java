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
import uk.co.octatec.scdds.GlobalProperties;
import uk.co.octatec.scdds.utilities.SimpleCacheListener;
import uk.co.octatec.scdds.utilities.SimpleData;
import uk.co.octatec.scdds.net.registry.RegistryServer;

/**
 * Created by Jeromy Drake on 06/05/16
 */
public class PublishingCacheBuilderTest {

    // test the server-side 'publishing' cache-builder

    private final static Logger log = LoggerFactory.getLogger(PublishingCacheBuilderTest.class);

    @BeforeClass
    public static void setup() {
        GlobalProperties.exposeHttpServer = false;
    }

    @Test
    public void builderTestNoRegistry() {
        log.info("## builderTestNoRegistry");
        String registries = "server1:9999,server2:9998";
        PublishingCacheBuilder<String, SimpleData> builder = new PublishingCacheBuilder<String, SimpleData>(PublishingCacheBuilder.addrListFromString(registries));
        Cache<String, SimpleData> cache = null;
        try {
            cache = builder.build("MY_CACHE_1", 1);
        }
        catch(RuntimeException x) {
            log.info("failed to register cache as expectd [{}]", x.getMessage());
        }
        Assert.assertTrue("cache was not created", cache == null);
    }

    @Test
    public void builderTestWithRegistry() throws Exception {
        log.info("## builderTestWithRegistry");

        RegistryServer registryServer1 = RegistryServer.startInThread();
        int portReadyCount = 0;
        int port = RegistryServer.awaitPortAllocation(registryServer1);
        String registries = "localhost:"+port;

        PublishingCacheBuilder<String, SimpleData> builder = new PublishingCacheBuilder<String, SimpleData>(PublishingCacheBuilder.addrListFromString(registries));
        Cache<String, SimpleData> cache = null;
        try {
            cache = builder.build("MY_CACHE_2", 1);
        }
        catch(RuntimeException x) {
            log.error("failed to register cache as expectd [{}]", x.getMessage());
        }
        Assert.assertTrue("cache was created", cache != null);

        cache.put("A", new SimpleData("A", 1));
        Thread.sleep(30); // make sure the event is processes before the listener is added
        SimpleCacheListener<String, SimpleData> cacheListener = new SimpleCacheListener<String, SimpleData>();
        log.info("before adding: cacheListener.onInitialUpdateCount = [{}] cacheListener.onUpdateCount = [{}]", cacheListener.onInitialUpdateCount, cacheListener.onUpdateCount);

        cache.addListener(cacheListener);
        cacheListener.awaitOnInitialUpdateCount(1);

        log.info("after adding: cacheListener.onInitialUpdateCount = [{}] cacheListener.onUpdateCount = [{}]", cacheListener.onInitialUpdateCount, cacheListener.onUpdateCount);
        Assert.assertTrue("cache has data", cache.size() == 1);
        Assert.assertTrue("listener is called: onInitialUpdateCount", cacheListener.onInitialUpdateCount == 1);
        Assert.assertTrue("listener is not-called: onUpdateCount", cacheListener.onUpdateCount == 0);


        cache.put("B", new SimpleData("B", 2));

        cacheListener.awaitOnUpdateCount(1);

        log.info("cacheListener.onUpdateCount = [{}]", cacheListener.onUpdateCount);
        Assert.assertTrue("cache has data", cache.size() == 2);
        Assert.assertTrue("listener is called: onInitialUpdateCount", cacheListener.onInitialUpdateCount == 1);
        Assert.assertTrue("listener is called: onUpdateCount", cacheListener.onUpdateCount == 1);

        RegistryServer.stopInThread(registryServer1);

    }
}
