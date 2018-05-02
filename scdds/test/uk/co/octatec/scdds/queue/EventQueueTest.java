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
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.utilities.AwaitParams;

/**
 * Created by Jeromy Drake on 30/04/2016.
 */
public class EventQueueTest {

    private final Logger log = LoggerFactory.getLogger(EventQueueTest.class);


    @BeforeClass
    public static void setup() {
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO");
    }

    @Test
    public void startStopTest() throws Exception {
        log.info("## startStopTest");
        EventQueueDefaultImpl<String, MyEvent<String>> eventQueue = new EventQueueDefaultImpl<>("test");
        MyEventQueueListener<String> listener = new MyEventQueueListener<>();
        eventQueue.start(listener);
        Assert.assertFalse("event q is stopped", eventQueue.isStopped());
        Thread.sleep(AwaitParams.AWAIT_SLEEP_TIME);
        log.info("event queue thread state = {}",eventQueue.getThread().getState());
        Assert.assertTrue("thread is WAITING", eventQueue.getThread().getState()== Thread.State.WAITING);
        eventQueue.stop();
        Assert.assertTrue("event q is stopped", eventQueue.isStopped());
        Thread.sleep(AwaitParams.AWAIT_SLEEP_TIME); // time for all events to be processesed
        log.info("event queue thread state = {}",eventQueue.getThread().getState());
        Assert.assertTrue("thread is WAITING", eventQueue.getThread().getState()== Thread.State.TERMINATED);

    }

    @Test
    public void eventTest() throws Exception {
        log.info("## eventTest");
        EventQueue<String, MyEvent<String>> eventQueue = new EventQueueDefaultImpl<>("test");
        MyEventQueueListener<String> listener = new MyEventQueueListener<>();
        eventQueue.start(listener);

        MyEvent<String> e1 = new MyEvent<>("1", "A1");
        eventQueue.put(e1);
        MyEvent<String> e2 = new MyEvent<>("2", "A2");
        eventQueue.put(e2);
        MyEvent<String> e3 = new MyEvent<>("3", "A3");
        eventQueue.put(e3);
        MyEvent<String> e4 = new MyEvent<>("4", "A4");
        eventQueue.put(e4);

        for(int i=0; i<AwaitParams.AWAIT_LOOP_COUNT; i++) {
            if( listener.events.size() == 4 ) {
                break;
            }
            Thread.sleep(AwaitParams.AWAIT_SLEEP_TIME); // time for all events to be processesed
        }
        log.info("events-processed = {}", listener.events.size());

        Assert.assertTrue("q processed 4 elements", listener.events.size()==4);


        eventQueue.stop();
        Assert.assertTrue("event q is stopped", eventQueue.isStopped());

        eventQueue.stop();

    }

