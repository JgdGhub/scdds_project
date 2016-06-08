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
import uk.co.octatec.scdds.GlobalDefaults;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;

import static uk.co.octatec.scdds.ConditionalCompilation._DBG;

/**
 * Created by Jeromy Drake on 16/05/2016.
 */
public abstract class AbstractEventQueue<K, E extends Event<K>>  {

    private final static Logger log = LoggerFactory.getLogger(AbstractEventQueue.class);

    private static final int DFLT_MAX_MATCH_SIZE = GlobalDefaults.BATCH_SIZE;

    private final int maxBatchEvents;
    private EventQueueListener listener;
    private Thread thread;
    private volatile boolean stopped = true;

    protected final String name;

    private final LinkedHashMap<K, E> batchEvents = new  LinkedHashMap<>();

    public AbstractEventQueue(int maxBatchEvents, String name) {
        this.name = name;
        this.maxBatchEvents = maxBatchEvents;
    }

    public AbstractEventQueue(String name) {
        this.name = name;
        this.maxBatchEvents = DFLT_MAX_MATCH_SIZE;
    }


    void setListener(EventQueueListener listener) { // for tests
        this.listener = listener;
    }

    public void start(EventQueueListener listener) {
        if( !stopped ) {
            // its ok to try and start the queue while it is running
            log.info("attempt to start queue multiple times");
            return;
        }
        stopped = false;
        this.listener = listener;
        log.info("start event queue {} [{}] listener=[{}]", name, listener, this.getClass().getSimpleName());
        if( listener == null)  {
            throw new NullPointerException("starting event queue with null listener");
        }
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                mainLoop();
            }
        });
        thread.setDaemon(true);
        thread.setName("EventQueue");
        thread.start();
    }

    public void stop() {  // mostly for tests
        log.info("stop event queue {}", name);
        stopped = true;
        if( thread != null ) {
            thread.interrupt();
        }
    }

    abstract E take() throws InterruptedException;
    abstract boolean hasAvailableItems();
    abstract void logStartup(Logger logger, String text);

    private void mainLoop() {

        logStartup(log, name+" MAINLOOP: ");

        while(true) {
            try {
                processEvents();
            }
            catch(InterruptedException x) {
                if( stopped ) {
                    log.warn("{} queue is stopped", name);
                }
                else {
                    log.warn("InterruptedException in event queue loop "+name);
                }
                break;
            }
        }
        log.info("{} finished running event queue loop", name);
    }

    void processEvents() throws InterruptedException {

        batchEvents.clear();

        E event = take();
        K key = event.getKey();
        if (_DBG) {
            log.debug("{} take event [{}] key=[{}]", name, event, key);
        }

        int count = 0;
        if (event.canBeBatched()) {
            batchEvents.put(key, event);
            ++count;
        } else {
            listener.onEvent(event);
        }

        while ( hasAvailableItems() && count < maxBatchEvents) {
            event = take();
            if( event == null) {
                log.error("NULL EVENT TAKEN");
                continue;
            }
            key = event.getKey();
            if (event.canBeBatched()) {
                batchEvents.put(key, event);
                ++count;
                if (_DBG) {
                    log.debug("{} take event {} {}", name, event, count);
                }
            } else {
                if (_DBG) {
                    log.debug("{} take event {}", name, event);
                    log.debug("{} accumulated events count={} (batching stopped by none-batchable event)", name, batchEvents.size());
                }
                // a none-batchable event must stop the batching processes, otherwise
                // events could be delivered out of order
                if( count > 0 ) {
                    listener.onBatchedEvents(batchEvents.values());
                }
                listener.onEvent(event);
                event = null;
                return;
            }
        }

        event = null;

        if (_DBG) {
            log.debug("{} accumulated events count={} (end of loop)", name, batchEvents.size());
        }
        if( count > 0 ) {
            listener.onBatchedEvents(batchEvents.values());
        }
    }

    public boolean isStarted() {
        return !stopped;
    }

    public boolean isStopped() {
        return stopped;
    }

    protected Thread getThread() {
        return thread;
    }
}
