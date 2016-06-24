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
import uk.co.octatec.scdds.GlobalProperties;
import uk.co.octatec.scdds.cache.info.CacheInfoDisplay;
import uk.co.octatec.scdds.cache.info.CacheInfoDisplayFactory;
import uk.co.octatec.scdds.cache.publish.CacheFilter;
import uk.co.octatec.scdds.cache.publish.MapFactory;
import uk.co.octatec.scdds.cache.publish.MapFactoryDefaultImpl;
import uk.co.octatec.scdds.cache.subscribe.*;
import uk.co.octatec.scdds.net.html.HttpServerFactory;
import uk.co.octatec.scdds.net.registry.CacheLocator;
import uk.co.octatec.scdds.net.registry.CacheLocatorImpl;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Jeromy Drake on 04/05/2016.
 */
public class SubscriptionCacheBuilder<K, T extends ImmutableEntry> {

    private final static Logger log = LoggerFactory.getLogger(SubscriptionCacheBuilder.class);

    private static final int DEFAULT_HEARTBEAT_SECONDS = 15;

    final private ListenerEventQueueFactory<K, ListenerEvent<K, T>> listenerEventQueueFactory;
    final private CacheSubscriberFactory<K, T> cacheSubscriberFactory;
    final private MapFactory<K, T> mapFactory;
    final private CacheLocator locator;
    final private ListenerEventFactory<K,T> listenerEventFactory;
    final private CacheInfoDisplayFactory cacheInfoDisplayFactory;

    final static private ConcurrentHashMap<Integer,CacheSubscriber> subscriptions = new ConcurrentHashMap<>();

    public SubscriptionCacheBuilder(List<InetSocketAddress> registries,
                                    ListenerEventQueueFactory<K, ListenerEvent<K, T>> listenerEventQueueFactory,
                                    CacheSubscriberFactory<K, T> cacheSubscriberFactory,
                                    MapFactory<K, T> mapFactory) {

        locator = new CacheLocatorImpl(registries);
        this.listenerEventQueueFactory = defaultListenerEventQueueFactory(listenerEventQueueFactory);
        //this.listenerEventQueueFactory = listenerEventQueueFactory==null ? new ListenerEventRingQueueFactory<K,ListenerEvent<K, T>>() : listenerEventQueueFactory;
        this.cacheSubscriberFactory = cacheSubscriberFactory==null ? new CacheSubscriberFactoryDefaultImpl<K,T>() : cacheSubscriberFactory;
        this.mapFactory = mapFactory==null ? new MapFactoryDefaultImpl<K,T>() : mapFactory;
        this.listenerEventFactory = new ListenerEventFactoryDefaultImpl<K,T>();
        this.cacheInfoDisplayFactory = new HttpServerFactory();
    }

    public SubscriptionCacheBuilder(List<InetSocketAddress> registries,
                                    ListenerEventQueueFactory<K, ListenerEvent<K, T>> listenerEventQueueFactory,
                                    CacheSubscriberFactory<K, T> cacheSubscriberFactory,
                                    MapFactory<K, T> mapFactory,
                                    ListenerEventFactory<K,T> listenerEventFactory,
                                    CacheInfoDisplayFactory cacheInfoDisplayFactory) {

        locator = new CacheLocatorImpl(registries);
        this.listenerEventQueueFactory = defaultListenerEventQueueFactory(listenerEventQueueFactory);
        //this.listenerEventQueueFactory = listenerEventQueueFactory==null ? new ListenerEventRingQueueFactory<K,ListenerEvent<K, T>>() : listenerEventQueueFactory;
        this.cacheSubscriberFactory = cacheSubscriberFactory==null ? new CacheSubscriberFactoryDefaultImpl<K,T>() : cacheSubscriberFactory;
        this.mapFactory = mapFactory==null ? new MapFactoryDefaultImpl<K,T>() : mapFactory;
        this.listenerEventFactory = listenerEventFactory==null ? new ListenerEventFactoryDefaultImpl<K,T>() : listenerEventFactory;
        this.cacheInfoDisplayFactory = cacheInfoDisplayFactory==null? new HttpServerFactory() : cacheInfoDisplayFactory;
    }

    public SubscriptionCacheBuilder(CacheLocator locator,
                                    ListenerEventQueueFactory<K, ListenerEvent<K, T>> listenerEventQueueFactory,
                                    CacheSubscriberFactory<K, T> cacheSubscriberFactory,
                                    MapFactory<K, T> mapFactory) {

        this.locator = locator;
        this.listenerEventQueueFactory = defaultListenerEventQueueFactory(listenerEventQueueFactory);
        //this.listenerEventQueueFactory = listenerEventQueueFactory==null ? new ListenerEventRingQueueFactory<K,ListenerEvent<K, T>>() : listenerEventQueueFactory;
        this.cacheSubscriberFactory = cacheSubscriberFactory==null ? new CacheSubscriberFactoryDefaultImpl<K,T>() : cacheSubscriberFactory;
        this.mapFactory = mapFactory==null ? new MapFactoryDefaultImpl<K,T>() : mapFactory;
        this.listenerEventFactory = new ListenerEventFactoryDefaultImpl<K,T>();
        this.cacheInfoDisplayFactory = new HttpServerFactory();
    }

