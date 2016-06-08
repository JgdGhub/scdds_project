package uk.co.octatec.scdds.cache.subscribe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.cache.*;
import uk.co.octatec.scdds.cache.publish.MapFactory;

import java.util.Map;

/**
 * Created by Jeromy Drake on 05/05/16
 */
// the only point of this claiss is to 'hide' the start() and getData() methods, just maing it available for
// very specific use in the CacheSubscriber
public class CacheImplClientSide<K,T> extends CacheImpl<K,T> {

    private final static Logger log = LoggerFactory.getLogger(CacheImplClientSide.class);

    public CacheImplClientSide(String name, ListenerEventQueueFactory<K, ListenerEvent<K,T>> queueFactory, MapFactory<K,T> mapFactory) {
        super(name, queueFactory, mapFactory);
    }

    public CacheImplClientSide(String name, ListenerEventQueueFactory<K, ListenerEvent<K,T>> queueFactory, MapFactory<K,T> mapFactory,  ListenerEventFactory<K,T> listenerEventFactory) {
        super(name, queueFactory, mapFactory, listenerEventFactory);
    }

    public CacheImplClientSide(String name) {
        super(name);
    }

    public CacheImplClientSide() {
        super();
    }

    private volatile boolean netErrorState;

    static public <K,T> Cache<K,T> createLocalCache(String name) {
        // this creates a cache that is 'started', i.e. will notify listeners of updates
        // its main use is in tests
        CacheImplClientSide<K,T> cache = new CacheImplClientSide<K,T>(name);
        cache.clientSideStart();
        return cache;
    }

    void clientSideStart() {
        log.info("starting client-side cache netErrorState=[{}] stale=[{}]", netErrorState, isStale());
        start(); // safe to call start() when already started
        if( netErrorState )  {
            log.warn("setting network error state [false] and stale [false]");
            netErrorState = false;
            stale = false;
            log.info("dispatch initial-update to all listeners after network recovery");
            dispatchOnActiveAndInitialUpdateToAllListeners();
                // the first call to start() does the dispatch, it won't do it
                // if its already started - we only come here again if there
                // has been a network error, in which case we need to manually
                // dispatch the update
        }
    }

    void clientNotifyFatalError(String errorText) {
        notifyFatalError(errorText);
    }

    private void dispatchOnActiveAndInitialUpdateToAllListeners() {
        log.info("dispatchOnActiveAndInitialUpdateToAllListeners stale?{}...", isStale());
        for(CacheListener<K,T> listener : listeners) {
            try {
                listener.onActive();
                listener.onInitialUpdate(this);
            }
            catch(Throwable t) {
                log.warn("caught throwable while dispatching to listeners", t);
            }
        }
    }

    public void setNetworkErrorState() {
        log.warn("setting network error state [true]");
        netErrorState = true;
    }

    protected String getEventQueueName() {
        return getName() +".cli";
    }

    Map<K,T> clientSideGetData() {
        return getData();
    }
}
