package uk.co.octatec.scdds.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Jeromy Drake on 08/06/2016.
 *
 * This implementation will eliminate the queue between putting an item in the cache and it being published (and
 * the listeners being notified). It might be useful in a high volume publisher that doesn't have any local listeners
 * (if the cache had local listeners it might be risky to use this as the actions of the local listeners could delay the next
 * publication)
 */
public class NoneQueueingEventQueue<K, E extends Event<K>> implements EventQueue<K,E> {

    private final static Logger log = LoggerFactory.getLogger(EventQueueDefaultImpl.class);

    private EventQueueListener<K, E> listener;
    private boolean started = false;
    private String name;

    public NoneQueueingEventQueue(String name) {
        this.name = name;
    }

    @Override
    public void start(EventQueueListener<K, E> listener) {
        log.info("starting none-queyeing event-queue [{}]", name);
        this.listener = listener;
        started = true;
    }

    @Override
    public void stop() {
        log.info("stopping none-queyeing event-queue [{}]", name);
        started = false;
    }

    @Override
    public synchronized void  put(E event) throws InterruptedException {
        // this is called from a different thread to trigger an initial-download so it must
        // be synchronized, although most of the time it is not contended
        listener.onEvent(event);
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public boolean isStopped() {
        return !started;
    }
}
