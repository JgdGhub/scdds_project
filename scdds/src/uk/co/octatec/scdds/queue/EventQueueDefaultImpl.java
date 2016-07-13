package uk.co.octatec.scdds.queue;
/*
  SC/DDS - simple cached data distribution service

  Copyright 2016 by Jeromy Drake

  This program is free software; you may redistribute and/or modify it under
  the terms of the GNU General Public License Version 2 as published by the
  Free Software Foundation.

  This program is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY, without even the implied warranty of MERCHANTABILITY
  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
  for complete details.
*/
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Jeromy Drake on 30/04/2016.
 *
 * This is the default queue implementation used to process items in the cache, i.e. send the item for publishing
 * and notify listeners of the 'cache event'. Note: the act of publishing an item will pass it to a second queue attached
 * to a thread-pool in order to service multiple remote clients on different threads. Configuration options allow both queues
 * to be eliminated, the first by passing a ListenerNoneQueuingEventQueueFactory to the PublishingCacheBuilder, and the second
 * by setting GlobalDefaults.numberOfNetworkSendThreads = 0. (NB that's zero and not 1, if it is set to 1, a queue will still be
 * used passing the update to a single sending thread). Eliminating the queues might be useful if the publihers sole task is to
 * publish the data and it has no local listeners and also if the number of subscribers is low (the number of subscribers might
 * be only one if a 'router' was being used (See class Router)
 *
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
