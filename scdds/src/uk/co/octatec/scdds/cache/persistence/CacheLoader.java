package uk.co.octatec.scdds.cache.persistence;

import uk.co.octatec.scdds.cache.Cache;

/**
 * Created by Jeromy Drake on 02/05/2016.
 */
public interface CacheLoader<K,T> {
    void open();
    void loadCache(Cache<K,T> cache);
    void close();
}
