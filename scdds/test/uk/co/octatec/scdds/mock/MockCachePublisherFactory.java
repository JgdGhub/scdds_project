package uk.co.octatec.scdds.mock;

import uk.co.octatec.scdds.cache.CacheImpl;
import uk.co.octatec.scdds.cache.publish.CachePublisher;
import uk.co.octatec.scdds.cache.publish.CachePublisherFactory;
import uk.co.octatec.scdds.cache.publish.GeneralRequestHandler;
import uk.co.octatec.scdds.cache.publish.threading.Threader;
import uk.co.octatec.scdds.net.serialize.SerializerFactory;

/**
 * Created by Jeromy Drake on 07/05/16
 */
public class MockCachePublisherFactory<K,T> implements CachePublisherFactory<K,T> {
    @Override
    public CachePublisher<K, T> create(CacheImpl<K, T> cache, SerializerFactory<K, T> serializerFactory, GeneralRequestHandler generalRequestHandler, Threader threader) {
        return new MockCachePublisher<K,T>(generalRequestHandler);
    }
}
