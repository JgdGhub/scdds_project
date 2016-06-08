package uk.co.octatec.scdds.cache.subscribe;

import uk.co.octatec.scdds.net.serialize.Serializer;
import uk.co.octatec.scdds.net.socket.BlockIO;
import uk.co.octatec.scdds.net.socket.InitialUpdateReader;

/**
 * Created by Jeromy Drake on 20/05/2016.
 */
public interface InitialUpdateReaderFactory<K,T> {
    InitialUpdateReader<K,T> create(BlockIO bIO, Serializer<K,T> serializer, CacheImplClientSide<K, T> clientSidecache);
}
