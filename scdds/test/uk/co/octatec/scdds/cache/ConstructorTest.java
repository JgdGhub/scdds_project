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
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.cache.persistence.ObjectStoreCacheLoaderPersisterFactory;
import uk.co.octatec.scdds.cache.publish.CachePublisherFactory;
import uk.co.octatec.scdds.cache.publish.GeneralRequestHandlerFactory;
import uk.co.octatec.scdds.cache.publish.MapFactory;
import uk.co.octatec.scdds.cache.publish.MapFactoryDefaultImpl;
import uk.co.octatec.scdds.cache.publish.threading.ThreaderFactory;
import uk.co.octatec.scdds.cache.publish.threading.ThreaderFactoryImpl;
import uk.co.octatec.scdds.mock.MockThreaderFactory;
import uk.co.octatec.scdds.utilities.SimpleCacheListener;
import uk.co.octatec.scdds.utilities.SimpleData;
import uk.co.octatec.scdds.mock.MockCachePublisherFactory;
import uk.co.octatec.scdds.mock.MockCacheSubscriberFactory;
import uk.co.octatec.scdds.mock.MockGeneralRequestHandlerFactory;
import uk.co.octatec.scdds.net.registry.CacheLocatorImpl;
import uk.co.octatec.scdds.net.serialize.SerializerFactory;
import uk.co.octatec.scdds.net.serialize.SerializerFactoryDefaultImpl;
import uk.co.octatec.scdds.odb.ObjectDataStoreTest;
import uk.co.octatec.scdds.queue.Event;
import uk.co.octatec.scdds.queue.EventQueue;
import uk.co.octatec.scdds.queue.EventQueueDefaultImpl;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by Jeromy Drake on 07/05/16
 */
public class ConstructorTest {

    // test some constructors

    private final Logger log = LoggerFactory.getLogger(ObjectDataStoreTest.class);

    private static class MyEventQueueFactory<K,E extends Event<K>> implements ListenerEventQueueFactory<K,E> {
        volatile boolean used;
        @Override
        public EventQueue<K,E> create(String name) {
            used = true;
            return new EventQueueDefaultImpl<>(name);
        }
    }


    private static class MyMapFactory<K,T> implements MapFactory<K,T> {
        MapFactoryDefaultImpl<K,T> delegate = new MapFactoryDefaultImpl<K,T>();
        volatile boolean used;
        @Override
        public ConcurrentMap<K, T> create() {
            used = true;
            return delegate.create();
        }
    }

    private static class  MyListenerEventFactory<K,T> implements ListenerEventFactory<K,T> {
        ListenerEventFactoryDefaultImpl<K,T> delegate = new ListenerEventFactoryDefaultImpl<>();
        volatile boolean used;
        @Override
        public ListenerEvent<K, T> create() {
            used = true;
            return delegate.create();
        }
    }

    @Test
    public void cacheConstructorTest3ArgsWithNulls() throws InterruptedException {
        log.info("## constructorTest3");
        CacheImpl<String, SimpleData> cache = new CacheImpl<>("Test", null,  null) ;
        SimpleCacheListener<String, SimpleData> listener = new SimpleCacheListener<>();
        cache.start();
        cache.addListener(listener);
        cache.put("A", new SimpleData("A", 1));

        listener.awaitOnUpdateCountGte(1);

        Assert.assertTrue("update received", listener.onUpdateCount > 0);
    }

    @Test
    public void cacheConstructorTest4ArgsWithNulls() throws InterruptedException {
        log.info("## constructorTest2");
        CacheImpl<String, SimpleData> cache = new CacheImpl<>("Test", null,  null, null) ;
        SimpleCacheListener<String, SimpleData> listener = new SimpleCacheListener<>();
        cache.start();
        cache.addListener(listener);
        cache.put("A", new SimpleData("A", 1));

        listener.awaitOnUpdateCountGte(1);

        Assert.assertTrue("update received", listener.onUpdateCount > 0);
    }

