package uk.co.octatec.scdds.cache.publish;

import uk.co.octatec.scdds.cache.Cache;
import uk.co.octatec.scdds.cache.CacheImpl;
import uk.co.octatec.scdds.cache.CacheListener;
import uk.co.octatec.scdds.cache.publish.threading.Threader;
import uk.co.octatec.scdds.net.serialize.SerializerFactory;

/**
 * Created by Jeromy Drake on 03/05/2016.
 */
public class CachePublisherFactoryDefaultImpl<K,T> implements CachePublisherFactory<K,T> {
    private final int serverPort;

    public CachePublisherFactoryDefaultImpl() {
        this(0);
    }

    public CachePublisherFactoryDefaultImpl(int serverPort) {
        // this should only be used for tests, normally the server will have the o/s choose
        // a port - the registry is automatically update with the port, so it works out
        // better than manually trying to maintain a port to use
        this.serverPort = serverPort;
    }

    @Override
    public CachePublisher<K, T> create(CacheImpl<K,T> cache, SerializerFactory<K, T> serializerFactory, GeneralRequestHandler generalRequestHandler, Threader threader) {
        CachePublisherImpl<K, T> publisher =  new CachePublisherImpl<>(cache, serializerFactory, generalRequestHandler, threader);
        publisher.setServerport(serverPort);
        return publisher;
    }
}
