package uk.co.octatec.scdds.cache.publish;
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
import static uk.co.octatec.scdds.ConditionalCompilation._DBG;

import uk.co.octatec.scdds.GlobalDefaults;
import uk.co.octatec.scdds.GlobalProperties;
import uk.co.octatec.scdds.cache.publish.threading.Threader;
import uk.co.octatec.scdds.net.serialize.Serializer;
import uk.co.octatec.scdds.net.socket.BlockIO;
import uk.co.octatec.scdds.net.socket.InitialUpdateWriter;
import uk.co.octatec.scdds.net.socket.Session;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;

import static uk.co.octatec.scdds.ConditionalCompilation._LOG_LATENCY;

/**
 * Created by Private on 30/04/2016.
 *
 * This is used to keep track of subscriptions in the server.
 */
public final class ServerSideCacheSubscription<K,T> {

    private final Logger log = LoggerFactory.getLogger(ServerSideCacheSubscription.class);

    final static AlwayAcceptingCacheFilter ALWAYS_ACCEPTING_FILTER = new AlwayAcceptingCacheFilter();

    enum ScheduledTaskAction { send, sendBatch, sendRemoved, sendDataStale, sendInitialUpdate }

    private final class ScheduledTask implements Runnable {

        ScheduledTaskAction action;
        K key;
        T value;
        byte[] data;
        boolean isStale;
        Map<K,T> initialData;
        long timestamp = System.nanoTime();

        @Override
        public void run() {
            try {
                if( _LOG_LATENCY ) {
                    long t2 = System.nanoTime();
                    GlobalProperties.llog_3.info("!!(3) time to receive scheduled send message [{} ns]", (t2-timestamp));
                }
                switch (action) {
                    case send:
                        doSend(key, value, data);
                        break;
                    case sendBatch:
                        doSendBatchUpdate(data);
                        break;
                    case sendInitialUpdate:
                        doSendInitialUpdate(initialData);
                        break;
                    case sendRemoved:
                        doSendRemoved(key, value, data);
                        break;
                    case sendDataStale:
                        doSendDataStale(isStale);
                        break;
                }
            }
            catch( IOException e) {
                log.error("error processing ScheduledTask action=[{}] key=[{}] value=[{}], will end client connection", key, value, e, action);
                if( cachePublisher == null ) {
                    log.error("internal setup error - cachePublisher is null so can't notify it of failed client connection");
                }
                else {
                    cachePublisher.onFailedClient(ServerSideCacheSubscription.this);
                }
            }
            catch( Exception e) {
                log.error("error processing ScheduledTask key=[{}] value=[{}]", key, value, e);
            }
        }

        @Override
        public String toString() {
            return "Task{"+key+":"+action+"}";
        }
    }

    private final Session sc;
    private final BlockIO bIO;
    private final Serializer<K,T> serializer;
    private final CacheFilter<K,T> filter;
    private final long sessionId;

    static final int DFLT_MAX_INITIAL_UPDATE_BLOCK = GlobalDefaults.BATCH_SIZE;;

    private final int  maxInitialBlockSize;

    private final Threader threader;
    private final int threadId;
    private final CachePublisher<K,T> cachePublisher; // needed for reporting client socket connection errors to the publisher when sending in a thread

    public ServerSideCacheSubscription(Session sc, Serializer<K,T> serializer, CacheFilter<K,T> filter, long sessionId) {
        this.sc = sc;
        bIO = sc.getBlockIO();
        this.serializer = serializer;
        this.filter = filter;
        this.sessionId = sessionId;
        this.maxInitialBlockSize =  DFLT_MAX_INITIAL_UPDATE_BLOCK;
        this.threader = null;
        this.threadId = 0;
        this.cachePublisher = null;
    }

    public ServerSideCacheSubscription(Session sc, Serializer<K,T> serializer, CacheFilter<K,T> filter, long sessionId, Threader threader, CachePublisher<K,T> cachePublisher) {
        this.sc = sc;
        bIO = sc.getBlockIO();
        this.serializer = serializer;
        this.filter = filter;
        this.sessionId = sessionId;
        this.maxInitialBlockSize =  DFLT_MAX_INITIAL_UPDATE_BLOCK;
        this.threader = threader;
        this.threadId =  threader==null ? 0 : (int)(sessionId%threader.getNumberOfThreads());
        this.cachePublisher = cachePublisher;
    }

    public ServerSideCacheSubscription(Session sc, Serializer<K,T> serializer, CacheFilter<K,T> filter, long sessionId, int maxInitialBlockSize) {
        this.sc = sc;
        bIO = sc.getBlockIO();
        this.serializer = serializer;
        this.filter = filter;
        this.sessionId = sessionId;
        this.maxInitialBlockSize =  maxInitialBlockSize;
        this.threader = null;
        this.threadId = 0;
        this.cachePublisher = null;
    }

