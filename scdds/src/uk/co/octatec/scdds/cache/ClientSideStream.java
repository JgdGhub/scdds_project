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
import uk.co.octatec.scdds.cache.ListenerEvent;
import uk.co.octatec.scdds.cache.ListenerEventFactory;
import uk.co.octatec.scdds.cache.ListenerEventQueueFactory;
import uk.co.octatec.scdds.cache.publish.MapFactory;
import uk.co.octatec.scdds.cache.subscribe.CacheImplClientSide;
import uk.co.octatec.scdds.cache.subscribe.CacheSubscriberImpl;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by Jeromy Drake on 20/05/2016.
 *
 * This class is used internal and takes the place of the local copy of the cache
 * when a client subscribes for streaming updates (as opposed to updates that are
 * applied to a local copy of the cache)
 */
class ClientSideStream<K,T extends ImmutableEntry> extends CacheImplClientSide<K,T> { //  this is only used internally and not visible to clients

    // this gives the client the option to subscribe to a remote cache, but not store the data locally,
    // i.e. the client will get the CacheListener events but there will be no local copy of the cache,
    // the client must make full use of the event when it is fired, the client won't be able to
    // query the value (unless it stores it itself, in which case it should probably use
    // the cache subscription)

    private final static Logger log = LoggerFactory.getLogger(CacheSubscriberImpl.class);

    private static class NoOpConcurrentMap<K,T> implements ConcurrentMap<K, T> {
        @Override
        public T putIfAbsent(K key, T value) {
            return null;
        }
        @Override
        public boolean remove(Object key, Object value) {
            return false;
        }
        @Override
        public boolean replace(K key, T oldValue, T newValue) {
            return false;
        }
        @Override
        public T replace(K key, T value) {
            return null;
        }
        @Override
        public int size() {
            return 0;
        }
        @Override
        public boolean isEmpty() {
            return false;
        }
        @Override
        public boolean containsKey(Object key) {
            return false;
        }
        @Override
        public boolean containsValue(Object value) {
            return false;
        }
        @Override
        public T get(Object key) {
            return null;
        }
        @Override
        public T put(K key, T value) {
            return null;
        }
        @Override
        public T remove(Object key) {
            return null;
        }
        @Override
        public void putAll(Map<? extends K, ? extends T> m) {
        }
        @Override
        public void clear() {
        }
        @Override
        public Set<K> keySet() {
            return new HashSet();
        }
        @Override
        public Collection<T> values() {
            return new ArrayList();
        }
        @Override
        public Set<Entry<K, T>> entrySet() {
            return new HashSet();
        }
    }

    private static class NoOpMapFactory<K,T> implements MapFactory<K,T> {
        @Override
        public ConcurrentMap<K, T> create() {
            return new  NoOpConcurrentMap<K,T>();
        }
    }

    public ClientSideStream(String name, ListenerEventQueueFactory<K, ListenerEvent<K,T>> queueFactory) {
        super(name, queueFactory, new NoOpMapFactory<K,T>());
    }

    public ClientSideStream(String name, ListenerEventQueueFactory<K, ListenerEvent<K,T>> queueFactory, ListenerEventFactory<K,T> listenerEventFactory) {
        super(name, queueFactory, new NoOpMapFactory<K,T>(), listenerEventFactory);
    }

    public ClientSideStream(String name) {
        super(name);
    }

    public ClientSideStream() {
        super();
    }

    @Override
    public T put(K key, T value) {

        // fire events but don't put anything in the local map

        ListenerEvent<K,T> event = listenerEventFactory.create();
        event.action = ListenerEvent.Action.update;
        event.key = key;
        event.value = value;
        event.canBeBatched = true;
        try {
            listenerEventQueue.put(event);
        }
        catch(InterruptedException e){
            log.error("listeners not notified due to InterruptedException adding CacheListener event to queue key=[{}] value=[{}]", key, value);
            log.error("got InterruptedException adding CacheListener event to queue", e);
        }
        return null;
    }

    @Override
    public T remove(K key) {
        ListenerEvent<K,T> event = new ListenerEvent<>();
        event.action = ListenerEvent.Action.remove;
        event.key = key;
        try {
            listenerEventQueue.put(event);
        }
        catch(InterruptedException e){
            log.error("listeners not notified due to InterruptedException adding CacheListener event to queue key=[{}]", key);
            log.error("got InterruptedException adding CacheListener event to queue", e);
        }
        return null;
    }


}
