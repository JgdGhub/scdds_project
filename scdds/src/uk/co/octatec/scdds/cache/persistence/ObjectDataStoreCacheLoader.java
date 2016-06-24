package uk.co.octatec.scdds.cache.persistence;
/*
  SC/DDS - simple cached data distribution service

  Copyright 2016 by Jeromy Drake

  This program is free software; you may redistribute and/or modify it under
  the terms of the GNU General Public License Version 2 as published by the
  Free Software Foundation.

  This program is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY, without even the implied warranty of MERCHANTABILITY
  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
  for complete details.
*/
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.cache.Cache;
import uk.co.octatec.scdds.cache.ImmutableEntry;
import uk.co.octatec.scdds.odb.ObjectDataStore;

import java.io.File;
import java.io.IOException;

/**
 * Created by Jeromy Drake on 02/05/2016.
 *
 * Read cache-entries saved by the ObjectDataStoreEntryPersister
 */
public class ObjectDataStoreCacheLoader<K,T extends ImmutableEntry> implements CacheLoader<K,T> {

    private final static Logger log = LoggerFactory.getLogger(ObjectDataStoreEntryPersister.class);

    private ObjectDataStore<K,T> db = new ObjectDataStore<>() ;

    private String fileName;

    public ObjectDataStoreCacheLoader(String cacheName, String suffix) {
        fileName =  cacheName+"_"+suffix+".dat";
        log.info("create ObjectDataStoreCacheLoader fileName=[{}]", fileName);
    }

    private boolean dataStoreExists() {
        // the data-store won't exists the first time
        File file = new File(fileName);
        return file.exists();
    }

    @Override
    public void open() {
        try {

            if( dataStoreExists() ) {
                log.info("ObjectDataStoreCacheLoader open [{}]", fileName);
                db.openForRead(fileName);
            }
            else {
                log.info("ObjectDataStoreCacheLoader open [{}]  files does not exist, not opening for read", fileName);
            }
        }
        catch( IOException e) {
            log.error("exception while opening data-store fileName=[{}}", fileName, e.getMessage()) ;
            throw new RuntimeException("exception while opening data-store fileName", e);
        }
    }

    @Override
    public void close() {
        if( db.isOpenForRead() )   {
            log.info("ObjectDataStoreCacheLoader close [{}]", fileName);
            db.close();
        }
        else {
            log.info("ObjectDataStoreCacheLoader close - not open for reade [{}]", fileName);
        }
    }

    @Override
    public void loadCache(Cache<K, T> cache) {

        if( !db.isOpenForRead() ) {
            log.info("ObjectDataStoreCacheLoader loadCache - not open for reade [{}]", fileName);
            return;
        }

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
