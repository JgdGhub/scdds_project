package uk.co.octatec.scdds.cache;

import uk.co.octatec.scdds.cache.subscribe.ImmutableCache;

/**
 * Created by Jeromy Drake on 30/04/2016.
 */
public interface Cache<K,T> extends ImmutableCache<K,T> {
    void notifyStale();
    void notifyUnStale();

    /**
     * Put an object into the cache. It will be published out to any subscribers over the network and any local
     * listeners will be notified. Once put in the cache, the object should not be changed, cached objects
     * should be immutable - this is because the object is published and passed to listeners in a separate thread,
     * if the object was changed whilst that was happening, listeners would see an inconsistent and possibly corrupt
     * version of the object.
     * @param key
     * @param value
     * @return The previous value held in the cache
     */
    T put(K key, T value);
    T remove(K key);
    void dispose();
}

