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
import uk.co.octatec.scdds.cache.CacheImpl;
import uk.co.octatec.scdds.cache.ImmutableEntry;
import uk.co.octatec.scdds.cache.publish.CachePublisher;
import uk.co.octatec.scdds.cache.publish.CachePublisherFactory;
import uk.co.octatec.scdds.cache.publish.GeneralRequestHandler;
import uk.co.octatec.scdds.cache.publish.threading.Threader;
import uk.co.octatec.scdds.net.serialize.SerializerFactory;

/**
 * Created by Jeromy Drake on 07/05/16
 */
public class MockCachePublisherFactory<K,T extends ImmutableEntry> implements CachePublisherFactory<K,T> {
    @Override
    public CachePublisher<K, T> create(CacheImpl<K, T> cache, SerializerFactory<K, T> serializerFactory, GeneralRequestHandler generalRequestHandler, Threader threader) {
        return new MockCachePublisher<K,T>(generalRequestHandler);
    }
}
