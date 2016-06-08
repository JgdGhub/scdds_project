package uk.co.octatec.scdds.threading;
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
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.cache.publish.threading.*;
import uk.co.octatec.scdds.utilities.AwaitParams;

/**
 * Created by Jeromy Drake on 19/05/2016.
 */
public class RunnableQueueTest {

    private final static Logger log = LoggerFactory.getLogger(RunnableQueueTest.class);

    static class MyRunnable implements Runnable {
        final int id;
        final long startTime = System.nanoTime();
        volatile long threadId;
        MyRunnable(int id) {
            this.id = id;
        }
        volatile int count;
        @Override
        public void run() {
            long t2 = System.nanoTime();
            log.info("queue latency [{} ns]", (t2-startTime) );
            threadId = Thread.currentThread().getId();
            ++count;
        }
        void awaitRunning() throws InterruptedException{
            for(int i=0; i< AwaitParams.AWAIT_LOOP_COUNT; i++) {
               if( count == 0 ) {
                   Thread.sleep(AwaitParams.AWAIT_SLEEP_TIME);
               }
            }
        }
    }

    @Test
    public void runnableQueueDefaultImplTest() throws InterruptedException{
        RunnableQueueFactoryDefaultImpl factory = new RunnableQueueFactoryDefaultImpl();
        RunnableQueue q = factory.create();
        Assert.assertNotNull("RunnableQueue created", q);
        MyRunnable r1 = new MyRunnable(0);
        q.put(r1);
        Runnable r2 = q.take();
        Assert.assertEquals("item taken correctly", System.identityHashCode(r1), System.identityHashCode(r2));
    }

    @Test
    public void threaderTest() throws InterruptedException{

        int numberOfThreads = 3;
        Threader t = new ThreaderFactoryImpl(numberOfThreads).getInstance();
        t.start();

        MyRunnable r1 = new MyRunnable(1);
        t.run(0,r1 );
        MyRunnable r2 = new MyRunnable(2);
        t.run(1, r2);
        MyRunnable r3 = new MyRunnable(3);
        t.run(2, r3);

        r3.awaitRunning();
        r2.awaitRunning();
        r1.awaitRunning();

        Assert.assertTrue("r1 has run", r1.count == 1);
        Assert.assertTrue("r1 has run", r2.count == 1);
        Assert.assertTrue("r1 has run", r3.count == 1);

        Assert.assertTrue("r1 has run on different thread to r2", r1.threadId != r2.threadId );
        Assert.assertTrue("r1 has run on different thread to r3", r1.threadId != r3.threadId );
        Assert.assertTrue("r2 has run on different thread to r3", r2.threadId != r3.threadId );

        t.stop();
    }

}
