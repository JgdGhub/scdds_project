package uk.co.octatec.scdds.cache.subscribe;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.cache.SubscriptionCacheBuilder;
import uk.co.octatec.scdds.cache.publish.CacheFilter;
import uk.co.octatec.scdds.cache.publish.PropertyUtils;
import uk.co.octatec.scdds.utilities.SimpleCacheListener;
import uk.co.octatec.scdds.utilities.SimpleData;
import uk.co.octatec.scdds.mock.MockCacheLocator;
import uk.co.octatec.scdds.mock.MockCachePublisher;
import uk.co.octatec.scdds.mock.MockGeneralRequestHandler;
import uk.co.octatec.scdds.net.registry.CacheLocator;

import java.util.Properties;

/**
 * Created by Jeromy Drake on 06/05/16
 */
public class SubscriptionCacheBuilderTest {

    // test the client-side 'subscription' cache-builder

    // A 'mock' server-side CachePublisher is used that will receive a real tcp/ip
    // socket connection from the client-side Subscriber

    private final static Logger log = LoggerFactory.getLogger(SubscriptionCacheBuilderTest.class);

    class MyCacheSubscriberFactory<K, T> implements CacheSubscriberFactory<K, T> {
        CacheSubscriberImpl<K, T> subscriber;
        @Override
        public CacheSubscriber<K, T> create(CacheImplClientSide<K, T> cache, CacheLocator locator, CacheFilter<K, T> filter, String filterArg, InitialUpdateReaderFactory<K, T> initialUpdateReaderFactory) {
            subscriber = new CacheSubscriberImpl<K, T>(cache, locator, filter, filterArg, 1, initialUpdateReaderFactory);
            return subscriber;
        }
    }

    @Test
    public void noServerToConectToTest() throws InterruptedException {

        MyCacheSubscriberFactory<String, SimpleData> cacheSubscriberFactory = new MyCacheSubscriberFactory<>();
        // this is to limit retries to 1

        SubscriptionCacheBuilder<String, SimpleData> builder = new SubscriptionCacheBuilder<>(
                new MockCacheLocator(), null, cacheSubscriberFactory, null);

        ImmutableCache<String, SimpleData> cache = builder.subscribe("TestCache");
        SimpleCacheListener<String, SimpleData> listener = new SimpleCacheListener<>();
        cache.addListener(listener);

        listener.awaitOnFatalCount(1);

        log.info("counters: onFatalCount={} onDataStaleCount={} onActiveCount={} onInitialUpdateCount={}",
                listener.onFatalCount, listener.onDataStaleCount, listener.onActiveCount, listener.onInitialUpdateCount);

        Assert.assertTrue("cache should be stale", cache.isStale());

        SubscriptionCacheBuilder.unSubscribe(cache);

        ((CacheImplClientSide) cache).dispose();
    }

    @Test
    public void goodConnectionTest() throws Exception {

        MockGeneralRequestHandler generalRequestHandler = new MockGeneralRequestHandler();

        MockCachePublisher<String, SimpleData> cahePublisher = new MockCachePublisher<String, SimpleData>(generalRequestHandler);
        // simplified version of the CachePublisher for tests

        int port = cahePublisher.initializePort();
        log.info("server-port=[{}]", port);
        cahePublisher.start();
        boolean started = cahePublisher.waitForStart();
        junit.framework.Assert.assertTrue("server has started", started);

        MyCacheSubscriberFactory<String, SimpleData> cacheSubscriberFactory = new MyCacheSubscriberFactory<>();
        // this is to limit retries to 1

        MockCacheLocator mockCacheLocator = new MockCacheLocator("localhost",port);

        SubscriptionCacheBuilder<String, SimpleData> builder = new SubscriptionCacheBuilder<>(
                mockCacheLocator, null, cacheSubscriberFactory, null);

        ImmutableCache<String, SimpleData> cache = builder.subscribe("TestCache");
        SimpleCacheListener<String, SimpleData> listener = new SimpleCacheListener<>();
        cache.addListener(listener);

        generalRequestHandler.awaitRegisteredSessionCount(1);

        log.info("counters: registerSessionCount={} onFatalCount={} onDataStaleCount={} onActiveCount={} onInitialUpdateCount={}",
                    generalRequestHandler.registerSessionCount, listener.onFatalCount, listener.onDataStaleCount,
                    listener.onActiveCount, listener.onInitialUpdateCount);

        Assert.assertEquals("cache got registered", 1, generalRequestHandler.registerSessionCount);

        Properties intPropertiesSent = PropertyUtils.getPropertiesFromString(cahePublisher.initString) ;

        log.info("intPropertiesSent {}", intPropertiesSent);


        Assert.assertEquals("subscription has been registered", 1, SubscriptionCacheBuilder.getSubscriptionsSize());  //

        // now do an unsubscribe

        log.info("unsubscribe cache [{}]", cache.getName());

        SubscriptionCacheBuilder.unSubscribe(cache);

        Assert.assertEquals("subscription has been removed", 0, SubscriptionCacheBuilder.getSubscriptionsSize());

        log.info("unsubscribed check: isUnsubscribed={} isSubscriptionThreadFinished={} ",
                cacheSubscriberFactory.subscriber.isUnsubscribed(), cacheSubscriberFactory.subscriber.isSubscriptionThreadFinished());

        Assert.assertTrue("subscriber is un-subscribed",  cacheSubscriberFactory.subscriber.isUnsubscribed());


        ((CacheImplClientSide) cache).dispose();
    }
}
