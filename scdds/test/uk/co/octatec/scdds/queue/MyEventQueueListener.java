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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * Created by Jeromy Drake on 13/05/2016.
 */
public class MyEventQueueListener<String> implements EventQueueListener<String, MyEvent<String>>  {

    private final Logger log = LoggerFactory.getLogger(EventQueueTest.class);

    ArrayList<MyEvent> events = new  ArrayList<>();
    HashSet<Object> eventsSet = new HashSet<>();
    int batchCallCount;
    int totalBatchedEvents;

    java.lang.String eventsAsString() {
        StringBuilder sb = new StringBuilder();
        for(MyEvent<String> e : events) {
            sb.append(e.value);
            sb.append(' ');
        }
        return sb.toString().trim();
    }

    @Override
    public void onEvent(MyEvent<String> event) {

        events.add(event);
        eventsSet.add(event.getKey());
        log.info("listener got event [{}] key=[{}] event-count={} canBeBatched?{}", event,  event.key, events.size(),event.canBeBatched);
    }

    @Override
    public void onBatchedEvents(Collection<MyEvent<String>> events) {
        ++batchCallCount;
        totalBatchedEvents += events.size();
        for(MyEvent<String> e : events ) {
            onEvent(e);
        }
    }
}