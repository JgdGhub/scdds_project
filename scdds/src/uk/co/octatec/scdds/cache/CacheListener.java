package uk.co.octatec.scdds.cache;
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
import uk.co.octatec.scdds.cache.subscribe.ImmutableCache;

import java.util.Map;

/**
 * Created by Jeromy Drake on 30/04/2016.
 */
public interface CacheListener<K,T extends ImmutableEntry>{
    void onInitialUpdate(ImmutableCache<K,T> initialState);
    void onUpdate(K key, T value);
    void onRemoved(K key, T value);
    void onDataStale();
    void onActive();
    void onFatalError(String errorText);
}
