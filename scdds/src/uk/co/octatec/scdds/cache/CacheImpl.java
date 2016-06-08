package uk.co.octatec.scdds.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static uk.co.octatec.scdds.ConditionalCompilation._LOG_LATENCY;

import uk.co.octatec.scdds.GlobalProperties;
import uk.co.octatec.scdds.cache.persistence.EntryPersister;
import uk.co.octatec.scdds.cache.persistence.NoOpEntryPersister;
import uk.co.octatec.scdds.cache.publish.*;
import uk.co.octatec.scdds.cache.subscribe.CacheImplClientSide;
import uk.co.octatec.scdds.queue.EventQueue;
import uk.co.octatec.scdds.queue.EventQueueListener;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Jeromy Drake on 30/04/2016.
 */
public class CacheImpl<K,T> implements Cache<K,T>, EventQueueListener<K,ListenerEvent<K,T>> {

    private final static Logger log = LoggerFactory.getLogger(CacheImpl.class);

    private final ConcurrentMap<K,T> data; // = new ConcurrentHashMap<>();
    protected final EventQueue<K, ListenerEvent<K,T>> listenerEventQueue;
    protected final CopyOnWriteArrayList<CacheListener<K,T>> listeners = new CopyOnWriteArrayList<CacheListener<K,T>>();
    private EntryPersister<K,T> entryPersister = new NoOpEntryPersister<K,T>();
    private CachePublisher<K,T> cachePublisher = new NoOpCachePublisher<K,T>();
    protected  final ListenerEventFactory<K,T> listenerEventFactory;
    protected volatile boolean stale;

    private final String name;

    private volatile String fatalErrorText;

    static public <K,T> Cache<K,T> createLocalCache(String name) {
        // this creates a cache that has no publication ability, its main use would probably be in
        // users tests, but it might also be useful to distribute data within the same application
        CacheImpl<K,T> cache = new CacheImpl<K,T>(name);
        cache.start();
        return cache;
    }

    static public <K,T> Cache<K,T> createLocalCache(String name, ListenerEventQueueFactory<K, ListenerEvent<K,T>>  queueFactory, MapFactory<K,T> mapFactory) {
        CacheImpl<K,T> cache = new CacheImpl<K,T>(name, queueFactory, mapFactory);
        cache.start();
        return cache;
    }

    static public <K,T> Cache<K,T> createLocalCache(String name, ListenerEventQueueFactory<K, ListenerEvent<K,T>>  queueFactory, MapFactory<K,T> mapFactory, ListenerEventFactory<K,T> listenerEventFactory) {
        CacheImpl<K,T> cache = new CacheImpl<K,T>(name, queueFactory, mapFactory, listenerEventFactory);
        cache.start();
        return cache;
    }

    protected CacheImpl(String name, ListenerEventQueueFactory<K, ListenerEvent<K,T>>  queueFactory, MapFactory<K,T> mapFactory, ListenerEventFactory<K,T> listenerEventFactory) {
        this.name = name;
        if( queueFactory == null ) {
            queueFactory = new ListenerEventQueueFactoryDefaultImpl<>();
        }
        listenerEventQueue = queueFactory.create(getEventQueueName());
        this.listenerEventFactory = listenerEventFactory==null ? new ListenerEventFactoryDefaultImpl<K,T>(): listenerEventFactory;
        if( mapFactory == null ) {
            mapFactory = new MapFactoryDefaultImpl<>();
        }
        data = mapFactory.create();
    }

    protected CacheImpl(String name, ListenerEventQueueFactory<K, ListenerEvent<K,T>>  queueFactory, MapFactory<K,T> mapFactory) {
        this.name = name;
        if( queueFactory == null ) {
            queueFactory = new ListenerEventQueueFactoryDefaultImpl<>();
        }

        listenerEventQueue = queueFactory.create(getEventQueueName());
        listenerEventFactory = new ListenerEventFactoryDefaultImpl<>();
        if( mapFactory == null ) {
            mapFactory = new MapFactoryDefaultImpl<>();
        }
        data = mapFactory.create();
    }