    @Test
    public void cacheConstructorTest4ArgsWithFactories() throws InterruptedException {

        log.info("## constructorTest4WithFactories");

        MyEventQueueFactory<String, ListenerEvent<String, SimpleData>> eventQueueFactory = new MyEventQueueFactory<>();
        MyMapFactory<String,SimpleData> mapFactory = new MyMapFactory<String,SimpleData>();
        MyListenerEventFactory<String,SimpleData> eventFactory = new   MyListenerEventFactory<>();
        CacheImpl<String, SimpleData> cache = new CacheImpl<>("Test", eventQueueFactory,  mapFactory, eventFactory) ;
        SimpleCacheListener<String, SimpleData> listener = new SimpleCacheListener<>();
        cache.start();
        cache.addListener(listener);
        cache.put("A", new SimpleData("A", 1));

        listener.awaitOnUpdateCountGte(1);

        Assert.assertTrue("update received", listener.onUpdateCount > 0);
        Assert.assertTrue("eventQueueFactory used", eventQueueFactory.used);
        Assert.assertTrue("mapFactory used", mapFactory.used);
        Assert.assertTrue("eventFactory used", eventFactory.used);

    }

    @Test
    public void cacheConstructorTest3ArgsWithFactories() throws InterruptedException {

        log.info("## constructorTest4WithFactories");

        MyEventQueueFactory<String, ListenerEvent<String, SimpleData>> eventQueueFactory = new MyEventQueueFactory<>();
        MyMapFactory<String,SimpleData> mapFactory = new MyMapFactory<String,SimpleData>();
        CacheImpl<String, SimpleData> cache = new CacheImpl<>("Test", eventQueueFactory,  mapFactory) ;
        SimpleCacheListener<String, SimpleData> listener = new SimpleCacheListener<>();
        cache.start();
        cache.addListener(listener);
        cache.put("A", new SimpleData("A", 1));

        listener.awaitOnUpdateCountGte(1);

        Assert.assertTrue("update received", listener.onUpdateCount > 0);
        Assert.assertTrue("eventQueueFactory used", eventQueueFactory.used);
        Assert.assertTrue("mapFactory used", mapFactory.used);
    }

    @Test
    public void cacheStaticStart2Args() throws InterruptedException{

        log.info("## staticStart2Args");

        ListenerEventQueueFactory<String, ListenerEvent<String,SimpleData>> listenerEventQueueFactory= new MyEventQueueFactory<>();
        MyMapFactory<String,SimpleData> mapFactory = new MyMapFactory<String,SimpleData>();

        SimpleCacheListener<String, SimpleData> listener = new SimpleCacheListener<>();
        Cache<String, SimpleData> cache =  CacheImpl.createLocalCache("MyCache", listenerEventQueueFactory, mapFactory);
        cache.addListener(listener);
        cache.put("A", new SimpleData("A", 1));

        listener.awaitOnUpdateCountGte(1);

        Assert.assertTrue("update received", listener.onUpdateCount > 0);
    }

    @Test
    public void cacheStaticStart2ArgsNull() throws InterruptedException{

        log.info("## staticStart2ArgsNull");

        ListenerEventQueueFactory<String, ListenerEvent<String,SimpleData>> listenerEventQueueFactory= null;
        MyMapFactory<String,SimpleData> mapFactory = null;

        SimpleCacheListener<String, SimpleData> listener = new SimpleCacheListener<>();
        Cache<String, SimpleData> cache =  CacheImpl.createLocalCache("MyCache", listenerEventQueueFactory, mapFactory);
        cache.addListener(listener);
        cache.put("A", new SimpleData("A", 1));

        listener.awaitOnUpdateCountGte(1);

        Assert.assertTrue("update received", listener.onUpdateCount > 0);
    }

