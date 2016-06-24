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
import uk.co.octatec.scdds.cache.ImmutableEntry;
import uk.co.octatec.scdds.odb.ObjectDataStore;

import java.io.File;
import java.io.IOException;

/**
 * Created by Jeromy Drake on 20/06/2016.
 *
 * Save an cache-entry to a single file with a name based on the key, in this way old entries are overwritten with
 * new entries
 */
public class KeyBasedEntryPersister<K,T extends ImmutableEntry> implements EntryPersister<K,T>  {

    public static final String FILE_SUFFIX = ".key.dat";

    private final static Logger log = LoggerFactory.getLogger(KeyBasedEntryPersister.class);

    private final String dataDir;

    ObjectDataStore<K,T> db = new ObjectDataStore<K,T>();

    public KeyBasedEntryPersister(String cacheName, String suffix) {
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
            log.error("failed to make data-directory [{}] cwd=[{}]", dataDir, System.getProperty("user.dir"));
            throw new RuntimeException("failed to make data-directory directory ["+dataDir+"]");
        }
    }

    @Override
    public void store(K key, T value) {
        String path =  dataDir + "/"+key.toString()+FILE_SUFFIX;
        try {
            db.openForWrite(path);
            db.store(key, value);
            db.close();
        }
        catch( IOException x) {
            log.error("store failed key=[{}] path=[{}]", key, path, x);
            throw new RuntimeException("store failed", x);
        }

    }

    @Override
    public void markDeleted(K key) {
        String path =  dataDir + "/"+key.toString()+FILE_SUFFIX;
        File file = new File(path);
        if( !file.exists()) {
            log.warn("attempt to delete a file that does not exist [{}]", path);
        }
        if( !file.delete() ) {
            log.error("delete failed key=[{}] path=[{}]", key, path);
            throw new RuntimeException("delete failed path=["+path+"]");
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
