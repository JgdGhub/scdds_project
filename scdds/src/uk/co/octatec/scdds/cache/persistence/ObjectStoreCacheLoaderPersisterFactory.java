package uk.co.octatec.scdds.cache.persistence;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Created by Jeromy Drake on 02/05/2016.
 */
public class ObjectStoreCacheLoaderPersisterFactory<K,T> implements CacheLoaderPersisterFactory<K,T> {
    @Override
    public CacheLoader<K, T> createCacheLoader(String cacheName) {
        DateFormat df = new SimpleDateFormat("yyyymmdd");
        String suffix = df.format(System.currentTimeMillis());
        return new ObjectDataStoreCacheLoader<>(cacheName, suffix);
    }
    @Override
    public EntryPersister<K, T> createEntryPersister(String cacheName) {
        DateFormat df = new SimpleDateFormat("yyyymmdd");
        String suffix = df.format(System.currentTimeMillis());
        return new ObjectDataStoreEntryPersisterImpl<>(cacheName, suffix);
    }
}