    @Test
    public void cacheStaticStart3Args() throws InterruptedException{

        log.info("## staticStart3Args");

        ListenerEventQueueFactory<String, ListenerEvent<String,SimpleData>> listenerEventQueueFactory= new MyEventQueueFactory<>();
        MyMapFactory<String,SimpleData> mapFactory = new MyMapFactory<String,SimpleData>();
        ListenerEventFactory<String,SimpleData>  listenerEventFactory = new ListenerEventFactoryDefaultImpl<>();

        SimpleCacheListener<String, SimpleData> listener = new SimpleCacheListener<>();
        Cache<String, SimpleData> cache =  CacheImpl.createLocalCache("MyCache", listenerEventQueueFactory, mapFactory, listenerEventFactory);
        cache.addListener(listener);
        cache.put("A", new SimpleData("A", 1));

        listener.awaitOnUpdateCountGte(1);

        Assert.assertTrue("update received", listener.onUpdateCount > 0);
    }

    @Test
    public void cacheStaticStart3ArgsNull() throws InterruptedException{

        log.info("## staticStart3ArgsNull");

        ListenerEventQueueFactory<String, ListenerEvent<String,SimpleData>> listenerEventQueueFactory=null;
        MyMapFactory<String,SimpleData> mapFactory = null;
        ListenerEventFactory<String,SimpleData>  listenerEventFactory = null;

        SimpleCacheListener<String, SimpleData> listener = new SimpleCacheListener<>();
        Cache<String, SimpleData> cache =  CacheImpl.createLocalCache("MyCache", listenerEventQueueFactory, mapFactory, listenerEventFactory);
        cache.addListener(listener);
        cache.put("A", new SimpleData("A", 1));

        listener.awaitOnUpdateCountGte(1);

        Assert.assertTrue("update received", listener.onUpdateCount > 0);
    }

    @Test
    public void publishingBuilder2ArgsA() {

        log.info("## publishingBuilder2ArgsA");

        List<InetSocketAddress> registries = new ArrayList<>();

        SerializerFactory<String,SimpleData> serializerFactory = new SerializerFactoryDefaultImpl<>();

        PublishingCacheBuilder<String,SimpleData> builder = new PublishingCacheBuilder(registries, serializerFactory);

        Assert.assertTrue("serializerFactory set on buildrt", isSameObject(builder.getSerializerFactory(),serializerFactory) );
    }

    @Test
    public void publishingBuilder2ArgsB() {

        log.info("## publishingBuilder2ArgsB");

        List<InetSocketAddress> registries = new ArrayList<>();

        ObjectStoreCacheLoaderPersisterFactory<String,SimpleData> cacheLoaderPersisterFactory = new   ObjectStoreCacheLoaderPersisterFactory<>();

        PublishingCacheBuilder<String,SimpleData> builder = new PublishingCacheBuilder(registries, cacheLoaderPersisterFactory);

        Assert.assertTrue("cacheLoaderPersisterFactory set on builder", isSameObject(builder.getCacheLoaderPersisterFactory(),cacheLoaderPersisterFactory) );
    }

    @Test
    public void publishingBuilder2ArgsWithNullsA() {

        log.info("## publishingBuilder2ArgsWithNullsA");

        List<InetSocketAddress> registries = new ArrayList<>();

        SerializerFactory<String,SimpleData> serializerFactory = null;

        PublishingCacheBuilder<String,SimpleData> builder = new PublishingCacheBuilder(registries, serializerFactory);

        Assert.assertNotNull("serializerFactory set on builder", builder.getSerializerFactory() );
        Assert.assertNotNull("cacheLoaderPersisterFactory set on builder", builder.getCacheLoaderPersisterFactory() );
        Assert.assertNotNull("mapFactory set on builder", builder.getMapFactory() );
        Assert.assertNotNull("listenerEventQueueFactory set on builder", builder.getListenerEventQueueFactory() );
        Assert.assertNotNull("cachePublisherFactory set on builder", builder.getCachePublisherFactory() );
        Assert.assertNotNull("listenerEventFactory set on builder", builder.getListenerEventFactory() );
        Assert.assertNotNull("generalRequestHandlerFactory set on builder", builder.getGeneralRequestHandlerFactory() );
    }

