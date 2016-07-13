package uk.co.octatec.scdds.cache.subscribe;
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
import uk.co.octatec.scdds.cache.CacheListener;
import uk.co.octatec.scdds.cache.ImmutableEntry;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Created by Jeromy Drake on 04/05/2016.
 *
 * The cache-interface that is returned to subscribers - subscribers
 * should not try and put items into the cache, as the cache contents
 * are controlled by the remote publisher.
 */

public interface ImmutableCache<K,T extends ImmutableEntry> {
    T get(K key);
    Set<K> keySet();
    int size();
    String getName();
    void addListener(CacheListener<K,T> listener);
    void removeListener(CacheListener<K,T> listener);
    Collection<T> values();
    Set<Map.Entry<K,T>> entrySet();
    boolean isStale();
}