    public ServerSideCacheSubscription(Session sc, Serializer<K,T> serializer, CacheFilter<K,T> filter, long sessionId, int maxInitialBlockSize, Threader threader, CachePublisher<K,T> cachePublisher) {
        this.sc = sc;
        bIO = sc.getBlockIO();
        this.serializer = serializer;
        this.filter = filter;
        this.sessionId = sessionId;
        this.maxInitialBlockSize =  maxInitialBlockSize;
        this.threader = threader;
        this.threadId =  threader==null ? 0 : (int)(sessionId%threader.getNumberOfThreads());
        this.cachePublisher = null;
    }

    public long getSessionId() {
        return sessionId;
    }


    public InetSocketAddress getAddress() {
        return sc.getAddress();
    }

    public void dispose() {
        sc.close();
    }

    public boolean isUnfiltered() {
        return filter == ALWAYS_ACCEPTING_FILTER;
    }

    void send(K key, T value, ByteBuffer data) throws IOException{
        if( filter.accept(key, value)) {
            //bIO.writeDataBlock_HeaderSpaceIncluded(data);
            long t1;
            if( _LOG_LATENCY ) {
                t1 = System.nanoTime();
            }
            sc.write(data);
            if( _LOG_LATENCY ) {
                long t2 = System.nanoTime();
                log.info("!!(2)time to write send (bb) [{}] [{} ns]", data.limit(), (t2-t1));
            }
        }
    }

    void send(K key, T value, byte[] data) throws IOException, InterruptedException{
        if( filter.accept(key, value)) {
            if (threader == null) {
                doSend(key, value, data);
            } else {
                doSendScheduled(key, value, data);
            }
        }
    }

    private void doSendScheduled(K key, T value, byte[] data) throws InterruptedException {

        ScheduledTask scheduledTask = new ScheduledTask();
        scheduledTask.action = ScheduledTaskAction.send;
        scheduledTask.key = key;
        scheduledTask.value = value;
        scheduledTask.data = data;
        threader.run(threadId, scheduledTask);

    }

    private void doSend(K key, T value, byte[] data) throws IOException {
        try {
            if (_DBG) {
                log.debug(">>send [{}]", key);
            }
            long t1;
            if (_LOG_LATENCY) {
                t1 = System.nanoTime();
            }
            sc.awaitWriteReady(); // doSend
            //bIO.writeDataBlock_HeaderSpaceIncluded(data, 0, data.length);
            sc.write(data, 0, data.length);
            if (_LOG_LATENCY) {
                long t2 = System.nanoTime();
                log.info("!!(2)time to write send [{}] [{} ns]", data.length, (t2 - t1));
            }
        }
        catch( RuntimeException e) {
            log.error("failed to send key=[{}] value=[{}] data-length=[{}] session-id=[{}]", key, value, data.length, sessionId);
            log.error("RuntimeException [{}] ", e.getMessage(), e);
        }

    }

    void sendBatchUpdate(byte[] batchBuff )  throws InterruptedException, IOException{
        if (threader == null) {
            doSendBatchUpdate(batchBuff);
        } else {
            doSendBatchUpdateScheduled(batchBuff);
        }
    }

    private void doSendBatchUpdateScheduled(byte[] batchBuff) throws InterruptedException {

        ScheduledTask scheduledTask = new ScheduledTask();
        scheduledTask.action = ScheduledTaskAction.sendBatch;  // BUG ScheduledTaskAction.sendBatch;
        scheduledTask.data = batchBuff;
        threader.run(threadId, scheduledTask);

    }

    private void doSendBatchUpdate(byte[] batchBuff) throws IOException {
        if (_DBG) {
            log.debug(">>sendBatchUpdate length=[{}]", batchBuff.length);
        }

        long t1;
        if (_LOG_LATENCY) {
            t1 = System.nanoTime();
        }
        sc.awaitWriteReady(); // doSend
        sc.write(batchBuff, 0, batchBuff.length);
        if (_LOG_LATENCY) {
            long t2 = System.nanoTime();
            GlobalProperties.llog_2.info("!!(2)time to write sendBatch [{}] [{} ns]", batchBuff.length, (t2 - t1));
        }
    }

    void sendRemoved(K key, T value, byte[] data) throws IOException, InterruptedException{
        if( filter.accept(key, value)) {
            if (threader == null) {
                doSendRemoved(key, value, data);
            } else {
                doSendRemovedScheduled(key, value, data);
            }
        }
    }

