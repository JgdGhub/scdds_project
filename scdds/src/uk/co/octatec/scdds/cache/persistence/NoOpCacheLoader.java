package uk.co.octatec.scdds.cache.persistence;

import uk.co.octatec.scdds.cache.Cache;

/**
 * Created by Jeromy Drake on 02/05/2016.
 */
public final class NoOpCacheLoader<K,T> implements CacheLoader<K,T> {
    @Override
    public void open() {

    }

    @Override
    public void loadCache(Cache<K, T> cache) {

    }

    @Override
    public void close() {

    }
}
