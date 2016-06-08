package uk.co.octatec.scdds.cache.publish;

import uk.co.octatec.scdds.cache.ListenerEvent;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * Created by Jeromy Drake on 03/05/2016.
 */
public interface CachePublisher<K,T>  {

    String ARG_SESSION_ID = "session-id";
    String ARG_FILTER = "filter";
    String ARG_FILTER_ARG = "filter-arg";
    String ARG_SERIALIZER_FACTORY = "serializer-factory";
    String ARG_CONNECTION_COUNT   = "connection-count";
    String ARG_CACHE_NAME   = "cache-name";
    String ARG_PORT         = "port";
    String ARG_EXPECED_CACHE_NAME = "expected-cache-name";
    String ARG_ERROR   = "error";
    String ARG_TIMESTAMP = "timestamp";
    String ARG_LOAD = "load";

    String ARG_REQUEST = "request";
    String RQST_HEARTBET   = "heartbeat";
    String RQST_SUBSCRIBE   = "subscribe";
    String RQST_UNSUBSCRIBE   = "unsubscribe";
    String RQST_LOAD_CHECK  = "load-check";

    int initializePort() throws IOException;
    void start();
    void stop();
    String getCacheName();
    boolean removeSubscription(long sessionId);

    void onInitialUpdate(ServerSideCacheSubscription<K,T> serverSideCacheSubscription, Map<K,T> data);
    void onUpdate(K key, T value);
    void onBatchUpdate(Collection<ListenerEvent<K, T>> events);
    void onDataStale();
    void onActive();
    void onRemoved(K key, T value);
    void onFailedClient(ServerSideCacheSubscription<K,T> client);
}
