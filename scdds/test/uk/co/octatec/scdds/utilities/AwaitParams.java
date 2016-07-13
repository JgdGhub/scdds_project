package uk.co.octatec.scdds.utilities;
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
import uk.co.octatec.scdds.cache.subscribe.CacheSubscriberImpl;
import uk.co.octatec.scdds.cache.subscribe.ImmutableCache;

/**
 * Created by Jeromy Drake on 10/05/2016.
 */
public class AwaitParams {
    public static final int AWAIT_SLEEP_TIME = 20;
    public static final int AWAIT_LOOP_COUNT = 100;

    public static<K,T extends ImmutableEntry>  void awaitCacheSizeGte(ImmutableCache<K,T> cache, int n) throws  InterruptedException{
        for(int i=0; i<AWAIT_LOOP_COUNT; i++) {
            if( cache.size() >= n ) {
                break;
            }
            Thread.sleep(AWAIT_SLEEP_TIME);
        }
    }

    public static<K,T extends ImmutableEntry>  void awaitCacheStaleValue(ImmutableCache<K,T> cache, boolean requiredValue) throws  InterruptedException{
        for(int i=0; i<AWAIT_LOOP_COUNT; i++) {
            if( cache.isStale() == requiredValue) {
                break;
            }
            Thread.sleep(AWAIT_SLEEP_TIME);
        }
    }
}
