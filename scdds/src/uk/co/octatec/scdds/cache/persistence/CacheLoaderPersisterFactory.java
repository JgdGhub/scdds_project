package uk.co.octatec.scdds.cache.persistence;

/**
 * Created by Jeromy Drake on 02/05/2016.
 */
public interface CacheLoaderPersisterFactory<K,T> {
    CacheLoader<K,T> createCacheLoader(String cachename);
    EntryPersister<K,T> createEntryPersister(String cacheName);
}
