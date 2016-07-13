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
import jdk.nashorn.internal.runtime.GlobalConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.ConditionalCompilation;
import uk.co.octatec.scdds.GlobalDefaults;
import uk.co.octatec.scdds.GlobalProperties;
import uk.co.octatec.scdds.cache.info.CacheInfoDisplay;
import uk.co.octatec.scdds.cache.info.CacheInfoDisplayFactory;
import uk.co.octatec.scdds.cache.persistence.CacheLoader;
import uk.co.octatec.scdds.cache.persistence.CacheLoaderPersisterFactory;
import uk.co.octatec.scdds.cache.persistence.EntryPersister;
import uk.co.octatec.scdds.cache.persistence.NoOpCacheLoaderPersisterFactory;
import uk.co.octatec.scdds.cache.publish.*;
import uk.co.octatec.scdds.cache.publish.threading.Threader;
import uk.co.octatec.scdds.cache.publish.threading.ThreaderFactory;
import uk.co.octatec.scdds.cache.publish.threading.ThreaderFactoryImpl;
import uk.co.octatec.scdds.net.html.HttpServerFactory;
import uk.co.octatec.scdds.net.registry.CacheRegistrar;
import uk.co.octatec.scdds.net.registry.CacheRegistrarImpl;
import uk.co.octatec.scdds.net.serialize.SerializerFactory;
import uk.co.octatec.scdds.net.serialize.SerializerFactoryDefaultImpl;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Jeromy Drake on 02/05/2016.
 */
public class PublishingCacheBuilder<K,T extends ImmutableEntry> {

    private final static Logger log = LoggerFactory.getLogger(PublishingCacheBuilder.class);

    private final List<InetSocketAddress> registries;
    private final ListenerEventQueueFactory<K, ListenerEvent<K,T>> listenerEventQueueFactory;
    private final CacheLoaderPersisterFactory<K,T> cacheLoaderPersisterFactory;
    private final CachePublisherFactory<K,T> cachePublisherFactory;
    private final SerializerFactory<K,T> serializerFactory;
    private final MapFactory<K,T> mapFactory;
    private final GeneralRequestHandlerFactory  generalRequestHandlerFactory;
    private final ListenerEventFactory<K,T> listenerEventFactory;
    private final ThreaderFactory threaderFactory;
    private final CacheInfoDisplayFactory cacheInfoDisplayFactory;

    private static ConcurrentHashMap<Integer, CachePublisher> liveCacheList = new ConcurrentHashMap<Integer, CachePublisher>();


    public PublishingCacheBuilder(List<InetSocketAddress> registries) {
        this.registries = registries;
        listenerEventQueueFactory = defaultListenerEventQueueFactory(null);
        cacheLoaderPersisterFactory = new NoOpCacheLoaderPersisterFactory<>();
        cachePublisherFactory = new CachePublisherFactoryDefaultImpl<>();
        serializerFactory = new SerializerFactoryDefaultImpl<>();
        mapFactory = new MapFactoryDefaultImpl<>();
        generalRequestHandlerFactory = new  GeneralRequestHandlerFactoryImpl();
        listenerEventFactory = new ListenerEventFactoryDefaultImpl<>();
        this.threaderFactory = GlobalDefaults.threaderFactory;
        this.cacheInfoDisplayFactory = new HttpServerFactory();
    }

    public PublishingCacheBuilder(List<InetSocketAddress> registries, SerializerFactory<K,T> serializerFactory) {
        this.registries = registries;
        this.serializerFactory = serializerFactory == null ? new SerializerFactoryDefaultImpl<K,T>() : serializerFactory;;
        listenerEventQueueFactory = defaultListenerEventQueueFactory(null);
        cacheLoaderPersisterFactory = new NoOpCacheLoaderPersisterFactory<>();
        cachePublisherFactory = new CachePublisherFactoryDefaultImpl<>();
        mapFactory = new MapFactoryDefaultImpl<>();
        generalRequestHandlerFactory = new  GeneralRequestHandlerFactoryImpl();
        listenerEventFactory = new ListenerEventFactoryDefaultImpl<>();
        this.threaderFactory = GlobalDefaults.threaderFactory;
        this.cacheInfoDisplayFactory = new HttpServerFactory();
    }

