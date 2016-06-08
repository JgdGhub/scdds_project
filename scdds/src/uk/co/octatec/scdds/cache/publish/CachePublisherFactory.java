package uk.co.octatec.scdds.cache.publish;

import uk.co.octatec.scdds.cache.Cache;
import uk.co.octatec.scdds.cache.CacheImpl;
import uk.co.octatec.scdds.cache.CacheListener;
import uk.co.octatec.scdds.cache.publish.threading.Threader;
import uk.co.octatec.scdds.net.serialize.SerializerFactory;

/**
 * Created by Jeromy Drake on 03/05/2016.
 */
public interface CachePublisherFactory<K,T>  {
    CachePublisher<K,T> create(CacheImpl<K,T> cache, SerializerFactory<K, T> serializerFactory, GeneralRequestHandler generalRequestHandler, Threader threader);
}
