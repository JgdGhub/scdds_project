package uk.co.octatec.scdds.cache;
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
