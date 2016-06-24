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

/**
 * Created by Jeromy Drake on 20/05/2016.
 */
public class InitialUpdateReaderFactoryForStreamsImpl<K,T extends ImmutableEntry> implements InitialUpdateReaderFactory<K,T>  {
    @Override
    public InitialUpdateReader<K, T> create(BlockIO bIO, Serializer<K, T> serializer, CacheImplClientSide<K, T> clientSidecache) {
        return new InitialUpdateReaderForStreams<K, T>(bIO, serializer, clientSidecache) ;  //  don't need  clientSideCache in this implementation;
    }
}
