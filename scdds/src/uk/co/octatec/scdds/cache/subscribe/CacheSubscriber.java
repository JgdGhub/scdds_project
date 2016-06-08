package uk.co.octatec.scdds.cache.subscribe;

/**
 * Created by Jeromy Drake.
 */
public interface CacheSubscriber<K,T> {
    boolean subscribe(int heartbeatSeconds);
    boolean unSubscribe();
    String getCacheName();
}
