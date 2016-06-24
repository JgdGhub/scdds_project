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

/**
 * Created by Jeromy Drake on 02/05/2016.
 *
 * If a CacheLoader is required but not an EntryPersister, or vice versa, a CacheLoaderPersisterFactory can
 * return null for the interface not required (or it could delegate to the NoOpCacheLoaderPersisterFactory
 */
public interface CacheLoaderPersisterFactory<K,T extends ImmutableEntry> {
    CacheLoader<K,T> createCacheLoader(String cachename);
    EntryPersister<K,T> createEntryPersister(String cacheName);
}