    public PublishingCacheBuilder(List<InetSocketAddress> registries, CacheLoaderPersisterFactory<K,T> cacheLoaderPersisterFactory) {
        this.registries = registries;
        this.cacheLoaderPersisterFactory = cacheLoaderPersisterFactory==null ? new NoOpCacheLoaderPersisterFactory<K,T>() : cacheLoaderPersisterFactory;;;
        listenerEventQueueFactory = defaultListenerEventQueueFactory(null);
        cachePublisherFactory = new CachePublisherFactoryDefaultImpl<>();
        serializerFactory = new SerializerFactoryDefaultImpl<>();
        mapFactory = new MapFactoryDefaultImpl<>();
        generalRequestHandlerFactory = new  GeneralRequestHandlerFactoryImpl();
        listenerEventFactory = new ListenerEventFactoryDefaultImpl<>();
        threaderFactory = GlobalDefaults.threaderFactory;
        this.cacheInfoDisplayFactory = new HttpServerFactory();
    }

    public PublishingCacheBuilder(List<InetSocketAddress> registries, SerializerFactory<K,T> serializerFactory, CacheLoaderPersisterFactory<K,T> cacheLoaderPersisterFactory) {
        this.registries = registries;
        this.serializerFactory = serializerFactory == null ? new SerializerFactoryDefaultImpl<K,T>() : serializerFactory;
        this.cacheLoaderPersisterFactory = cacheLoaderPersisterFactory==null ? new NoOpCacheLoaderPersisterFactory<K,T>() : cacheLoaderPersisterFactory;;
        listenerEventQueueFactory = defaultListenerEventQueueFactory(null);
        //listenerEventQueueFactory = new ListenerEventRingQueueFactory<>();
        cachePublisherFactory = new CachePublisherFactoryDefaultImpl<>();
        mapFactory = new MapFactoryDefaultImpl<>();
        generalRequestHandlerFactory = new  GeneralRequestHandlerFactoryImpl();
        listenerEventFactory = new ListenerEventFactoryDefaultImpl<>();
        threaderFactory = GlobalDefaults.threaderFactory;
        this.cacheInfoDisplayFactory = new HttpServerFactory();
    }

    public PublishingCacheBuilder(List<InetSocketAddress> registries,
                                  SerializerFactory<K,T> serializerFactory,
                                  CacheLoaderPersisterFactory<K,T> cacheLoaderPersisterFactory,
                                  ListenerEventQueueFactory<K, ListenerEvent<K,T>> listenerEventQueueFactory,
                                  MapFactory<K,T>  mapFactory,
                                  CachePublisherFactory<K,T>  cachePublisherFactory


    ) {
        this.registries = registries;
        this.serializerFactory = serializerFactory==null? new SerializerFactoryDefaultImpl<K,T>() : serializerFactory;
        this.cacheLoaderPersisterFactory = cacheLoaderPersisterFactory==null ? new NoOpCacheLoaderPersisterFactory<K,T>() : cacheLoaderPersisterFactory;
        this.listenerEventQueueFactory = defaultListenerEventQueueFactory(listenerEventQueueFactory);
        //this.listenerEventQueueFactory = listenerEventQueueFactory==null ? new ListenerEventRingQueueFactory<K,ListenerEvent<K,T>>() : listenerEventQueueFactory;
        this.cachePublisherFactory = cachePublisherFactory==null ? new CachePublisherFactoryDefaultImpl<K,T>() : cachePublisherFactory;
        this.mapFactory = mapFactory==null ? new MapFactoryDefaultImpl<K,T>() : mapFactory;
        generalRequestHandlerFactory = new  GeneralRequestHandlerFactoryImpl();
        listenerEventFactory = new ListenerEventFactoryDefaultImpl<>();
        threaderFactory = GlobalDefaults.threaderFactory;
        this.cacheInfoDisplayFactory = new HttpServerFactory();
    }


