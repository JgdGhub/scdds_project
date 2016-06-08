package uk.co.octatec.scdds.mock;

import uk.co.octatec.scdds.cache.publish.CacheFilter;
import uk.co.octatec.scdds.cache.subscribe.CacheImplClientSide;
import uk.co.octatec.scdds.cache.subscribe.CacheSubscriber;
import uk.co.octatec.scdds.cache.subscribe.CacheSubscriberFactory;
import uk.co.octatec.scdds.cache.subscribe.InitialUpdateReaderFactory;
import uk.co.octatec.scdds.net.registry.CacheLocator;

/**
 * Created by Jeromy Drake on 08/05/16
 */
public class MockCacheSubscriberFactory<K,T> implements CacheSubscriberFactory<K,T> {
    @Override
    public CacheSubscriber<K, T> create(CacheImplClientSide<K, T> cache, CacheLocator locator, CacheFilter<K, T> filter, String filterArg, InitialUpdateReaderFactory<K, T> initialUpdateReaderFactory) {
        return new MockCacheSubscriber<K,T>();
    }
}
