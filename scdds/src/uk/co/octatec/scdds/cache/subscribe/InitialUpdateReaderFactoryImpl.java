package uk.co.octatec.scdds.cache.subscribe;

import uk.co.octatec.scdds.net.serialize.Serializer;
import uk.co.octatec.scdds.net.socket.BlockIO;
import uk.co.octatec.scdds.net.socket.InitialUpdateReader;

/**
 * Created by Jeromy Drake on 20/05/2016.
 */
public class InitialUpdateReaderFactoryImpl<K,T> implements InitialUpdateReaderFactory<K,T>  {
    @Override
    public InitialUpdateReader<K, T> create(BlockIO bIO, Serializer<K,T> serializer, CacheImplClientSide<K, T> clientSideCache) {
        return new InitialUpdateReader<K, T>(bIO, serializer) ;  //  don't need  clientSideCache in this implementation
    }
}