    @Test
    public void publishingBuilder2ArgsWithNullsB() {

        log.info("## publishingBuilder2ArgsWithNullsB");

        List<InetSocketAddress> registries = new ArrayList<>();

        ObjectStoreCacheLoaderPersisterFactory<String,SimpleData> cacheLoaderPersisterFactory = null;

        PublishingCacheBuilder<String,SimpleData> builder = new PublishingCacheBuilder(registries, cacheLoaderPersisterFactory);

        Assert.assertNotNull("serializerFactory set on builder", builder.getSerializerFactory() );
        Assert.assertNotNull("cacheLoaderPersisterFactory set on builder", builder.getCacheLoaderPersisterFactory() );
        Assert.assertNotNull("mapFactory set on builder", builder.getMapFactory() );
        Assert.assertNotNull("listenerEventQueueFactory set on builder", builder.getListenerEventQueueFactory() );
        Assert.assertNotNull("cachePublisherFactory set on builder", builder.getCachePublisherFactory() );
        Assert.assertNotNull("listenerEventFactory set on builder", builder.getListenerEventFactory() );
        Assert.assertNotNull("generalRequestHandlerFactory set on builder", builder.getGeneralRequestHandlerFactory() );
    }

    @Test
    public void publishingBuilder3Args() {

        log.info("## publishingBuilder3Args");

        List<InetSocketAddress> registries = new ArrayList<>();

        SerializerFactory<String,SimpleData> serializerFactory = new SerializerFactoryDefaultImpl<>();
        ObjectStoreCacheLoaderPersisterFactory<String,SimpleData> cacheLoaderPersisterFactory = new   ObjectStoreCacheLoaderPersisterFactory<>();

        PublishingCacheBuilder<String,SimpleData> builder = new PublishingCacheBuilder(registries, serializerFactory, cacheLoaderPersisterFactory);

        Assert.assertTrue("serializerFactory set on builder", isSameObject(builder.getSerializerFactory(),serializerFactory ) );
        Assert.assertTrue("cacheLoaderPersisterFactory set on builder", isSameObject(builder.getCacheLoaderPersisterFactory(),cacheLoaderPersisterFactory ) );
    }

    @Test
    public void publishingBuilder3ArgsWithNulls() {

        log.info("## publishingBuilder3ArgsWithNulls");

        List<InetSocketAddress> registries = new ArrayList<>();

        SerializerFactory<String,SimpleData> serializerFactory = null;
        ObjectStoreCacheLoaderPersisterFactory<String,SimpleData> cacheLoaderPersisterFactory = null;

        PublishingCacheBuilder<String,SimpleData> builder = new PublishingCacheBuilder(registries, serializerFactory, cacheLoaderPersisterFactory);

        Assert.assertNotNull("serializerFactory set on builder", builder.getSerializerFactory() );
        Assert.assertNotNull("cacheLoaderPersisterFactory set on builder", builder.getCacheLoaderPersisterFactory() );
        Assert.assertNotNull("mapFactory set on builder", builder.getMapFactory() );
        Assert.assertNotNull("listenerEventQueueFactory set on builder", builder.getListenerEventQueueFactory() );
        Assert.assertNotNull("cachePublisherFactory set on builder", builder.getCachePublisherFactory() );
        Assert.assertNotNull("listenerEventFactory set on builder", builder.getListenerEventFactory() );
        Assert.assertNotNull("generalRequestHandlerFactory set on builder", builder.getGeneralRequestHandlerFactory() );
    }

