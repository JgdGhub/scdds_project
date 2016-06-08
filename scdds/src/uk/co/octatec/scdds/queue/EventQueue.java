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
