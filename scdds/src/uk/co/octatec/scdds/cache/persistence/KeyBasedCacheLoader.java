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
 * Created by Jeromy Drake on 20/06/2016.
 *
 * Read entries created by the KeyBasedEntryPersister
 */
public class KeyBasedCacheLoader <K,T extends ImmutableEntry> implements CacheLoader<K,T> {

    private final static Logger log = LoggerFactory.getLogger(KeyBasedCacheLoader.class);

    private final String dataDir;

    public KeyBasedCacheLoader(String cacheName, String suffix) {
        this.dataDir = cacheName+"_"+suffix+".dir";
        log.info("initialized dataDir [{}]", dataDir);
    }

    @Override
    public void open() {
        File dir = new File(dataDir);
        if (dir.exists() && dir.isDirectory()) {
            log.info("using data dir [{}] in [{}]", dataDir, System.getProperty("user.dir"));
            return;
        }
        if (dir.exists() && !dir.isDirectory()) {
            log.error("name exists but is not a directory [{}] cwd=[{}]", dataDir, System.getProperty("user.dir"));
            throw new RuntimeException("name exists but is not a directory ["+dataDir+"]");
        }
        log.info("create data dir [{}] in [{}]", dataDir, System.getProperty("user.dir"));
        if( !dir.mkdirs() ) {
            log.error("failed to make directory [{}] cwd=[{}]", dataDir, System.getProperty("user.dir"));
            throw new RuntimeException("failed to make directory directory ["+dataDir+"]");
        }
    }

    @Override
    public void loadCache(Cache<K, T> cache) {
        try {
            log.info("loading cache from [{}] in [{}], current size [{}]", dataDir, System.getProperty("user.dir"), cache.size());
            ObjectDataStore<K,T> db = new ObjectDataStore<K,T>();
            File dir = new File(dataDir);
            File[] files = dir.listFiles();
            for (File file : files) {
                if( file.getName().endsWith(KeyBasedEntryPersister.FILE_SUFFIX)) {
                    db.openForRead(file.getPath());
                    ObjectDataStore.Entry<K, T> entry = db.readEntry();
                    if( entry != null ) {
                        cache.put(entry.getKey(), entry.getValue());
                    }
                    db.close();
                }
            }
            log.info("finished loading cache, size [{}]", cache.size());
        }
        catch( IOException e) {
            log.error("exception while reading from objecyt data store", e) ;
            throw new RuntimeException("exception while storing data to object store", e);
        }
    }

    @Override
    public void close() {
        // do nothing
    }

    public String getDataDir() {
        return dataDir;
    }
}