    public PublishingCacheBuilder(List<InetSocketAddress> registries,
                                  SerializerFactory<K,T> serializerFactory,
                                  CacheLoaderPersisterFactory<K,T> cacheLoaderPersisterFactory,
                                  ListenerEventQueueFactory<K, ListenerEvent<K,T>> listenerEventQueueFactory,
                                  MapFactory<K,T>  mapFactory,
                                  CachePublisherFactory<K,T>  cachePublisherFactory,
                                  ListenerEventFactory<K,T>  listenerEventFactory,
                                  ThreaderFactory threaderFactory,
                                  GeneralRequestHandlerFactory generalRequestHandlerFactory,
                                  CacheInfoDisplayFactory cacheInfoDisplayFactory


    ) {
        this.registries = registries;
        this.serializerFactory = serializerFactory==null? new SerializerFactoryDefaultImpl<K,T>() : serializerFactory;
        this.cacheLoaderPersisterFactory = cacheLoaderPersisterFactory==null ? new NoOpCacheLoaderPersisterFactory<K,T>() : cacheLoaderPersisterFactory;
        this.listenerEventQueueFactory = defaultListenerEventQueueFactory(listenerEventQueueFactory);
        this.cachePublisherFactory = cachePublisherFactory==null ? new CachePublisherFactoryDefaultImpl<K,T>() : cachePublisherFactory;
        this.mapFactory = mapFactory==null ? new MapFactoryDefaultImpl<K,T>() : mapFactory;
        this.listenerEventFactory  = listenerEventFactory==null ? new ListenerEventFactoryDefaultImpl<K,T>():listenerEventFactory;
        this.threaderFactory = threaderFactory == null ? GlobalDefaults.threaderFactory :  threaderFactory;
        this.generalRequestHandlerFactory = generalRequestHandlerFactory==null ? new  GeneralRequestHandlerFactoryImpl() : generalRequestHandlerFactory;
        this.cacheInfoDisplayFactory = cacheInfoDisplayFactory==null? new HttpServerFactory() : cacheInfoDisplayFactory;
    }

    public Cache<K,T> build(String cacheName) {
        return build(cacheName, 0/* unlimited retries*/);
    }

    Cache<K,T> build(String cacheName, int registarRetries) {

        log.info("building cache [{}] registarRetries=[{}]", cacheName, registarRetries);

        CacheImpl<K,T> cacheImpl = new  CacheImpl<K,T>(cacheName, listenerEventQueueFactory, mapFactory, listenerEventFactory);

        try {

            // load the cache with its persistent data, there my be no persistent data to load,
            // the default cacheLoader is a No-Op implementation
            log.info("using cacheLoaderPersisterFactory [{}]", cacheLoaderPersisterFactory.getClass().getName());
            CacheLoader<K,T> cacheLoader = cacheLoaderPersisterFactory.createCacheLoader(cacheName);
            if( cacheLoader == null ) {
                // the cacheLoaderPersisterFactory is free to return nulls for either the Loader or Persister
                cacheLoader = (new NoOpCacheLoaderPersisterFactory<K,T>()).createCacheLoader(cacheName);
            }
            cacheLoader.open();
            cacheLoader.loadCache(cacheImpl);
            cacheLoader.close();
            log.info("current cache size after any data loading, size=[{}]", cacheImpl.size());

            // set the cache entryPersister, there may be no persistence require, the default
            // entryPersister is a No-Op implementation
            EntryPersister<K,T> entryPersister = cacheLoaderPersisterFactory.createEntryPersister(cacheName);
            if( entryPersister == null ) {
                // the cacheLoaderPersisterFactory is free to return nulls for either the Loader or Persister
                entryPersister = (new NoOpCacheLoaderPersisterFactory<K,T>()).createEntryPersister(cacheName);
            }
            entryPersister.open();
            cacheImpl.setEntryPersister(entryPersister);

            // add the cache-publisher
            Threader threader = threaderFactory.getInstance();
            if( threader != null ) {
                // the threader factory can return null, indicating network-sends are single threaded
                threader.start();
            }

            CachePublisher<K, T> cachePublisher = cachePublisherFactory.create(cacheImpl, serializerFactory, generalRequestHandlerFactory.getInstance(), threader);
            int port = cachePublisher.initializePort();
            cacheImpl.setCachePublisher(cachePublisher);

            log.info("cache [{}] will receive subscriptions on port [{}]", cacheName, port);

            CacheInfoDisplay cacheInfoDisplay = null;
            int httpPort = 0;
            if(GlobalProperties.exposeHttpServer) {
                // a basic http server to display active caches and the data in them
                cacheInfoDisplay = cacheInfoDisplayFactory.get();
                httpPort = cacheInfoDisplay.getHttpPort();
            }

            CacheRegistrar registrar = new CacheRegistrarImpl(registries);
            String hostname = InetAddress.getLocalHost().getHostName();
            registrar.registerCache(cacheName, hostname, port, httpPort, registarRetries);

            cachePublisher.start();

            cacheImpl.start();

            log.info("cache [{}] is built and started, listening on port [{}] for subscription requests", cacheName, port);

            liveCacheList.put(System.identityHashCode(cacheImpl), cachePublisher);

            if( cacheInfoDisplay != null ) {
                cacheInfoDisplay.addCache(cacheImpl);
                log.info("enabled server cacheInfoDisplay port=[{}] cache-name=[{}]", httpPort, cacheImpl.getName());
            }

            return cacheImpl;
        }
        catch( Throwable t) {
            log.error("exception building cache [{}]", cacheName, t);
            throw new RuntimeException("failed to build cache "+cacheName, t);
        }
    }

