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
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.utilities.AwaitParams;
import uk.co.octatec.scdds.utilities.SimpleCacheListener;
import uk.co.octatec.scdds.utilities.SimpleData;

/**
 * Created by Jeromy Drake on 30/04/2016.
 */
public class CacheTest {

    // test the cache, particlualt that listeners are fired

    private final Logger log = LoggerFactory.getLogger(CacheTest.class);

    @BeforeClass
    public static void setup() {
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");
    }


    @Test
    public void basicTest() {
        log.info("## basicTest");
        Cache<String, SimpleData> myCache = new  CacheImpl<>();
        myCache.put("A", new SimpleData("A", 1));
        SimpleData d = myCache.get("A");
        Assert.assertFalse("data is not null", d==null);
        Assert.assertTrue("data1 is A", d.data1.equals("A"));
        Assert.assertTrue("data2 is 1", d.data2 == 1 );

        Assert.assertFalse("isStale", myCache.isStale());
    }

    @Test
    public void initialUpdateTest() throws Exception {

        log.info("## initialUpdateTest");

        Cache<String, SimpleData> myCache = new  CacheImpl<>();
        ((CacheImpl)myCache).start();
        myCache.put("A", new SimpleData("A", 1));
        myCache.put("B", new SimpleData("B", 2));
        myCache.put("C", new SimpleData("C", 3));

        Thread.sleep(30);
            // let the above events get to the end of the queue before we add a listener

        SimpleCacheListener<String, SimpleData> cacheListener = new SimpleCacheListener<>();
        myCache.addListener(cacheListener);

        cacheListener.awaitOnInitialUpdateCount(1);


        log.info("counters stale={} active={} initial-update={} update={}",
                cacheListener.onDataStaleCount,cacheListener.onActiveCount,cacheListener.onInitialUpdateCount,cacheListener.onUpdateCount);

        Assert.assertTrue("initial update called", cacheListener.onInitialUpdateCount==1);
        Assert.assertTrue("correct active count", cacheListener.onActiveCount==1);
        Assert.assertTrue("stale count correct", cacheListener.onDataStaleCount==0);
        Assert.assertTrue("update count correct", cacheListener.onUpdateCount==0);
        Assert.assertTrue("removed count correct", cacheListener.onRemoveCount==0);

        Assert.assertTrue("initial update map correct", cacheListener.initialUpdate.size()==3);

        Assert.assertFalse("isStale", myCache.isStale());

        ((CacheImpl)myCache).dispose();
    }


    @Test
    public void removalTest() throws Exception {

        log.info("## initialUpdateTest");

        Cache<String, SimpleData> myCache = new  CacheImpl<>();
        ((CacheImpl)myCache).start();
        myCache.put("A", new SimpleData("A", 1));
        myCache.put("B", new SimpleData("B", 2));
        myCache.put("C", new SimpleData("C", 3));

        Thread.sleep(10);
        // let the above events get to the end of the queue before we add a listener

        SimpleCacheListener<String, SimpleData> cacheListener = new SimpleCacheListener<>();
        myCache.addListener(cacheListener);

        cacheListener.awaitOnInitialUpdateCount(1);

        log.info("counters stale={} active={} initial-update={} update={}",
                cacheListener.onDataStaleCount,cacheListener.onActiveCount,cacheListener.onInitialUpdateCount,cacheListener.onUpdateCount);

        Assert.assertTrue("initial update called", cacheListener.onInitialUpdateCount==1);
        Assert.assertTrue("initial update called", cacheListener.onActiveCount==1);
        Assert.assertTrue("stale count correct", cacheListener.onDataStaleCount==0);
        Assert.assertTrue("update count correct", cacheListener.onUpdateCount==0);

        Assert.assertTrue("cache size correct", myCache.size()==3);

        SimpleData removedItem = myCache.remove("A");
        log.info("removeed item [{}]", removedItem);

        cacheListener.awaitOnRemoveCount(1);

        log.info("onRemoveCount = {}", cacheListener.onRemoveCount);

        Assert.assertTrue("cache size correct", myCache.size()==2);
        Assert.assertNotNull("removedItem not null", removedItem);
        Assert.assertEquals("removedItem is the correct one", "A", removedItem.data1);

        Assert.assertEquals("removed count correct", 1, cacheListener.onRemoveCount);

        Assert.assertFalse("isStale", myCache.isStale());

        ((CacheImpl)myCache).dispose();
    }

