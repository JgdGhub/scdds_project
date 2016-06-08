package uk.co.octatec.scdds.cache;

import uk.co.octatec.scdds.queue.Event;
import uk.co.octatec.scdds.queue.EventQueue;
import uk.co.octatec.scdds.queue.EventQueueDefaultImpl;
import uk.co.octatec.scdds.queue.EventQueueFactory;

/**
 * Created by Jeromy Drake on 30/04/2016.
 */
public class ListenerEventQueueFactoryDefaultImpl<K,E extends Event<K>> implements ListenerEventQueueFactory<K,E> {
    @Override
    public EventQueue<K,E> create(String name) {
        return new EventQueueDefaultImpl<>(name);
    }
}
