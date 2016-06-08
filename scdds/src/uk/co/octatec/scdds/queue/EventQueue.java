package uk.co.octatec.scdds.queue;

/**
 * Created by Jeromy Drake on 30/04/2016.
 */
public interface EventQueue<K,E extends Event<K>> {
    /**
     * start the event queue
     * @param listener
     */
    void start(EventQueueListener<K,E> listener);

    /**
     * stop the event queue
     */
    void stop();

    /**
     * Put an item on the event queue - this method must be implemented so that it is thread-safe
     * @param event
     * @throws InterruptedException
     */
    void put(E event) throws InterruptedException;

    /**
     * determin if the event queue is started
     * @return
     */
    boolean isStarted();

    /**
     *  determin if the event queue is stopped
     * @return
     */
    boolean isStopped();
}