    @Test
    public void staleAndActiveTest() throws Exception {

        log.info("## staleAndActiveTest");

        Cache<String, SimpleData> myCache = new  CacheImpl<>();
        ((CacheImpl)myCache).start();
        myCache.put("A", new SimpleData("A", 1));
        myCache.put("B", new SimpleData("B", 2));
        myCache.put("C", new SimpleData("C", 3));


        SimpleCacheListener<String, SimpleData> cacheListener = new SimpleCacheListener<>();
        myCache.addListener(cacheListener);

        cacheListener.awaitOnInitialUpdateCount(1);

        myCache.notifyStale();
        cacheListener.awaitOnRemoveCount(1);


        log.info("counters stale={} active={} initial-update={} update={}",
                cacheListener.onDataStaleCount,cacheListener.onActiveCount,cacheListener.onInitialUpdateCount,cacheListener.onUpdateCount);

        Assert.assertTrue("initial update called", cacheListener.onInitialUpdateCount==1);
        Assert.assertTrue("active cout correct", cacheListener.onActiveCount==1);
        Assert.assertTrue("stale count correct", cacheListener.onDataStaleCount==1);

        Assert.assertTrue("cache size correct", myCache.size()==3);
        Assert.assertTrue("cache is stale", myCache.isStale());

        myCache.notifyUnStale();

        cacheListener.awaitOnActiveCount(2);


        Assert.assertTrue("initial update called", cacheListener.onInitialUpdateCount==1);
        Assert.assertTrue("active count correcr", cacheListener.onActiveCount==2);
        Assert.assertTrue("stale count correct", cacheListener.onDataStaleCount==1);

        Assert.assertTrue("cache size correct", myCache.size()==3);
        Assert.assertFalse("cache is not stale", myCache.isStale());

        ((CacheImpl)myCache).dispose();
    }

    @Test
    public void initialUpdateTestListenerAddBeforeStart() throws Exception {

        log.info("## initialUpdateTestListenerAddBeforeStart");

        Cache<String, SimpleData> myCache = new  CacheImpl<>();

        ((CacheImpl)myCache).getData().put("A", new SimpleData("A", 1));
        ((CacheImpl)myCache).getData().put("B", new SimpleData("B", 1));
        ((CacheImpl)myCache).getData().put("V", new SimpleData("C", 1));
            // put the items directly in the cache without firing onUpdate - if we used the caches
            // put method, the updates would be queued and sent when the listener is attached
            // (that scenario does not arise when the cache is created using the builders)

        SimpleCacheListener<String, SimpleData> cacheListener = new SimpleCacheListener<>();
        myCache.addListener(cacheListener);

        ((CacheImpl)myCache).start();

        cacheListener.awaitOnInitialUpdateCount(1);

        log.info("counters stale={} active={} initial-update={} update={}",
                cacheListener.onDataStaleCount,cacheListener.onActiveCount,cacheListener.onInitialUpdateCount,cacheListener.onUpdateCount);

        Assert.assertTrue("initial update called", cacheListener.onInitialUpdateCount==1);
        Assert.assertTrue("correct active count", cacheListener.onActiveCount==1);
        Assert.assertTrue("stale count correct", cacheListener.onDataStaleCount==0);
        Assert.assertTrue("update count correct", cacheListener.onUpdateCount==0);
        Assert.assertTrue("removed count correct", cacheListener.onRemoveCount==0);
        Assert.assertTrue("initial update map correct", cacheListener.initialUpdate.size()==3);

        Assert.assertFalse("isStale", myCache.isStale());

        ((CacheImpl)myCache).dispose();
    }

