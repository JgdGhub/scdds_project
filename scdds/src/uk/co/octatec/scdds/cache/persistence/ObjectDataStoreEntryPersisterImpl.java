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
import uk.co.octatec.scdds.odb.ObjectDataStore;

import java.io.IOException;

/**
 * Created by Jeromy Drake on 02/05/2016.
 */
public class ObjectDataStoreEntryPersisterImpl<K,T> implements EntryPersister<K,T> {
    private final static Logger log = LoggerFactory.getLogger(ObjectDataStoreEntryPersisterImpl.class);

    private ObjectDataStore<K,T> db = new ObjectDataStore<>() ;

    private String fileName;

    public ObjectDataStoreEntryPersisterImpl(String cacheName, String suffix) {
        fileName =  cacheName+"_"+suffix+".dat";
    }

    @Override
    public void open() {
        try {
            db.openForWrite(fileName);
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
    public void store(K key, T value) {
        try {
            db.store(key, value);
        }
        catch( IOException e ) {
            log.error("exception while storing data key=[{}] value=[{}] exceptoion=[{}}", key, value, e) ;
            throw new RuntimeException("exception while storing data to object store", e);
        }

    }

    @Override
    public void markDeleted(K key) {
        try {
            db.markDeleted(key);
        }
        catch( IOException e ) {
            log.error("exception while marking data as deleted key=[{}] exceptoion=[{}}", key, e) ;
            throw new RuntimeException("exception while storing data to object store", e);
        }
    }

    public String getFileName() {
        return fileName;
    }
}