    public static <K,T extends ImmutableEntry> void stop(Cache<K,T> cache) {
        try {
            log.info("stopping cache [{}]", cache.getName());
            CachePublisher cachePublisher = liveCacheList.remove(System.identityHashCode(cache));
            if (cachePublisher != null) {
                cachePublisher.stop();
            } else {
                log.warn("trying to stop an unknown cache");
            }
        }
        catch( Throwable t) {
            log.error("exception tryinbh to stop cache", t);
        }
    }

    public static List<InetSocketAddress> addrListFromString(String str){
        ArrayList<InetSocketAddress> list = new ArrayList<>();
        String[]  addressList = str.split("[,]");
        for(String addrStr : addressList ){
            String[] hostPort = addrStr.split("[:]");
            if( hostPort.length != 2 ) {
                log.error("bad registry-address in registry-address-string [{}], reuired fromat is hostname:port", hostPort);
            }
            log.info("regsitry configured - located at {}:{}", hostPort[0], hostPort[1]);
            InetSocketAddress addr = new  InetSocketAddress(hostPort[0], Integer.parseInt(hostPort[1]));
            list.add(addr);
        }
        return list;
    }

    ListenerEventQueueFactory<K, ListenerEvent<K, T>> getListenerEventQueueFactory() {
        return listenerEventQueueFactory;
    }

    CacheLoaderPersisterFactory<K, T> getCacheLoaderPersisterFactory() {
        return cacheLoaderPersisterFactory;
    }

    CachePublisherFactory<K, T> getCachePublisherFactory() {
        return cachePublisherFactory;
    }

    SerializerFactory<K, T> getSerializerFactory() {
        return serializerFactory;
    }

    MapFactory<K, T> getMapFactory() {
        return mapFactory;
    }

    GeneralRequestHandlerFactory getGeneralRequestHandlerFactory() {
        return generalRequestHandlerFactory;
    }

    public ThreaderFactory getThreaderFactory() {
        return threaderFactory;
    }

    ListenerEventFactory<K, T> getListenerEventFactory() {
        return listenerEventFactory;
    }

    static  <K,T extends ImmutableEntry> ListenerEventQueueFactory<K, ListenerEvent<K,T>>  defaultListenerEventQueueFactory(ListenerEventQueueFactory<K, ListenerEvent<K,T>>  theFactory) {
        if( theFactory != null ) {
            return theFactory;
        }
        return new ListenerEventQueueFactoryDefaultImpl<>();
    }
}
