package uk.co.octatec.scdds.cache;

import uk.co.octatec.scdds.cache.subscribe.ImmutableCache;

import java.util.Map;

/**
 * Created by Jeromy Drake on 30/04/2016.
 */
public interface CacheListener<K,T>{
    void onInitialUpdate(ImmutableCache<K,T> initialState);
    void onUpdate(K key, T value);
    void onRemoved(K key, T value);
    void onDataStale();
    void onActive();
    void onFatalError(String errorText);
}