    @Test
    public void duplicateEventTest() throws Exception {
        log.info("## duplicateEventTest");
        EventQueueDefaultImpl<String, MyEvent<String>> eventQueue = new EventQueueDefaultImpl<>("test");
        MyEventQueueListener<String> listener = new MyEventQueueListener<>();
        eventQueue.start(listener);

        MyEvent<String> e1 = new MyEvent<>("1", "A1");
        eventQueue.put(e1);
        MyEvent<String> e2 = new MyEvent<>("2", "A2");
        eventQueue.put(e2);
        MyEvent<String> e3 = new MyEvent<>("2", "A3");
        eventQueue.put(e3);
        MyEvent<String> e4 = new MyEvent<>("2", "A4");
        eventQueue.put(e4);
        MyEvent<String> e5 = new MyEvent<>("5", "A5");
        eventQueue.put(e5);

        Thread.sleep(AwaitParams.AWAIT_SLEEP_TIME); // time for all events to be processesed
        log.info("events-processed = {}", listener.events.size());
        log.info("events in processed map: ",  listener.events);
        log.info("events in processed set: ",  listener.eventsSet);
        Assert.assertTrue("listener has 3 elements", listener.eventsSet.size()==3);

        Assert.assertTrue("q processed 3 elements", listener.events.size()>=3);
            // Assert.assertTrue("q processed 3 elements", listener.events.size()==3);
            //      this can sometimes fail depending on the speed at which items are added to the queue
            //      the queue will ensure that only one elemnt with the same key is delivered, but it won't
            //      wait for items, if the item is in the queue the its processed, but if there was a deley between
            //       adding, say "A2" and "A3", then and extra event would get delivered, this isn't an error

        eventQueue.stop();
        Assert.assertTrue("event q is stopped", eventQueue.isStopped());

        eventQueue.stop();

    }
    @Test
    public void interleavingDuplicateEventTest() throws Exception {
        log.info("## interleavingDuplicateEventTest");
        EventQueueDefaultImpl<String, MyEvent<String>> eventQueue = new EventQueueDefaultImpl<>("test");
        MyEventQueueListener<String> listener = new MyEventQueueListener<>();
        eventQueue.start(listener);

        MyEvent<String> e1 = new MyEvent<>("1", "A1");
        eventQueue.put(e1);
        MyEvent<String> e2 = new MyEvent<>("2", "A2");
        eventQueue.put(e2);
        MyEvent<String> e3 = new MyEvent<>("3", "A3");
        eventQueue.put(e3);
        Thread.sleep(AwaitParams.AWAIT_SLEEP_TIME);
        log.info("events-processed = {}", listener.events.size());
        Assert.assertTrue("q processed 3 elements", listener.events.size()==3);

        MyEvent<String> e4 = new MyEvent<>("2", "A4");
        eventQueue.put(e4);
        MyEvent<String> e5 = new MyEvent<>("5", "A5");
        eventQueue.put(e5);
        Thread.sleep(AwaitParams.AWAIT_SLEEP_TIME);
        log.info("events-processed = {}", listener.events.size());
        Assert.assertTrue("q processed 5 elements", listener.events.size()==5);

        MyEvent<String> e6 = new MyEvent<>("2", "A6");
        eventQueue.put(e6);
        MyEvent<String> e7 = new MyEvent<>("7", "A7");
        eventQueue.put(e7);
        Thread.sleep(AwaitParams.AWAIT_SLEEP_TIME);
        log.info("events-processed = {}", listener.events.size());
        Assert.assertTrue("q processed 7 elements", listener.events.size()==7);

        MyEvent<String> e8 = new MyEvent<>("2", "A8");
        eventQueue.put(e8);
        Thread.sleep(AwaitParams.AWAIT_SLEEP_TIME); // time for all events to be processesed
        log.info("events-processed = {}", listener.events.size());
        Assert.assertTrue("q processed 8 elements", listener.events.size()==8);

        eventQueue.stop();
        Assert.assertTrue("event q is stopped", eventQueue.isStopped());

        eventQueue.stop();

    }

    @Test
    public void batchingTest() throws Exception {

        log.info("## batchingTest");

        EventQueueDefaultImpl<String, MyEvent<String>> eventQueue = new EventQueueDefaultImpl<>(15, "test");
        MyEventQueueListener<String> listener = new MyEventQueueListener<>();
        eventQueue.setListener(listener);

        // don't 'start' the queue so we can fill it up and see the events drain in a batch

        for(int i=1; i<=20; i++) {
            eventQueue.put(new MyEvent<String>(Integer.toString(i), "E_"+i));
        }

        eventQueue.processEvents(); // we expect 15 of the 20 events to be processed

        log.info("events processesd(1) count = [{}}", listener.events.size() );

        Assert.assertEquals("correct number of events processed(1) ", 15, listener.events.size() );

        eventQueue.processEvents(); // finish all evengts

        log.info("events processesd(2) count = [{}}", listener.events.size() );

        Assert.assertEquals("correct number of events processed(2) ", 20, listener.events.size() );

    }

    @Test
    public void batchingSomeNullKeysTest() throws Exception {

        log.info("## batchingSomeNullKeysTest");

        int maxMatchSize = 15;

        EventQueueDefaultImpl<String, MyEvent<String>> eventQueue = new EventQueueDefaultImpl<>(maxMatchSize, "test");
        MyEventQueueListener<String> listener = new MyEventQueueListener<>();
        eventQueue.setListener(listener);

        // don't 'start' the queue so we can fill it up and see the events drain in a batch

        for(int i=1; i<=20; i++) {
            String key =  Integer.toString(i);
            if( i == 9  ) {
                        // this event will split the batch in 2, we expect 8 and then 11 items
                key = null;
            }
            eventQueue.put(new MyEvent<String>(key, "E_"+i));
        }

        log.info("queue size = [{}]", eventQueue.getQueueSize());

        Assert.assertEquals("queue size = 20", 20, eventQueue.getQueueSize());

        eventQueue.processEvents(); // we expect 15 of the 20 events to be processed  in a batch
                                    // the other events occur before 15 and soo will also get sent to the
                                    // listener, but not in a batch


        log.info("events processesd(1) count = [{}}", listener.events.size() );

        Assert.assertEquals("(1)correct number of events processed ", 9, listener.events.size() );
                                                    // 8 batched events and the one that could not be batched

        Assert.assertEquals("(1)correct number of batched events", 8, listener.totalBatchedEvents );

        eventQueue.processEvents(); // finish all events

        log.info("events processed(2) count = [{}}", listener.events.size() );

        Assert.assertEquals("(2)correct number of events processed ", 20, listener.events.size() );

        Assert.assertEquals("(2)correct number of batched events", 19, listener.totalBatchedEvents );

        String estr =  listener.eventsAsString();
        log.info("processed order of events [{}] ", estr );
        Assert.assertTrue("events processed in the expected order", estr.startsWith("E_1 E_2 E_3 E_4 E_5 E_6 E_7 E_8 E_9"));

    }

