package uk.co.octatec.scdds.utilities;

import uk.co.octatec.scdds.cache.subscribe.CacheSubscriberImpl;
import uk.co.octatec.scdds.cache.subscribe.ImmutableCache;

/**
 * Created by Jeromy Drake on 10/05/2016.
 */
public class AwaitParams {
    public static final int AWAIT_SLEEP_TIME = 20;
    public static final int AWAIT_LOOP_COUNT = 100;

    public static<K,T>  void awaitCacheSizeGte(ImmutableCache<K,T> cache, int n) throws  InterruptedException{
        for(int i=0; i<AWAIT_LOOP_COUNT; i++) {
            if( cache.size() >= n ) {
                break;
            }
            Thread.sleep(AWAIT_SLEEP_TIME);
        }
    }

    public static<K,T>  void awaitCacheStaleValue(ImmutableCache<K,T> cache, boolean requiredValue) throws  InterruptedException{
        for(int i=0; i<AWAIT_LOOP_COUNT; i++) {
            if( cache.isStale() == requiredValue) {
                break;
            }
            Thread.sleep(AWAIT_SLEEP_TIME);
        }
    }
}
