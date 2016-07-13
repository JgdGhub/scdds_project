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
import uk.co.octatec.scdds.cache.publish.ServerSideCacheSubscription;
import uk.co.octatec.scdds.queue.Event;

/**
 * Created by Jeromy Drake on 30/04/2016.
 */
public class ListenerEvent<K,T extends ImmutableEntry> implements Event<K> {

    enum Action {active, update, remove, initialUpdate, stale, publisherInitialUpdate, fatal}

    public K key;
    public T value;
    Action action;
    boolean canBeBatched;
    long timestamp = System.nanoTime();

    CacheListener<K, T> listenerForInitialUpdate; // used for a newly attached listener

    ServerSideCacheSubscription<K,T> cacheSubsciption; // just used for the Initial-Upddate of a new remote-subscriber

    @Override
    final public K getKey() {
        return key;
    }

    @Override
    final public boolean canBeBatched() {
        return canBeBatched;
    }

    @Override
    public String toString() {
        return "ListenerEvent{" +
                action + ":" +key+":" + value +
                '}';
    }
}
