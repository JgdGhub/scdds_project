package uk.co.octatec.scdds.cache.subscribe;

import uk.co.octatec.scdds.cache.CacheListener;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Created by Jeromy Drake on 04/05/2016.
 */

// the cache-interface that is passed to subscribers - subscribers
// should not try and put items into the cache

public interface ImmutableCache<K,T> {
    T get(K key);
    Set<K> keySet();
    int size();
    String getName();
    void addListener(CacheListener<K,T> listener);
    void removeListener(CacheListener<K,T> listener);
    Collection<T> values();
    Set<Map.Entry<K,T>> entrySet();
    boolean isStale();
}