    @Test
    public void publishingBuilder6Args() {

        log.info("## publishingBuilder6Args");

        List<InetSocketAddress> registries = new ArrayList<>();

        SerializerFactory<String,SimpleData> serializerFactory = new SerializerFactoryDefaultImpl<>();
        ObjectStoreCacheLoaderPersisterFactory<String,SimpleData> cacheLoaderPersisterFactory = new   ObjectStoreCacheLoaderPersisterFactory<>();
        ListenerEventQueueFactory<String, ListenerEvent<String,SimpleData>> listenerEventQueueFactory= new MyEventQueueFactory<>();
        MapFactory<String,SimpleData>  mapFactory = new  MyMapFactory<>();
        CachePublisherFactory<String,SimpleData> cachePublisherFactory = new MockCachePublisherFactory<>();


        PublishingCacheBuilder<String,SimpleData> builder = new PublishingCacheBuilder(registries, serializerFactory, cacheLoaderPersisterFactory, listenerEventQueueFactory, mapFactory, cachePublisherFactory);

        Assert.assertTrue("serializerFactory set on builder", isSameObject(builder.getSerializerFactory(),serializerFactory) );
        Assert.assertTrue("cacheLoaderPersisterFactory set on builder", isSameObject(builder.getCacheLoaderPersisterFactory(),cacheLoaderPersisterFactory ) );
        Assert.assertTrue("mapFactory set on builder", isSameObject(builder.getMapFactory(),mapFactory ) );
        Assert.assertTrue("listenerEventQueueFactory set on builder", isSameObject(builder.getListenerEventQueueFactory(),listenerEventQueueFactory ) );
        Assert.assertTrue("cachePublisherFactory set on builder", isSameObject(builder.getCachePublisherFactory(),cachePublisherFactory ) );
    }

    public void publishingBuilder6ArgsWithNulls() {

        log.info("## publishingBuilder6ArgsWithNulls");

        List<InetSocketAddress> registries = new ArrayList<>();

        SerializerFactory<String,SimpleData> serializerFactory = null;
        ObjectStoreCacheLoaderPersisterFactory<String,SimpleData> cacheLoaderPersisterFactory = null;
        ListenerEventQueueFactory<String, ListenerEvent<String,SimpleData>> listenerEventQueueFactory= null;
        MapFactory<String,SimpleData>  mapFactory = null;
        CachePublisherFactory<String,SimpleData> cachePublisherFactory =null;


        PublishingCacheBuilder<String,SimpleData> builder = new PublishingCacheBuilder(registries, serializerFactory, cacheLoaderPersisterFactory, listenerEventQueueFactory, mapFactory, cachePublisherFactory);

        Assert.assertNotNull("serializerFactory set on builder", builder.getSerializerFactory() );
        Assert.assertNotNull("cacheLoaderPersisterFactory set on builder", builder.getCacheLoaderPersisterFactory() );
        Assert.assertNotNull("mapFactory set on builder", builder.getMapFactory() );
        Assert.assertNotNull("listenerEventQueueFactory set on builder", builder.getListenerEventQueueFactory() );
        Assert.assertNotNull("cachePublisherFactory set on builder", builder.getCachePublisherFactory() );
        Assert.assertNotNull("listenerEventFactory set on builder", builder.getListenerEventFactory() );
        Assert.assertNotNull("generalRequestHandlerFactory set on builder", builder.getGeneralRequestHandlerFactory() );
    }

