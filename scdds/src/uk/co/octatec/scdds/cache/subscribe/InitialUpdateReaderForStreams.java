package uk.co.octatec.scdds.cache.subscribe;

import uk.co.octatec.scdds.net.serialize.Serializer;
import uk.co.octatec.scdds.net.socket.BlockIO;
import uk.co.octatec.scdds.net.socket.InitialUpdateReader;

import java.util.Map;

/**
 * Created by Jeromy Drake on 20/05/2016.
 */
public class InitialUpdateReaderForStreams<K,T> extends InitialUpdateReader<K,T>  {

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
