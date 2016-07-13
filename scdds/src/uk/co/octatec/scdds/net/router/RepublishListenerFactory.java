package uk.co.octatec.scdds.net.router;

import uk.co.octatec.scdds.cache.Cache;
import uk.co.octatec.scdds.cache.CacheListener;

/**
 * Created by Jeromy Drake on 13/07/2016.
 */
public interface RepublishListenerFactory {
    CacheListener create(Cache cache, String inCacheName);
}
