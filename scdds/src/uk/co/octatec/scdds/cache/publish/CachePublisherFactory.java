package uk.co.octatec.scdds.cache.publish;
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
import uk.co.octatec.scdds.cache.Cache;
import uk.co.octatec.scdds.cache.CacheImpl;
import uk.co.octatec.scdds.cache.CacheListener;
import uk.co.octatec.scdds.cache.ImmutableEntry;
import uk.co.octatec.scdds.cache.publish.threading.Threader;
import uk.co.octatec.scdds.net.serialize.SerializerFactory;

/**
 * Created by Jeromy Drake on 03/05/2016.
 */
public interface CachePublisherFactory<K,T extends ImmutableEntry>  {
    CachePublisher<K,T> create(CacheImpl<K,T> cache, SerializerFactory<K, T> serializerFactory, GeneralRequestHandler generalRequestHandler, Threader threader);
}