    @Test
    public void publishingBuilder8Args() {

        log.info("## publishingBuilder8Args");

        List<InetSocketAddress> registries = new ArrayList<>();

        SerializerFactory<String,SimpleData> serializerFactory = new SerializerFactoryDefaultImpl<>();
        ObjectStoreCacheLoaderPersisterFactory<String,SimpleData> cacheLoaderPersisterFactory = new   ObjectStoreCacheLoaderPersisterFactory<>();
        ListenerEventQueueFactory<String, ListenerEvent<String,SimpleData>> listenerEventQueueFactory= new MyEventQueueFactory<>();
        MapFactory<String,SimpleData>  mapFactory = new  MyMapFactory<>();
        CachePublisherFactory<String,SimpleData> cachePublisherFactory = new MockCachePublisherFactory<>();
        ListenerEventFactory<String,SimpleData>  listenerEventFactory = new ListenerEventFactoryDefaultImpl<>();
        GeneralRequestHandlerFactory generalRequestHandlerFactory = new MockGeneralRequestHandlerFactory();
        ThreaderFactory threaderFactory = new MockThreaderFactory();

        PublishingCacheBuilder<String,SimpleData> builder = new PublishingCacheBuilder(registries, serializerFactory, cacheLoaderPersisterFactory,
                                        listenerEventQueueFactory, mapFactory, cachePublisherFactory, listenerEventFactory, threaderFactory, generalRequestHandlerFactory, null);

        Assert.assertTrue("serializerFactory set on builder", isSameObject(builder.getSerializerFactory(),serializerFactory) );
        Assert.assertTrue("cacheLoaderPersisterFactory set on builder", isSameObject(builder.getCacheLoaderPersisterFactory(),cacheLoaderPersisterFactory ) );
        Assert.assertTrue("mapFactory set on builder", isSameObject(builder.getMapFactory(),mapFactory ) );
        Assert.assertTrue("listenerEventQueueFactory set on builder", isSameObject(builder.getListenerEventQueueFactory(),listenerEventQueueFactory ) );
        Assert.assertTrue("cachePublisherFactory set on builder", isSameObject(builder.getCachePublisherFactory(),cachePublisherFactory ) );
        Assert.assertTrue("listenerEventFactory set on builder", isSameObject(builder.getListenerEventFactory(),listenerEventFactory ) );
        Assert.assertTrue("generalRequestHandlerFactory set on builder", isSameObject(builder.getGeneralRequestHandlerFactory(),generalRequestHandlerFactory ) );
        Assert.assertTrue("threaderFactory set on builder", isSameObject(builder.getThreaderFactory(),threaderFactory ) );
    }

    @Test
    public void publishingBuilder8ArgsWithNulls() {

        log.info("## publishingBuilder8ArgsWithNulls");

         // if nulls are passed, default as used

        List<InetSocketAddress> registries = new ArrayList<>();

        SerializerFactory<String,SimpleData> serializerFactory = null;
        ObjectStoreCacheLoaderPersisterFactory<String,SimpleData> cacheLoaderPersisterFactory = null;
        ListenerEventQueueFactory<String, ListenerEvent<String,SimpleData>> listenerEventQueueFactory= null;
        MapFactory<String,SimpleData>  mapFactory = null;
        CachePublisherFactory<String,SimpleData> cachePublisherFactory =null;
        ListenerEventFactory<String,SimpleData>  listenerEventFactory = null;
        GeneralRequestHandlerFactory generalRequestHandlerFactory = null;
        ThreaderFactory threaderFactory = null;

        PublishingCacheBuilder<String,SimpleData> builder = new PublishingCacheBuilder(registries, serializerFactory, cacheLoaderPersisterFactory,
                listenerEventQueueFactory, mapFactory, cachePublisherFactory, listenerEventFactory, threaderFactory, generalRequestHandlerFactory, null);

        Assert.assertNotNull("serializerFactory set on builder", builder.getSerializerFactory() );
        Assert.assertNotNull("cacheLoaderPersisterFactory set on builder", builder.getCacheLoaderPersisterFactory() );
        Assert.assertNotNull("mapFactory set on builder", builder.getMapFactory() );
        Assert.assertNotNull("listenerEventQueueFactory set on builder", builder.getListenerEventQueueFactory() );
        Assert.assertNotNull("cachePublisherFactory set on builder", builder.getCachePublisherFactory() );
        Assert.assertNotNull("listenerEventFactory set on builder", builder.getListenerEventFactory() );
        Assert.assertNotNull("generalRequestHandlerFactory set on builder", builder.getGeneralRequestHandlerFactory() );
        Assert.assertNotNull("threaderFactory set on builder", builder.getThreaderFactory() );
    }

