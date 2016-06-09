package uk.co.octatec.scdds.cache;

import uk.co.octatec.scdds.queue.Event;
import uk.co.octatec.scdds.queue.EventQueue;
import uk.co.octatec.scdds.queue.NoneQueueingEventQueue;

/**
 * Created by Jeromy Drake on 08/06/2016.
 */
public class ListenerNoneQueuingEventQueueFactory<K,E extends Event<K>> implements ListenerEventQueueFactory<K,E>  {
    @Override
    public EventQueue<K, E> create(String name) {
        return new NoneQueueingEventQueue<>(name);
    }
}
