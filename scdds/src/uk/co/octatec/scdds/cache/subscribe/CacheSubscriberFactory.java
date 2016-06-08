package uk.co.octatec.scdds.cache.subscribe;

import uk.co.octatec.scdds.cache.publish.CacheFilter;
import uk.co.octatec.scdds.net.registry.CacheLocator;

/**
 * Created by Jeromy Drake on 04/05/2016.
 */
public interface CacheSubscriberFactory<K,T> {
    CacheSubscriber<K,T> create(CacheImplClientSide<K,T> cache, CacheLocator locator, CacheFilter<K,T> filter, String filterArg, InitialUpdateReaderFactory<K,T> initialUpdateReaderFactory);
}
