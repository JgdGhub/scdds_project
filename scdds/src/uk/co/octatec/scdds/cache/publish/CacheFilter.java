package uk.co.octatec.scdds.cache.publish;

/**
 * Created by Jeromy Drake on 04/05/2016.
 */
public interface CacheFilter<K,T> {
    void init(String data);
    boolean accept(K key, T value);
}