    @Test
    public void initialUpdateTestEmptyCache() throws Exception {

        log.info("## initialUpdateTest");

        Cache<String, SimpleData> myCache = new  CacheImpl<>();
        ((CacheImpl)myCache).start();

        SimpleCacheListener<String, SimpleData> cacheListener = new SimpleCacheListener<>();
        myCache.addListener(cacheListener);

        cacheListener.awaitOnInitialUpdateCount(1);

        log.info("counters stale={} active={} initial-update={} update={}",
                cacheListener.onDataStaleCount,cacheListener.onActiveCount,cacheListener.onInitialUpdateCount,cacheListener.onUpdateCount);

        Assert.assertTrue("initial update called", cacheListener.onInitialUpdateCount==1);
        Assert.assertTrue("correct active count", cacheListener.onActiveCount==1);
        Assert.assertTrue("stale count correct", cacheListener.onDataStaleCount==0);
        Assert.assertTrue("update count correct", cacheListener.onUpdateCount==0);
        Assert.assertTrue("removed count correct", cacheListener.onRemoveCount==0);
        Assert.assertTrue("initial update map correct", cacheListener.initialUpdate.size()==0);
        Assert.assertFalse("isStale", myCache.isStale());

        ((CacheImpl)myCache).dispose();
    }

    @Test
    public void updateTest() throws Exception {

        log.info("## updateTest");

        Cache<String, SimpleData> myCache = new  CacheImpl<>();
        ((CacheImpl)myCache).start();
        SimpleCacheListener<String, SimpleData> cacheListener = new SimpleCacheListener<>();
        myCache.addListener(cacheListener);

        cacheListener.awaitOnInitialUpdateCount(1);
                // if we didn't wait here, it could be that the InitialUpdate
                // notification would be sent after one or more items below have
                // been added to the cache, so to keep the test working, there
                // is a wait - in general that situation could always arise
                // but isn't a problem, the worst that would happen is that
                // the listener receives a value twice  - once in the IjnitialUpdate
                // and once as a result of the normal update

        myCache.put("A", new SimpleData("A", 1));
        myCache.put("B", new SimpleData("B", 2));
        myCache.put("C", new SimpleData("C", 3));

        cacheListener.awaitOnUpdateCount(3);

        log.info("counters stale={} active={} initial-update={} update={} initial-update-size={}",
                cacheListener.onDataStaleCount,cacheListener.onActiveCount,cacheListener.onInitialUpdateCount,cacheListener.onUpdateCount,
                cacheListener.initialUpdate.size());


        Assert.assertTrue("initial update called", cacheListener.onInitialUpdateCount==1);
        Assert.assertTrue("correct active count", cacheListener.onActiveCount==1);
        Assert.assertTrue("stale count correct", cacheListener.onDataStaleCount==0);
        Assert.assertTrue("update count correct", cacheListener.onUpdateCount==3);
        Assert.assertTrue("removed count correct", cacheListener.onRemoveCount==0);
        Assert.assertTrue("initial update map correct", cacheListener.initialUpdate.size()==0);
        Assert.assertFalse("isStale", myCache.isStale());

        ((CacheImpl)myCache).dispose();
    }

    @Test
    public void staleTest() throws Exception {

        log.info("## staleTest");

        Cache<String, SimpleData> myCache = new  CacheImpl<>();
        ((CacheImpl)myCache).start();
        SimpleCacheListener<String, SimpleData> cacheListener = new SimpleCacheListener<>();
        myCache.addListener(cacheListener);
        myCache.put("A", new SimpleData("A", 1));
        myCache.put("B", new SimpleData("B", 2));
        myCache.put("C", new SimpleData("C", 3));

        cacheListener.awaitOnUpdateCount(3);


        log.info("update count = {}", cacheListener.onUpdateCount);
        Assert.assertTrue("update count 3", cacheListener.onUpdateCount==3);
        Assert.assertTrue("initial update map correct", cacheListener.updateAccumulation.size()==3);

        myCache.notifyStale();

        cacheListener.awaitOnDataStaleCount(1);

        log.info("stale count = {}", cacheListener.onDataStaleCount);
        Assert.assertTrue("stale count 1", cacheListener.onDataStaleCount==1);
        Assert.assertTrue("stale count correct", cacheListener.onDataStaleCount==1);
        Assert.assertTrue("removed count correct", cacheListener.onRemoveCount==0);
        Assert.assertTrue("isStale", myCache.isStale());

        ((CacheImpl)myCache).dispose();
    }

