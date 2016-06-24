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
import java.util.Date;

/**
 * Created by Jeromy Drake on 20/06/2016.
 *
 * This implementation stores only the current value, for an implementation that produces a history of how the
 * values changed see ObjectStoreCacheLoaderPersisterFactory
 */
public class KeyBasedCacheLoaderPersisterFactory<K,T extends ImmutableEntry> implements CacheLoaderPersisterFactory<K,T> {
    @Override
    public CacheLoader<K, T> createCacheLoader(String cacheName) {
        DateFormat df = new SimpleDateFormat("yyyyMMdd");
        return new KeyBasedCacheLoader<K,T>(cacheName, df.format(new Date()));
    }

    @Override
    public EntryPersister<K, T> createEntryPersister(String cacheName) {
        DateFormat df = new SimpleDateFormat("yyyyMMdd");
        return new KeyBasedEntryPersister<K,T>(cacheName, df.format(new Date()));
    }
}
