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
import uk.co.octatec.scdds.cache.ImmutableEntry;
import uk.co.octatec.scdds.net.serialize.Serializer;
import uk.co.octatec.scdds.net.socket.BlockIO;
import uk.co.octatec.scdds.net.socket.InitialUpdateReader;

import java.util.Map;

/**
 * Created by Jeromy Drake on 20/05/2016.
 *
 * When a streams mode subscription is made, there is no local copy of the cache and the initial update received by the
 * listener is always empty. However, the server still sends an initial update, but in the client side this
 * just triggers a sequence of onUpdate() calls. This class is used in the client to support that difference in
 * behaviour.
 */
public class InitialUpdateReaderForStreams<K,T extends ImmutableEntry> extends InitialUpdateReader<K,T>  {

    // a client-side cache-subscription in 'streaming mode' gets an 'empty' initial update
    // and gets all updates through the onUpdate() callback, so we just add the values
    // to the cache directly (remember no data is store in a 'stream-based-cache' so the put()
    // methiod triggers an onUpdate call, but nothing more)

    private final CacheImplClientSide<K, T> streamingCache;

    public InitialUpdateReaderForStreams(BlockIO bIO, Serializer<K,T> serializer, CacheImplClientSide<K, T> streamingCache) {
        super(bIO, serializer);
        this.streamingCache = streamingCache;
    }

    @Override
    protected void applyUpdate(K key, T value, Map<K,T> data) {
        streamingCache.put(key, value);
    }
}
