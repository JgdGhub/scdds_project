package uk.co.octatec.scdds.instrumentation;
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
import uk.co.octatec.scdds.cache.subscribe.*;
import uk.co.octatec.scdds.net.registry.CacheLocator;
import uk.co.octatec.scdds.net.socket.ClientConnector;

/**
 * Created by Jeromy Drake on 12/05/2016.
 */
public class InstrumentedCacheSubscriberFactory<K,T> implements CacheSubscriberFactory<K,T> {

    static final int MAX_RETRIES = 0;

    private final InstrumentedClientConnector instrumentedConnector = new  InstrumentedClientConnector();

    private CacheSubscriberImpl<K,T>  cacheSubscriberImpl;

    public InstrumentedClientConnector getInstrumentedConnector() {
        return instrumentedConnector;
    }

    public CacheSubscriberImpl<K, T> getCacheSubscriberImpl() {
        return cacheSubscriberImpl;
    }

      @Override
    public CacheSubscriber<K, T> create(CacheImplClientSide<K, T> cache, CacheLocator locator, CacheFilter<K, T> filter, String filterArg, InitialUpdateReaderFactory<K, T> initialUpdateReaderFactory) {
        cacheSubscriberImpl = new CacheSubscriberImpl<K,T>(instrumentedConnector, cache, locator, filter, filterArg, MAX_RETRIES, initialUpdateReaderFactory);
        return cacheSubscriberImpl;
    }
}
