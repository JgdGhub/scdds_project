package uk.co.octatec.scdds.instrumentation;

import uk.co.octatec.scdds.cache.CacheImpl;
import uk.co.octatec.scdds.cache.publish.CachePublisher;
import uk.co.octatec.scdds.cache.publish.CachePublisherFactory;
import uk.co.octatec.scdds.cache.publish.CachePublisherImpl;
import uk.co.octatec.scdds.cache.publish.GeneralRequestHandler;
import uk.co.octatec.scdds.cache.publish.threading.Threader;
import uk.co.octatec.scdds.net.serialize.SerializerFactory;

/**
 * Created by Jeromy Drake on 12/05/2016.
 */
public class InstrumentedCachePublisherFactory<K,T> implements CachePublisherFactory<K,T> {


    private final InstrumentedServerSession instrumentedServerSession = new InstrumentedServerSession();

    public InstrumentedServerSession getInstrumentedServerSession() {
        return instrumentedServerSession;
    }

    @Override
    public CachePublisher<K, T> create(CacheImpl<K, T> cache, SerializerFactory<K, T> serializerFactory, GeneralRequestHandler generalRequestHandler, Threader threader) {
        return new CachePublisherImpl<K, T>(cache, serializerFactory, generalRequestHandler, instrumentedServerSession, threader);
    }
}
