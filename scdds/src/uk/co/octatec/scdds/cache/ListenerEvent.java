package uk.co.octatec.scdds.cache;

import uk.co.octatec.scdds.cache.publish.ServerSideCacheSubscription;
import uk.co.octatec.scdds.queue.Event;

/**
 * Created by Jeromy Drake on 30/04/2016.
 */
public class ListenerEvent<K,T> implements Event<K> {

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