    private void doSendRemoved(K key, T value, byte[] data) throws IOException {
        if (_DBG) {
            log.debug(">>sendRemoved [{}]", key);
        }
        long t1;
        if (_LOG_LATENCY) {
            t1 = System.nanoTime();
        }
        sc.awaitWriteReady(); // doSendRemoved
        bIO.writeRemovalBlock_HeaderSpaceIncluded(data, 0, data.length);
        if (_LOG_LATENCY) {
            long t2 = System.nanoTime();
            GlobalProperties.llog_2.info("!!(2)time to write sendRemoved[{} ns]", (t2 - t1));
        }
    }

    private void doSendRemovedScheduled(K key, T value, byte[] data) throws InterruptedException {
        ScheduledTask scheduledTask = new ScheduledTask();
        scheduledTask.action = ScheduledTaskAction.sendRemoved;
        scheduledTask.key = key;
        scheduledTask.value = value;
        scheduledTask.data = data;
        threader.run(threadId, scheduledTask);
    }

    void sendDataStale(boolean isStale) throws IOException, InterruptedException{

        if( threader==null ) {
            doSendDataStale(isStale);
        }
        else {
            doSendDataStaleScheduled(isStale);
        }
    }

    private void doSendDataStale(boolean isStale) throws IOException {
        if( _DBG ) {
            log.debug(">>sendDataStale [{}]", isStale);
        }
        long t1;
        if( _LOG_LATENCY ) {
            t1 = System.nanoTime();
        }
        sc.awaitWriteReady(); //doSendDataStale
        if( isStale ) {
            bIO.sendStaleNotification();
        }
        else {
            bIO.sendActiveNotification();
        }
        if( _LOG_LATENCY ) {
            long t2 = System.nanoTime();
            GlobalProperties.llog_2.info("!!(2)time to write sendDataStale[{} ns]", (t2-t1));
        }
    }

    private void doSendDataStaleScheduled(boolean isStale)throws InterruptedException {
        ScheduledTask scheduledTask = new ScheduledTask();
        scheduledTask.action = ScheduledTaskAction.sendDataStale;
        scheduledTask.isStale = isStale;
        threader.run(threadId, scheduledTask);
    }


    void sendInitialUpdate(Map<K,T> initialData) throws IOException, InterruptedException{
        if( threader==null ) {
            doSendInitialUpdate(initialData);
        }
        else {
            doSendInitialUpdateScheduled(initialData);
        }
    }

    private void doSendInitialUpdate(Map<K,T> initialData) throws IOException{

        log.info(">>sendInitialUpdate map-size=[{}]", initialData.size());

        InitialUpdateWriter initialUpdateWriter = new InitialUpdateWriter();

        initialUpdateWriter.prepareInitialUpdateMessage();

        Set<Map.Entry<K,T>> set = initialData.entrySet();
        int count = 0;
        for(Map.Entry<K,T> entry : set) {
            K key = entry.getKey();
            T value = entry.getValue();
            if( filter.accept(key, value) ) {

                initialUpdateWriter.addInitialUpdateEntry(serializer.serialize(key, value, 0));
                ++count;
                if (count == maxInitialBlockSize) {
                    byte[] buff = initialUpdateWriter.getInitialUpdateMessage();
                    int len = initialUpdateWriter.getInitialUpdateMessageLength();
                    long t1;
                    if (_LOG_LATENCY) {
                        t1 = System.nanoTime();
                    }
                    sc.awaitWriteReady(); // doSendInitialUpdate
                    log.info(">>write initial-update byte-count=[{}] item-count=[{}]", len, count);
                    sc.write(buff, 0, len);
                    if (_LOG_LATENCY) {
                        long t2 = System.nanoTime();
                        GlobalProperties.llog_2.info("!!(2)time to write sending initial update [{} ns]", (t2 - t1));
                    }

                    initialUpdateWriter.prepareInitialUpdateMessage();
                    count = 0;
                }

            }
        }
        initialUpdateWriter.completeInitialUpdateMessage();
        byte[] buff = initialUpdateWriter.getInitialUpdateMessage();
        int len = initialUpdateWriter.getInitialUpdateMessageLength();


        long t1;
        if (_LOG_LATENCY) {
            t1 = System.nanoTime();
        }
        sc.awaitWriteReady(); // doSendInitialUpdate
        log.info(">>write final initial-update byte-count=[{}] item-count=[{}]", len, count);
        sc.write(buff, 0, len);
        if (_LOG_LATENCY) {
            long t2 = System.nanoTime();
            GlobalProperties.llog_2.info("!!(2)time to write sending final initial update [{}] [{} ns]", len, (t2-t1));
            }

    }

    void doSendInitialUpdateScheduled(Map<K,T> initialData) throws InterruptedException{
        ScheduledTask scheduledTask = new ScheduledTask();
        scheduledTask.action = ScheduledTaskAction.sendInitialUpdate;
        scheduledTask.initialData = initialData;
        threader.run(threadId, scheduledTask);
    }

}