    @Test
    public void batchingSomeNullAndDuplicateKeysTest() throws Exception {

        log.info("## batchingSomeNullKeysTest");

        int maxMatchSize = 15;

        EventQueueDefaultImpl<String, MyEvent<String>> eventQueue = new EventQueueDefaultImpl<>(maxMatchSize, "test");
        MyEventQueueListener<String> listener = new MyEventQueueListener<>();
        eventQueue.setListener(listener);

        // don't 'start' the queue so we can fill it up and see the events drain in a batch

        for(int i=1; i<=20; i++) {
            String key =  Integer.toString(i);
            if(    i == 9  ) {
                // these events that don't have keys wil be processed but won't go into a batch
                key = null;
            }
            if( i == 3 || i == 7 ) {
                key = Integer.toString(2);  // no value E_3 or E_2,  but there is E_7
                                            // these 2 events get batched as they occur before i==0
                                            // so the overall number of events is reduced by 2
            }
            if( i == 12 ) {   // batching occurs but the item with key = 5 has already been
                              // processed so the number of events is not reduced . E_5
                              // and E_12 werer both deliverd
                key = Integer.toString(5);
            }

            eventQueue.put(new MyEvent<String>(key, "E_"+i));
        }

        log.info("queue size = [{}]", eventQueue.getQueueSize());

        Assert.assertEquals("queue size = 20", 20, eventQueue.getQueueSize());

        eventQueue.processEvents();

        log.info("events processed(1) count = [{}}", listener.events.size() );

        Assert.assertEquals("(1)correct number of events processed ", 7, listener.events.size() );
                            // 8 batched events and the one that could not be batched


        Assert.assertEquals("(1)correct number of batched events", 6, listener.totalBatchedEvents );

        eventQueue.processEvents(); // finish all events

        log.info("events processesd(2) count = [{}}", listener.events.size() );

        Assert.assertEquals("(2)correct number of events processed ", 18, listener.events.size() );

        Assert.assertEquals("(2)correct number of batched events", 17, listener.totalBatchedEvents );

        String estr =  listener.eventsAsString();
        log.info("processed order of events [{}] ", estr );

        Assert.assertTrue("events processed in the expected order", estr.startsWith("E_1 E_7 E_4 E_5 E_6 E_8 E_9 E_10"));

        Assert.assertFalse("no E2", estr.contains("E_2 "));
        Assert.assertFalse("no E3", estr.contains("E_3 "));
        Assert.assertTrue("contains E7", estr.contains("E_7 "));
        Assert.assertTrue("has E5", estr.contains("E_5 "));
        Assert.assertTrue("has E12", estr.contains("E_12 "));

    }


    @Test
    public void  largeEventSetTest() throws Exception {

        log.info("## largeEventSetTest");

        EventQueueDefaultImpl<String, MyEvent<String>> eventQueue = new EventQueueDefaultImpl<>("test");
        MyEventQueueListener<String> listener = new MyEventQueueListener<>();
        eventQueue.start(listener);

        int eventCount = 73;
        long t1 = System.nanoTime();
        for(int i=0; i<eventCount; i++) {
            MyEvent<String> e = new MyEvent<>("E_"+i, "Event_"+i);
            eventQueue.put(e);
        }
        for (int i = 0; i < AwaitParams.AWAIT_LOOP_COUNT; i++) {
            if (listener.events.size() == eventCount) {
                break;
            }
            Thread.sleep(AwaitParams.AWAIT_SLEEP_TIME); // time for all events to be processesed
        }
        long t2 = System.nanoTime();
        log.info("events-processed = {} time [{}]", listener.events.size(), (t2-t1));
        Assert.assertEquals("all events received ", eventCount, listener.events.size());

        for(int i=0; i<eventCount; i++) {
            MyEvent<String> e = new MyEvent<>("E_"+i, "Event_"+i);
            Assert.assertTrue("event was received "+e, listener.events.contains(e));
        }
    }
}
