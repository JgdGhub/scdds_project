package uk.co.octatec.scdds.mock;

import uk.co.octatec.scdds.cache.subscribe.CacheSubscriber;

/**
 * Created by Jeromy Drake on 09/05/2016.
 */
public class MockCacheSubscriber<K,T> implements CacheSubscriber<K,T>  {
    @Override
    public boolean subscribe(int heartbeatSeconds) {
        return false;
    }

    @Override
    public boolean unSubscribe() {
        return false;
    }

    @Override
    public String getCacheName() {
        return "MockCacheName";
    }
}
