package uk.co.octatec.scdds.queue;

import java.util.Collection;

/**
 * Created by Jeromy Drake on 30/04/2016.
 */
public interface EventQueueListener<K,E extends Event<K>> {
    void onEvent(E event);
    void onBatchedEvents(Collection<E> events);
}
