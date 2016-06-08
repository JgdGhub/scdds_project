package uk.co.octatec.scdds.queue;

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
        log.info("listener got event [{}] key=[{}] canBeBatched?{}", event, event.key, event.canBeBatched);
        events.add(event);
        eventsSet.add(event.getKey());
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