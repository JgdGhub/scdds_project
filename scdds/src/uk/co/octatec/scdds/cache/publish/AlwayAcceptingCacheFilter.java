package uk.co.octatec.scdds.cache.publish;

/**
 * Created by Jeromy Drake on 04/05/16
 */
public final class AlwayAcceptingCacheFilter<K,T> implements CacheFilter<K,T> {
    @Override
    public void init(String data) {

    }

    @Override
    public boolean accept(K key, T value) {
        return true;
    }
}