    @Test
    public void removeListenerTest() throws Exception {

        log.info("## removeListenerTest");

        Cache<String, SimpleData> myCache = new  CacheImpl<>();
        ((CacheImpl)myCache).start();
        SimpleCacheListener<String, SimpleData> cacheListener = new SimpleCacheListener<>();
        myCache.addListener(cacheListener);
        myCache.put("A", new SimpleData("A", 1));
        myCache.put("B", new SimpleData("B", 2));
        myCache.put("C", new SimpleData("C", 3));


        cacheListener.awaitOnUpdateCount(3);

        log.info("update count = {}", cacheListener.onUpdateCount);
        Assert.assertTrue("update count 3", cacheListener.onUpdateCount==3);
        Assert.assertTrue("initial update map correct", cacheListener.updateAccumulation.size()==3);

        myCache.removeListener(cacheListener);
        myCache.put("D", new SimpleData("D", 10));
        myCache.put("E", new SimpleData("E", 20));
        myCache.put("F", new SimpleData("F", 30));
                // the listener is removed so we don't expect further updated

        Thread.sleep(AwaitParams.AWAIT_SLEEP_TIME+10); // give the system chance to record the calls if any are made
                    // we are waiting for NOTHING to happen, i.e. we expect nothing to change,
                    // we just wait a while to the update count a chance to change if it is going to;
                    // (if it does change the test has failed)

        log.info("update count = {}", cacheListener.onUpdateCount);
        Assert.assertTrue("update count 3", cacheListener.onUpdateCount==3);  // hasn't changed
        Assert.assertTrue("initial update map correct", cacheListener.updateAccumulation.size()==3);

        ((CacheImpl)myCache).dispose();
    }


    @Test
    public void updateValueChangeTest() throws Exception {

        log.info("## updateValueChangeTest");

        Cache<String, SimpleData> myCache = new CacheImpl<>();
        ((CacheImpl)myCache).start();

        myCache.put("A", new SimpleData("A", 1));
        myCache.put("B", new SimpleData("B", 2));
        myCache.put("C", new SimpleData("C", 3));
        SimpleCacheListener<String, SimpleData> cacheListener = new SimpleCacheListener<>();
        myCache.addListener(cacheListener);

        cacheListener.awaitOnInitialUpdateCount(1);


        Assert.assertTrue("initial update called", cacheListener.onInitialUpdateCount==1);
        Assert.assertTrue("initial update map correct", cacheListener.initialUpdate.size()==3);

        myCache.put("C", new SimpleData("C", 30));
        myCache.put("A", new SimpleData("A", 10));
        myCache.put("A", new SimpleData("A", 100));
        myCache.put("B", new SimpleData("B", 20));

        myCache.put("D", new SimpleData("D", 40));


        cacheListener.awaitOnUpdateCountGte(4);


        log.info("update count = {}", cacheListener.onUpdateCount);
        Assert.assertTrue("update count 4", cacheListener.onUpdateCount<=8 && cacheListener.onUpdateCount>=4);
        Assert.assertTrue("initial update map correct", cacheListener.updateAccumulation.size()==4);

        SimpleData dA = cacheListener.updateAccumulation.get("A");
        SimpleData dB = cacheListener.updateAccumulation.get("B");
        SimpleData dC = cacheListener.updateAccumulation.get("C");
        SimpleData dD = cacheListener.updateAccumulation.get("D");

        Assert.assertTrue("data A check", dA.data2==100);
        Assert.assertTrue("data B check", dB.data2==20);
        Assert.assertTrue("data C check", dC.data2==30);
        Assert.assertTrue("data D check", dD.data2==40);

        ((CacheImpl)myCache).dispose();

    }



}