    protected String getEventQueueName() {
        return name +".srv";
    }

    protected CacheImpl(String name) {
        this(name, new ListenerEventQueueFactoryDefaultImpl<K, ListenerEvent<K,T>>(), new MapFactoryDefaultImpl<K, T>(), new ListenerEventFactoryDefaultImpl<K,T>());
    }
    protected CacheImpl() {
        this("unnamed", new ListenerEventQueueFactoryDefaultImpl<K, ListenerEvent<K,T>>(), new MapFactoryDefaultImpl<K, T>());
    }
    protected void start() {
        listenerEventQueue.start(this);
    }

    @Override
    public int size() {
        return data.size();

    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Collection<T> values() {
        return Collections.unmodifiableCollection(data.values());
    }

    @Override
    public Set<K> keySet() {
        return Collections.unmodifiableSet(data.keySet());
    }

    @Override
    public Set<Map.Entry<K, T>> entrySet() {
        return Collections.unmodifiableSet(data.entrySet());
    }

    @Override
    public T get(K key) {
        return data.get(key);
    }

    @Override
    public T put(K key, T value) {

        entryPersister.store(key, value);
            // we store the entry before doing anything so that is the store fails, the entry does not get published
            // the default entry persister is No-Op and does nothing - often the datw will not need to be perisisted at
            // all, but if it does you can either user the DataStore implementation or create your own

        T oldValue = data.put(key, value);
        ListenerEvent<K,T> event = listenerEventFactory.create();
        event.action = ListenerEvent.Action.update;
        event.key = key;
        event.value = value;
        event.canBeBatched = true;
        try {
            listenerEventQueue.put(event);
        }
        catch(InterruptedException e){
            log.error("listeners not notified due to InterruptedException adding CacheListener event to queue key=[{}] value=[{}]", key, value);
            log.error("got InterruptedException adding CacheListener event to queue", e);
        }
        return oldValue;
    }

    @Override
    public T remove(K key) {
        T value = data.remove(key);
        if( value == null ) {
            log.warn("remove: key [{}] does not exists in [{}], no events will be fired", key, name);
            return null;
        }
        entryPersister.markDeleted(key);
        ListenerEvent<K,T> event = new ListenerEvent<>();
        event.action = ListenerEvent.Action.remove;
        event.key = key;
        event.value = value;
        try {
            listenerEventQueue.put(event);
        }
        catch(InterruptedException e){
            log.error("listeners not notified due to InterruptedException adding CacheListener event to queue key=[{}] value=[{}]", key, value);
            log.error("got InterruptedException adding CacheListener event to queue", e);
        }
        return value;
    }

    @Override
    public boolean isStale() {
        return stale;
    }

    public void requestInitialUpdate(ServerSideCacheSubscription<K,T> cacheSubsciption) {
        log.info("requestInitialUpdate for sessionId [{}]", cacheSubsciption.getSessionId());
        ListenerEvent<K,T> event = new ListenerEvent<>();
        event.action = ListenerEvent.Action.publisherInitialUpdate;
        event.cacheSubsciption =  cacheSubsciption;
        try {
            listenerEventQueue.put(event);
        }
        catch(InterruptedException e){
            log.error("listeners not notified due to InterruptedException adding CacheListener event to session-id=[{}]", cacheSubsciption.getSessionId());
            log.error("got InterruptedException adding CacheListener event to queue", e);
        }
    }

    @Override
    public void notifyStale() {
        stale = true;
        log.info("setting cache stale");
        ListenerEvent<K,T> event = new ListenerEvent<>();
        event.action = ListenerEvent.Action.stale;
        try {
            listenerEventQueue.put(event);
        }
        catch(InterruptedException e){
            log.error("cache not marked stale due to InterruptedException adding CacheListener event to queue");
            log.error("got InterruptedException adding event to queue", e);
        }
    }

    protected void notifyFatalError(String errorText) {
        // this is only called while trying to subscribe on the client side
        // id an unrecoverable error occurs
        log.info("notifyFatalError [{}] cache=[{}]", errorText, name);
        stale = true;
        ListenerEvent<K,T> event = new ListenerEvent<>();
        event.action = ListenerEvent.Action.fatal;
        fatalErrorText = errorText;
        try {
            listenerEventQueue.put(event);
        }
        catch(InterruptedException e){
            log.error("cache not marked stale due to InterruptedException adding CacheListener event to queue");
            log.error("got InterruptedException adding event to queue", e);
        }
    }

    @Override
    public void notifyUnStale() {
        stale = false;
        log.info("setting cache un-stale");
        ListenerEvent<K,T> event = new ListenerEvent<>();
        event.action = ListenerEvent.Action.active;
        try {
            listenerEventQueue.put(event);
        }
        catch(InterruptedException e){
            log.error("cache not marked stale due to InterruptedException adding CacheListener event to queue");
            log.error("got InterruptedException adding event to queue", e);
        }
    }

    @Override
    public void addListener(CacheListener<K, T> listener) {

        log.info("cache: [{}] addListener [{}] map-size=[{}]", name, listener, data.size());
        if( fatalErrorText != null ) {
            log.warn("adding listing to cache in fatal-error state");
            listener.onDataStale();
            listener.onFatalError(fatalErrorText);
            return;
        }

        ListenerEvent<K,T> event = new ListenerEvent<>();
        event.action = ListenerEvent.Action.initialUpdate;
        event.listenerForInitialUpdate = listener;
        try {
            listenerEventQueue.put(event);
            listeners.add(listener);
            log.info("cache: listener added [{}] size=[{}]", listener, listeners.size());
        }
        catch(InterruptedException e){
            log.error("listener {} not added due to InterruptedException adding CacheListener event to queue", listener);
            log.error("got InterruptedException adding CacheListener event to queue", e);
        }
    }

    @Override
    public void removeListener(CacheListener<K, T> listener) {
        log.info("cache: removeListener {}", listener);
        listeners.remove(listener);
    }

    @Override
    public void onBatchedEvents(Collection<ListenerEvent<K, T>> events) {

        // NB - it is guaranteed that the events in a batch are Update events,
        // those are the only ones that can be batched

        cachePublisher.onBatchUpdate(events);
        for(ListenerEvent<K, T> event : events) {
            dispatchOnUpdate(event.key, event.value);
        }

        //for(ListenerEvent<K, T> event : events) {
        //    onEvent(event);
        //}

    }

    @Override
    public void onEvent(ListenerEvent<K, T> event) {

        // dispatch cache events to all listeners

        // we are now in a separate thread dispatching to listeners,
        // events for the same key should already have be coalesced within
        // so that multiple events with the same key are replaced with
        // just the latest event

        // nb: tried to make this an inner class but couldn't get round the generic syntax
        //
        //this wouldn't compile...
        //class CacheEventQueueListener<K,E> implements EventQueueListener<K,ListenerEvent<K,T>> {
        //    @Override
        //    public void onEvent(ListenerEvent<K, T> event) {
        //        for(CacheListener<K,T> listener : listeners) {
        //            listener.onUpdate(event.key, event.value);
        //        }
        //    }
        //}

        if( _LOG_LATENCY ) {
            long t2 = System.nanoTime();
            GlobalProperties.llog_1.info("!!(1)cache event key=[{}] action=[{}] listener-count=[{}] queueLatency=[{} ns]", event.key, event.action, listeners.size(), (t2 - event.timestamp));
        }

        if( event.action == ListenerEvent.Action.update) {
            // data was added to the cache, all local listeners will
            // be notified as well as the publisher
            // the same code runs in the client cache, but there
            // the publisher is a No-Op implementation
            cachePublisher.onUpdate(event.key, event.value);
            dispatchOnUpdate(event.key, event.value);
        }
        else if( event.action == ListenerEvent.Action.initialUpdate) {
            // the cache publisher never gets this event because publishes
            // the the cache's data as part of its initialization phase

            if( stale ) {
                log.warn("cache detected stale during initial update");
                event.listenerForInitialUpdate.onDataStale();
            }
            else {
                event.listenerForInitialUpdate.onActive(); // always send an OnActive message
            }

            event.listenerForInitialUpdate.onInitialUpdate(this);

            if( fatalErrorText != null  ) {
                log.warn("cache detected fatal error is set during initial update");
                event.listenerForInitialUpdate.onFatalError(fatalErrorText);
            }
        }
        else if( event.action == ListenerEvent.Action.stale) {
            cachePublisher.onDataStale();
            dipatchOnDataStale();
        }
        else if( event.action == ListenerEvent.Action.publisherInitialUpdate) {
            // a new client has subscribed to the cache - the subscription mechanism
            // arranges for an event to be fired on the queue-listener thread (here)
            // so that all publications take place omn the same thread
            cachePublisher.onInitialUpdate(event.cacheSubsciption, data);
        }
        else if( event.action == ListenerEvent.Action.active) {
            cachePublisher.onActive();
            dipatchOnActive();
        }
        else if( event.action == ListenerEvent.Action.remove) {
            cachePublisher.onRemoved(event.key, event.value);
            dipatchOnRemoved(event.key, event.value);
        }
        else if( event.action == ListenerEvent.Action.fatal) {
            dispatchOnFatal();
        }
    }

    private void dispatchOnUpdate(K key, T value) {
        for(CacheListener<K,T> listener : listeners) {
            try {
                listener.onUpdate(key, value);
            }
            catch(Throwable t) {
                log.warn("caught throwable while dispatching to listeners", t);
            }
        }
    }

    private void dispatchOnFatal() {
        log.info("dispatching onFatal({}) to all listeners", fatalErrorText);
        for(CacheListener<K,T> listener : listeners) {
            try {
                listener.onDataStale();
                listener.onFatalError(fatalErrorText);
            }
            catch(Throwable t) {
                log.warn("caught throwable while dispatching to listeners", t);
            }
        }
    }

    private void dipatchOnRemoved(K key, T value) {
        for(CacheListener<K,T> listener : listeners) {
            try {
                listener.onRemoved(key, value);
            }
            catch(Throwable t) {
                log.warn("caught throwable while dispatching to listeners", t);
            }
        }
    }

    private void dipatchOnDataStale() {
        for(CacheListener<K,T> listener : listeners) {
            try {
                listener.onDataStale();
            }
            catch(Throwable t) {
                log.warn("caught throwable while dispatching to listeners", t);
            }
        }
    }

    private void dipatchOnActive() {
        for(CacheListener<K,T> listener : listeners) {
            try {
                listener.onActive();
            }
            catch(Throwable t) {
                log.warn("caught throwable while dispatching to listeners", t);
            }
        }
    }

    void setEntryPersister(EntryPersister<K, T> entryPersister) {
        log.info("setEntryPersister [{}]", entryPersister);
        this.entryPersister = entryPersister;
    }
    void setCachePublisher(CachePublisher<K, T> cachePublisher) {
        log.info("setCachePublisher [{}]", cachePublisher);
        this.cachePublisher = cachePublisher;
    }

    public void dispose() {
        try {
            listenerEventQueue.stop();
            listeners.clear();
            data.clear();
            entryPersister.close();
            cachePublisher.onDataStale();
            dipatchOnDataStale();
            cachePublisher.stop();
        }
        catch( RuntimeException e) {
            log.error("ecxeption while disposing of cache [{}]", name, e);
        }
    }

    protected Map<K,T> getData() {
        // not part of the interface - only for user by the subscriber on the client side during initial update
        return data;
    }
}