    @Test
    public void subscriptionCacheBuider1Arg() {

        log.info("## subscriptionCacheBuider1Arg");

        List<InetSocketAddress> registries = new ArrayList<>();

        SubscriptionCacheBuilder<String, SimpleData> builder = new SubscriptionCacheBuilder<>(registries);

        Assert.assertNotNull("listenerEventQueueFactory not null", builder.getListenerEventQueueFactory() );
        Assert.assertNotNull("cacheSubscriberFactory not null", builder.getCacheSubscriberFactory() );
        Assert.assertNotNull("mapFactory not null",builder.getMapFactory() );
        Assert.assertNotNull("listenerEventFactory not null", builder.getListenerEventFactory() );
        Assert.assertNotNull("locator not null", builder.getLocator() );
    }

    @Test
    public void subscriptionCacheBuider3Arg() {

        log.info("## subscriptionCacheBuider3Arg");

        List<InetSocketAddress> registries = new ArrayList<>();

        ListenerEventQueueFactory<String, ListenerEvent<String,SimpleData>> listenerEventQueueFactory= new MyEventQueueFactory<>();
        MockCacheSubscriberFactory<String,SimpleData> cacheSubscriberFactory = new MockCacheSubscriberFactory<>() ;
        MapFactory<String,SimpleData>  mapFactory = new  MyMapFactory<>();

        SubscriptionCacheBuilder<String, SimpleData> builder = new SubscriptionCacheBuilder<>(registries, listenerEventQueueFactory, cacheSubscriberFactory, mapFactory);

        Assert.assertTrue("listenerEventQueueFactory set on builder", isSameObject(builder.getListenerEventQueueFactory(),listenerEventQueueFactory) );
        Assert.assertTrue("cacheSubscriberFactory set on builder", isSameObject(builder.getCacheSubscriberFactory(),cacheSubscriberFactory) );
        Assert.assertTrue("mapFactory set on builder", isSameObject(builder.getMapFactory(),mapFactory) );
    }

    @Test
    public void subscriptionCacheBuider3ArgNulls() {

        log.info("## subscriptionCacheBuider3ArgNulls");

        List<InetSocketAddress> registries = new ArrayList<>();

        ListenerEventQueueFactory<String, ListenerEvent<String,SimpleData>> listenerEventQueueFactory= null;
        MockCacheSubscriberFactory<String,SimpleData> cacheSubscriberFactory = null;
        MapFactory<String,SimpleData>  mapFactory = null;

        SubscriptionCacheBuilder<String, SimpleData> builder = new SubscriptionCacheBuilder<>(registries, listenerEventQueueFactory, cacheSubscriberFactory, mapFactory);

        Assert.assertNotNull("listenerEventQueueFactory not null", builder.getListenerEventQueueFactory() );
        Assert.assertNotNull("cacheSubscriberFactory not null", builder.getCacheSubscriberFactory() );
        Assert.assertNotNull("mapFactory not null",builder.getMapFactory() );
        Assert.assertNotNull("listenerEventFactory not null", builder.getListenerEventFactory() );
        Assert.assertNotNull("locator not null", builder.getLocator() );
    }


