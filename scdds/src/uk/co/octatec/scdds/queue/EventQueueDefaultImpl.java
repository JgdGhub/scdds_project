package uk.co.octatec.scdds.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Jeromy Drake on 30/04/2016.
 */
public class EventQueueDefaultImpl<K, E extends Event<K>> extends AbstractEventQueue<K, E> implements EventQueue<K,E> {

    private final static Logger log = LoggerFactory.getLogger(EventQueueDefaultImpl.class);

    private final LinkedBlockingQueue<E> eventQueue = new  LinkedBlockingQueue<>();

    public EventQueueDefaultImpl(String name) {
        super(name);
        log.info("init EventQueueDefaultImpl {}  ", name);

    }
    public EventQueueDefaultImpl(int maxBatchEvents, String name) {
        super(maxBatchEvents, name);
        log.info("init {} EventQueueDefaultImpl maxBatchEvents={} ", name, maxBatchEvents);
    }

    @Override
    public void put(E event) throws InterruptedException{
        eventQueue.put(event);
    }

    E take() throws InterruptedException {
        return eventQueue.take();
    }

    boolean hasAvailableItems() {
        return eventQueue.size() > 0;
    }

    void logStartup(Logger logger, String text) {
        logger.info("{} starting default event queue", text);
    }

    int getQueueSize() {
        return eventQueue.size();
    }

}
