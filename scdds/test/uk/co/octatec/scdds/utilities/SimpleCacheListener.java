package uk.co.octatec.scdds.utilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.cache.CacheListener;
import uk.co.octatec.scdds.cache.subscribe.ImmutableCache;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by Jeromy Drake on 06/05/16
 */
public class SimpleCacheListener<K, T> implements CacheListener<K, T> {

    private final static Logger log = LoggerFactory.getLogger(SimpleCacheListener.class);

    public volatile int onInitialUpdateMapSize;
    public volatile int onInitialUpdateCount;
    public volatile int onUpdateCount;
    public volatile int onRemoveCount;
    public volatile int onDataStaleCount;
    public volatile int onActiveCount;
    public volatile int onFatalCount;
    public volatile String fatalErrorText;
    public volatile Map<K, T> initialUpdate;
    public volatile Map<K, T> updateAccumulation = new HashMap<>();
    public volatile Map<K, T> removedAccumulation = new HashMap<>();
    public volatile boolean isStale;

    private volatile boolean allowOnUpdatelogging = true;

    public void suppressOnUpdateLogging() { // supres
        allowOnUpdatelogging = false;
    }

    @Override
    public void onInitialUpdate(ImmutableCache<K, T> initialUpdate) {
        ++onInitialUpdateCount;
        onInitialUpdateMapSize = initialUpdate.size();
        log.info("ON-INITIAL_UPDATE count=[{}] data-size=[{}]", onInitialUpdateCount, initialUpdate.size());
        this.initialUpdate = new HashMap<>();
        Set<Map.Entry<K,T>> set = initialUpdate.entrySet();
        for(Map.Entry<K,T> e : set ) {
            this.initialUpdate.put(e.getKey(), e.getValue());
        }
    }

    @Override
    public void onUpdate(K key, T value) {
        ++onUpdateCount;
        if( allowOnUpdatelogging ) {
            log.info("ON-UPDATE key=[{}], value=[{}] count=[{}]", key, value, onUpdateCount);
        }
        updateAccumulation.put(key, value);
    }

    @Override
    public void onRemoved(K key, T value) {
        ++onRemoveCount;
        log.info("ON-REMOVE key=[{}], value=[{}] count=[{}]", key, value, onUpdateCount);
        removedAccumulation.put(key, value);
    }

    @Override
    public void onDataStale() {
        log.info("ON-DATA_STALE");
        isStale = true;
        ++onDataStaleCount;
    }

    @Override
    public void onActive() {
        log.info("ON-ACTIVE");
        isStale = false;
        ++onActiveCount;
    }

    @Override
    public void onFatalError(String errorText) {
        log.info("ON-FATAL-ERROR");
        ++onFatalCount;
        fatalErrorText = errorText;
    }

    public void awaitOnRemoveCount(int n) throws InterruptedException {
        for (int i = 0; i < AwaitParams.AWAIT_LOOP_COUNT; i++) {
            if (onRemoveCount == n) {
                break;
            } else {
                Thread.sleep(AwaitParams.AWAIT_SLEEP_TIME);
            }
        }
        if( onRemoveCount < n ) {
            log.warn("*** awaitOnRemoveCount: wait failed");
        }
    }
    public void awaitOnInitialUpdateCount(int n) throws InterruptedException {
        for (int i = 0; i < AwaitParams.AWAIT_LOOP_COUNT; i++) {
            if (onInitialUpdateCount == n) {
                break;
            } else {
                Thread.sleep(AwaitParams.AWAIT_SLEEP_TIME);
            }

        }
        if( onInitialUpdateCount < n ) {
            log.warn("*** awaitOnInitialUpdateCount: wait failed");
        }
    }
    public void awaitOnUpdateCount(int n) throws InterruptedException {
        for (int i = 0; i < AwaitParams.AWAIT_LOOP_COUNT; i++) {
            if (onUpdateCount == n) {
                break;
            } else {
                Thread.sleep(AwaitParams.AWAIT_SLEEP_TIME);
            }

        }
        if( onUpdateCount < n ) {
            log.warn("*** awaitOnUpdateCount: wait failed");
        }
    }
    public void awaitOnUpdateCountGte(int n) throws InterruptedException {
        for (int i = 0; i < AwaitParams.AWAIT_LOOP_COUNT; i++) {
            if (onUpdateCount >= n) {
                break;
            } else {
                Thread.sleep(AwaitParams.AWAIT_SLEEP_TIME);
            }
            if( onUpdateCount < n ) {
                log.warn("*** awaitOnUpdateCountGte: wait failed");
            }
        }
    }
    public void awaitOnDataStaleCount(int n) throws InterruptedException {
        for(int i=0; i<AwaitParams.AWAIT_LOOP_COUNT; i++) {
            Thread.sleep(AwaitParams.AWAIT_SLEEP_TIME); // give the system chance to record the call
            if( onDataStaleCount==n ) {
                break;
            }
        }
        if( onDataStaleCount < n ) {
            log.warn("*** awaitOnDataStaleCount: wait failed");
        }
    }
    public void awaitOnDataStaleCountGte(int n) throws InterruptedException {
        for(int i=0; i<AwaitParams.AWAIT_LOOP_COUNT; i++) {
            Thread.sleep(AwaitParams.AWAIT_SLEEP_TIME); // give the system chance to record the call
            if( onDataStaleCount>=n ) {
                break;
            }
        }
        if( onDataStaleCount < n ) {
            log.warn("*** awaitOnDataStaleCount: wait failed");
        }
    }
    public void awaitOnActiveCount(int n) throws InterruptedException {
        for(int i=0; i<AwaitParams.AWAIT_LOOP_COUNT; i++) {
            Thread.sleep(AwaitParams.AWAIT_SLEEP_TIME); // give the system chance to record the call
            if( onActiveCount==n ) {
                break;
            }

        }
        if( onActiveCount < n ) {
            log.warn("*** awaitOnActiveCount: wait failed");
        }
    }
    public void awaitOnFatalCount(int n) throws InterruptedException {
        for (int i = 0; i < AwaitParams.AWAIT_LOOP_COUNT; i++) {
            if (onFatalCount == n) {
                break;
            } else {
                Thread.sleep(AwaitParams.AWAIT_SLEEP_TIME);
            }
        }
        if( onFatalCount < n ) {
            log.warn("*** awaitOnFatalCount: wait failed");
        }
    }

}