    @Test
    public void subscriptionCacheBuider3ArgLocator() {

        log.info("## subscriptionCacheBuider3ArgLocator");

        List<InetSocketAddress> registries = new ArrayList<>();
        CacheLocatorImpl  locator = new CacheLocatorImpl(registries);

        ListenerEventQueueFactory<String, ListenerEvent<String,SimpleData>> listenerEventQueueFactory= new MyEventQueueFactory<>();
        MockCacheSubscriberFactory<String,SimpleData> cacheSubscriberFactory = new MockCacheSubscriberFactory<>() ;
        MapFactory<String,SimpleData>  mapFactory = new  MyMapFactory<>();

        SubscriptionCacheBuilder<String, SimpleData> builder = new SubscriptionCacheBuilder<>(locator, listenerEventQueueFactory, cacheSubscriberFactory, mapFactory);

        Assert.assertTrue("listenerEventQueueFactory set on builder", isSameObject(builder.getListenerEventQueueFactory(),listenerEventQueueFactory) );
        Assert.assertTrue("cacheSubscriberFactory set on builder", isSameObject(builder.getCacheSubscriberFactory(),cacheSubscriberFactory) );
        Assert.assertTrue("mapFactory set on builder", isSameObject(builder.getMapFactory(),mapFactory) );
        Assert.assertTrue("locator set on builder", isSameObject(builder.getLocator(),locator) );
    }

    @Test
    public void subscriptionCacheBuider4Arg() {

        log.info("## subscriptionCacheBuider4Arg");

        List<InetSocketAddress> registries = new ArrayList<>();

        ListenerEventQueueFactory<String, ListenerEvent<String,SimpleData>> listenerEventQueueFactory= new MyEventQueueFactory<>();
        MockCacheSubscriberFactory<String,SimpleData> cacheSubscriberFactory = new MockCacheSubscriberFactory<>() ;
        MapFactory<String,SimpleData>  mapFactory = new  MyMapFactory<>();
        ListenerEventFactory<String,SimpleData>  listenerEventFactory = new ListenerEventFactoryDefaultImpl<>();

        SubscriptionCacheBuilder<String, SimpleData> builder = new SubscriptionCacheBuilder<>(registries, listenerEventQueueFactory, cacheSubscriberFactory, mapFactory,listenerEventFactory, null);

        Assert.assertTrue("listenerEventQueueFactory set on builder", isSameObject(builder.getListenerEventQueueFactory(),listenerEventQueueFactory) );
        Assert.assertTrue("cacheSubscriberFactory set on builder", isSameObject(builder.getCacheSubscriberFactory(),cacheSubscriberFactory) );
        Assert.assertTrue("mapFactory set on builder", isSameObject(builder.getMapFactory(),mapFactory) );
        Assert.assertTrue("listenerEventFactory set on builder", isSameObject(builder.getListenerEventFactory(),listenerEventFactory) );
    }


    @Test
    public void subscriptionCacheBuider4ArgNulls() {

        log.info("## subscriptionCacheBuider4ArgNulls");

        List<InetSocketAddress> registries = new ArrayList<>();

        ListenerEventQueueFactory<String, ListenerEvent<String,SimpleData>> listenerEventQueueFactory= null;
        MockCacheSubscriberFactory<String,SimpleData> cacheSubscriberFactory = null;
        MapFactory<String,SimpleData>  mapFactory = null;
        ListenerEventFactory<String,SimpleData>  listenerEventFactory = null;

        SubscriptionCacheBuilder<String, SimpleData> builder = new SubscriptionCacheBuilder<>(registries, listenerEventQueueFactory, cacheSubscriberFactory, mapFactory,listenerEventFactory, null);

        Assert.assertNotNull("listenerEventQueueFactory not null", builder.getListenerEventQueueFactory() );
        Assert.assertNotNull("cacheSubscriberFactory not null", builder.getCacheSubscriberFactory() );
        Assert.assertNotNull("mapFactory not null",builder.getMapFactory() );
        Assert.assertNotNull("listenerEventFactory not null", builder.getListenerEventFactory() );
        Assert.assertNotNull("locator not null", builder.getLocator() );
    }

    static boolean isSameObject(Object o1, Object o2) {
        return System.identityHashCode(o1) == System.identityHashCode(o2);
    }
}
