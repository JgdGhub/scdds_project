package uk.co.octatec.scdds.cache.publish;

import java.util.concurrent.ConcurrentMap;

/**
 * Created by Jeromy Drake on 03/05/2016.
 */
public interface MapFactory<K,T> {
    ConcurrentMap<K,T> create();
}
