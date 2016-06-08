package uk.co.octatec.scdds.queue;


/**
 * Created by Jeromy Drake on 30/04/2016.
 */
public interface EventQueueFactory<K,E extends Event<K>> {
    /**
     *
     * @param name  - an arbitrary name
     * @return
     */
    EventQueue<K,E> create(String name);
}