    public SubscriptionCacheBuilder(List<InetSocketAddress> registries) {
        locator = new CacheLocatorImpl(registries);
        this.listenerEventQueueFactory = defaultListenerEventQueueFactory(null);
        this.cacheSubscriberFactory = new CacheSubscriberFactoryDefaultImpl<K,T>();
        this.mapFactory = new MapFactoryDefaultImpl<>();
        this.listenerEventFactory = new ListenerEventFactoryDefaultImpl<K,T>();
        this.cacheInfoDisplayFactory = new HttpServerFactory();
    }

    public ImmutableCache<K, T> subscribe(String cacheName) {
        return subscribe(cacheName, null, DEFAULT_HEARTBEAT_SECONDS);
    }

    public ImmutableCache<K, T> subscribe(String cacheName, CacheFilter<K, T> filter) {
        return subscribe(cacheName, filter, DEFAULT_HEARTBEAT_SECONDS);
    }

    public ImmutableCache<K, T> subscribe(String cacheName, CacheFilter<K, T> filter, String filterArg) {
        return subscribe(cacheName, filter, filterArg, DEFAULT_HEARTBEAT_SECONDS);
    }

    public ImmutableCache<K, T> subscribe(String cacheName, CacheFilter<K, T> filter, int heartbeatSeconds) {
        return subscribe(cacheName, filter, null, heartbeatSeconds);
    }

    public ImmutableCache<K, T> subscribe(String cacheName, CacheFilter<K, T> filter, String filterArg, int heartbeatSeconds) {

        log.info("will subscribe to cache [{}] filter=[{}] filterArg=[{}] heartbeatSeconds=[{}]", cacheName, filter, heartbeatSeconds);

        CacheImplClientSide<K,T> cacheImpl = new  CacheImplClientSide<K,T>(cacheName, listenerEventQueueFactory, mapFactory, listenerEventFactory);

        CacheSubscriber<K, T> cacheSubscriber = cacheSubscriberFactory.create(cacheImpl, locator, filter, filterArg, new InitialUpdateReaderFactoryImpl<K, T>());
        cacheSubscriber.subscribe(heartbeatSeconds);
        Integer key = System.identityHashCode(cacheImpl);
        log.info("add subscription [{}] [{}]", key, cacheSubscriber);
        subscriptions.put(key, cacheSubscriber);

        if(GlobalProperties.exposeHttpServer ) {
            CacheInfoDisplay cacheInfoDisplay = cacheInfoDisplayFactory.get();
            log.info("enabled client cacheInfoDisplay port=[{}] cache-name=[{}]", cacheInfoDisplay.getHttpPort(), cacheImpl.getName());
            cacheInfoDisplay.addCache(cacheImpl);
        }

        return cacheImpl;
    }

    public static  <K, T> boolean unSubscribe(ImmutableCache<K, T> cache) {
        // this will probably not be a used very much, it is included mostly for completeness rather than in the
        // expectation that caches will be subscribed and then unsubscribe, the expection is that a cache will remain subscribed
        // until the application exits, an explicit un-subscribeis not required at application exit
        log.info("unsubscribe to cache [{}]", cache.getName());
        Integer key = System.identityHashCode(cache);
        CacheSubscriber<K, T> cacheSubscriber =  subscriptions.remove(key);
        if( cacheSubscriber == null ) {
            log.info("unknown cache [{}], can't unsubscribe", cache.getName());
            return false;
        }
        cacheSubscriber.unSubscribe();
        return true;
    }

    public ListenerEventQueueFactory<K, ListenerEvent<K, T>> getListenerEventQueueFactory() {
        return listenerEventQueueFactory;
    }

    public CacheSubscriberFactory<K, T> getCacheSubscriberFactory() {
        return cacheSubscriberFactory;
    }

    public MapFactory<K, T> getMapFactory() {
        return mapFactory;
    }

    public CacheLocator getLocator() {
        return locator;
    }

    public ListenerEventFactory<K, T> getListenerEventFactory() {
        return listenerEventFactory;
    }

    static public int getSubscriptionsSize() {
        log.info("subscriptions {}", subscriptions.entrySet());
        return subscriptions.size();
    }

    static  <K,T> ListenerEventQueueFactory<K, ListenerEvent<K,T>>  defaultListenerEventQueueFactory(ListenerEventQueueFactory<K, ListenerEvent<K,T>>  theFactory) {
        if( theFactory != null ) {
            return theFactory;
        }
        return new ListenerEventQueueFactoryDefaultImpl<>();
    }
}
