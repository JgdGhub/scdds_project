package uk.co.octatec.scdds.cache.publish;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by Jeromy Drake on 03/05/2016.
 */
public class MapFactoryDefaultImpl<K,T> implements MapFactory<K,T> {
    @Override
    public ConcurrentMap<K, T> create() {
        return new ConcurrentHashMap<>();
    }
}
