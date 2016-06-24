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
import uk.co.octatec.scdds.cache.ImmutableEntry;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Created by Jeromy Drake on 02/05/2016.
 *
 * This implementation produces a history of how the values changed in the data-store, which may of may not be what you want,
 * for an implementatiomn that stores only the current value, see KeyBasedCacheLoaderPersisterFactory
 */
public class ObjectStoreCacheLoaderPersisterFactory<K,T extends ImmutableEntry> implements CacheLoaderPersisterFactory<K,T> {
    @Override
    public CacheLoader<K, T> createCacheLoader(String cacheName) {
        DateFormat df = new SimpleDateFormat("yyyyMMdd");
        String suffix = df.format(System.currentTimeMillis());
        return new ObjectDataStoreCacheLoader<>(cacheName, suffix);
    }
    @Override
    public EntryPersister<K, T> createEntryPersister(String cacheName) {
        DateFormat df = new SimpleDateFormat("yyyyMMdd");
        String suffix = df.format(System.currentTimeMillis());
        return new ObjectDataStoreEntryPersister<>(cacheName, suffix);
    }
}
