package uk.co.octatec.scdds.cache.subscribe;

import uk.co.octatec.scdds.cache.publish.CacheFilter;
import uk.co.octatec.scdds.net.registry.CacheLocator;

/**
 * Created by Jeromy Drake.
 */
public class CacheSubscriberFactoryDefaultImpl<K,T> implements CacheSubscriberFactory<K,T> {
    @Override
    public CacheSubscriber create(CacheImplClientSide<K,T> cache, CacheLocator locator, CacheFilter<K,T> filter, String filerArg, InitialUpdateReaderFactory<K,T> initialUpdateReaderFactory) {
        return new CacheSubscriberImpl<K,T>(cache, locator, filter, filerArg, initialUpdateReaderFactory);
    }
}
