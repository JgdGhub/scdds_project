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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.ConditionalCompilation;
import uk.co.octatec.scdds.cache.publish.CacheFilter;
import uk.co.octatec.scdds.cache.publish.MapFactory;
import uk.co.octatec.scdds.cache.subscribe.*;
import uk.co.octatec.scdds.net.registry.CacheLocator;
import uk.co.octatec.scdds.net.registry.CacheLocatorImpl;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Jeromy Drake on 20/05/2016.
 */
public class SubscriptionStreamBuilder<K,T extends ImmutableEntry> {

    // this differs from SubscriptionCacheBuilder in that it does not maintain a local cache of data - a subscription is still
    // made to a remote cache and events are fired on a local CacheListener, but there is no actual local cache

    private final static Logger log = LoggerFactory.getLogger(SubscriptionStreamBuilder.class);

    private static final int DEFAULT_HEARTBEAT_SECONDS = 15;

    final private ListenerEventQueueFactory<K, ListenerEvent<K, T>> listenerEventQueueFactory;
    final private CacheSubscriberFactory<K, T> cacheSubscriberFactory;
    final private CacheLocator locator;
    final private ListenerEventFactory<K,T> listenerEventFactory;


    final static private ConcurrentHashMap<Integer,CacheSubscriber> subscriptions = new ConcurrentHashMap<>();

    public SubscriptionStreamBuilder(List<InetSocketAddress> registries,
                                    ListenerEventQueueFactory<K, ListenerEvent<K, T>> listenerEventQueueFactory,
                                    CacheSubscriberFactory<K, T> cacheSubscriberFactory) {

        locator = new CacheLocatorImpl(registries);
        this.listenerEventQueueFactory = defaultListenerEventQueueFactory(listenerEventQueueFactory);
        this.cacheSubscriberFactory = cacheSubscriberFactory==null ? new CacheSubscriberFactoryDefaultImpl<K,T>() : cacheSubscriberFactory;
        this.listenerEventFactory = new ListenerEventFactoryDefaultImpl<K,T>();
    }

    public SubscriptionStreamBuilder(List<InetSocketAddress> registries,
                                    ListenerEventQueueFactory<K, ListenerEvent<K, T>> listenerEventQueueFactory,
                                    CacheSubscriberFactory<K, T> cacheSubscriberFactory,
                                    ListenerEventFactory<K,T> listenerEventFactory) {

        locator = new CacheLocatorImpl(registries);
        this.listenerEventQueueFactory = defaultListenerEventQueueFactory(listenerEventQueueFactory);
        this.cacheSubscriberFactory = cacheSubscriberFactory==null ? new CacheSubscriberFactoryDefaultImpl<K,T>() : cacheSubscriberFactory;
        this.listenerEventFactory = listenerEventFactory==null ? new ListenerEventFactoryDefaultImpl<K,T>() : listenerEventFactory;
    }

    public SubscriptionStreamBuilder(CacheLocator locator,
                                    ListenerEventQueueFactory<K, ListenerEvent<K, T>> listenerEventQueueFactory,
                                    CacheSubscriberFactory<K, T> cacheSubscriberFactory) {

        this.locator = locator;
        this.listenerEventQueueFactory = defaultListenerEventQueueFactory(listenerEventQueueFactory);
        this.cacheSubscriberFactory = cacheSubscriberFactory==null ? new CacheSubscriberFactoryDefaultImpl<K,T>() : cacheSubscriberFactory;
        this.listenerEventFactory = new ListenerEventFactoryDefaultImpl<K,T>();
    }

    public SubscriptionStreamBuilder(List<InetSocketAddress> registries) {
        locator = new CacheLocatorImpl(registries);
        this.listenerEventQueueFactory = defaultListenerEventQueueFactory(null);
        this.cacheSubscriberFactory = new CacheSubscriberFactoryDefaultImpl<K,T>();
        this.listenerEventFactory = new ListenerEventFactoryDefaultImpl<K,T>();
    }

    public int subscribe(String cacheName, CacheListener<K,T> listener) {
        return subscribe(cacheName, listener, null, DEFAULT_HEARTBEAT_SECONDS);
    }

    public int subscribe(String cacheName, CacheListener<K,T> listener, CacheFilter<K, T> filter) {
        return subscribe(cacheName, listener, filter, null, DEFAULT_HEARTBEAT_SECONDS);
    }
    public int subscribe(String cacheName, CacheListener<K,T> listener, CacheFilter<K, T> filter, String filterArg) {
        return subscribe(cacheName, listener, filter, filterArg, DEFAULT_HEARTBEAT_SECONDS);
    }

    public int subscribe(String cacheName, CacheListener<K,T> listener, CacheFilter<K, T> filter, int heartbeatSeconds) {
        return subscribe(cacheName, listener, filter, null, heartbeatSeconds);
    }

    public int subscribe(String cacheName, CacheListener<K,T> listener, CacheFilter<K, T> filter, String filterArg, int heartbeatSeconds) {

        log.info("subscribing (stream-based) to cache [{}] filter=[{}] filterArg=[{}] heartbeatSeconds=[{}]", cacheName, filter, heartbeatSeconds);

        ClientSideStream<K,T> stream = new  ClientSideStream<K,T>(cacheName, listenerEventQueueFactory, listenerEventFactory);

        stream.start();
        stream.addListener(listener); // the listener will get an onActive() and then an empty initialUpdate
                                      // all data is delivered through the onUpdate() callback for 'streams'

        CacheSubscriber<K, T> streamSubscriber = cacheSubscriberFactory.create(stream, locator, filter, filterArg, new InitialUpdateReaderFactoryForStreamsImpl<K, T>());
        streamSubscriber.subscribe(heartbeatSeconds);
        Integer key = System.identityHashCode(stream);
        log.info("add stream-based subscription [{}] [{}]", key, streamSubscriber);
        subscriptions.put(key, streamSubscriber);

        return System.identityHashCode(stream);
    }

    public static  <K, T> boolean unSubscribe(int streafRef) {
        // this will probably not be a used very much, it is included mostly for completeness rather than in the
        // expectation that caches will be subscribed and then unsubscribe, the expection is that a cache will remain subscribed
        // until the application exits, an explicit un-subscribeis not required at application exit
        CacheSubscriber<K, T> cacheSubscriber =  subscriptions.remove(streafRef);
        if( cacheSubscriber == null ) {
            log.info("unknown streafRef [{}], can't unsubscribe",streafRef);
            return false;
        }
        cacheSubscriber.unSubscribe();
        return true;
    }

    static  <K,T> ListenerEventQueueFactory<K, ListenerEvent<K,T>>  defaultListenerEventQueueFactory(ListenerEventQueueFactory<K, ListenerEvent<K,T>>  theFactory) {
        if( theFactory != null ) {
            return theFactory;
        }
        return new ListenerEventQueueFactoryDefaultImpl<>();
    }
}
