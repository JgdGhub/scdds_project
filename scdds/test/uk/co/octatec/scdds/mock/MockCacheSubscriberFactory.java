package uk.co.octatec.scdds.mock;
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
import uk.co.octatec.scdds.cache.publish.CacheFilter;
import uk.co.octatec.scdds.cache.subscribe.CacheImplClientSide;
import uk.co.octatec.scdds.cache.subscribe.CacheSubscriber;
import uk.co.octatec.scdds.cache.subscribe.CacheSubscriberFactory;
import uk.co.octatec.scdds.cache.subscribe.InitialUpdateReaderFactory;
import uk.co.octatec.scdds.net.registry.CacheLocator;

/**
 * Created by Jeromy Drake on 08/05/16
 */
public class MockCacheSubscriberFactory<K,T> implements CacheSubscriberFactory<K,T> {
    @Override
    public CacheSubscriber<K, T> create(CacheImplClientSide<K, T> cache, CacheLocator locator, CacheFilter<K, T> filter, String filterArg, InitialUpdateReaderFactory<K, T> initialUpdateReaderFactory) {
        return new MockCacheSubscriber<K,T>();
    }
}
