package uk.co.octatec.scdds.cache.persistence;

/**
 * Created by Jeromy Drake on 02/05/2016.
 */
public final class  NoOpCacheLoaderPersisterFactory <K,T> implements CacheLoaderPersisterFactory <K,T> {
    @Override
    public CacheLoader<K, T> createCacheLoader(String cachename) {
        return new NoOpCacheLoader<>();
    }

    @Override
    public EntryPersister<K, T> createEntryPersister(String cacheName) {
        return new NoOpEntryPersister<>();
    }
}
