package uk.co.octatec.scdds.cache.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.cache.Cache;
import uk.co.octatec.scdds.odb.ObjectDataStore;

import java.io.IOException;

/**
 * Created by Jeromy Drake on 02/05/2016.
 */
public class ObjectDataStoreCacheLoader<K,T> implements CacheLoader<K,T> {

    private final static Logger log = LoggerFactory.getLogger(ObjectDataStoreEntryPersisterImpl.class);

    private ObjectDataStore<K,T> db = new ObjectDataStore<>() ;

    private String fileName;

    public ObjectDataStoreCacheLoader(String cacheName, String suffix) {
        fileName =  cacheName+"_"+suffix+".dat";
    }

    @Override
    public void open() {
        try {
            db.openForRead(fileName);
        }
        catch( IOException e) {
            log.error("exception while opening data-store fileName=[{}}", fileName, e.getMessage()) ;
            throw new RuntimeException("exception while opening data-store fileName", e);
        }
    }

    @Override
    public void close() {
        db.close();
    }

    @Override
    public void loadCache(Cache<K, T> cache) {
        // using the normal PublishingCacheBuilder mechanism fro creating a cache, this method
        // is called before any listeners are added
        try {
            log.info("loading cache, current size [{}]", cache.size());
            ObjectDataStore.Entry<K, T> entry;
            while( (entry = db.readEntry()) != null ) {
                T value = entry.getValue();
                if( value == null ) {
                    cache.remove(entry.getKey());
                }
                else {
                    cache.put(entry.getKey(), value);
                }
            }
            log.info("finished loading cache, size [{}]", cache.size());
        }
        catch( IOException e) {
            log.error("exception while reading from objecyt data store", e) ;
            throw new RuntimeException("exception while storing data to object store", e);
        }
    }

    public String getFileName() {
        return fileName;
    }
}
