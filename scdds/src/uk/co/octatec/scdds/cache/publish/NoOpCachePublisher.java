package uk.co.octatec.scdds.cache.publish;

import uk.co.octatec.scdds.cache.ListenerEvent;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * Created by Jeromy Drake on 04/05/16
 */
public final class NoOpCachePublisher<K,T> implements CachePublisher<K,T> {
    @Override
    public int initializePort() throws IOException {
        return 0;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public String getCacheName() {
        return null;
    }

    @Override
    public boolean removeSubscription(long sessionId) {
        return true;
    }

    @Override
    public void onInitialUpdate(ServerSideCacheSubscription<K, T> serverSideCacheSubscription, Map<K, T> data) {

    }

    @Override
    public void onBatchUpdate(Collection<ListenerEvent<K, T>> listenerEvents) {

    }

    @Override
    public void onUpdate(K key, T value) {

    }

    @Override
    public void onDataStale() {

    }

    @Override
    public void onActive() {

    }

    @Override
    public void onRemoved(K key, T value) {

    }

    @Override
    public void onFailedClient(ServerSideCacheSubscription<K, T> client) {

    }
}
