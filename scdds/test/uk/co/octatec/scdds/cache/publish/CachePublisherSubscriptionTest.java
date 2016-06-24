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
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.GlobalProperties;
import uk.co.octatec.scdds.utilities.SimpleData;
import uk.co.octatec.scdds.mock.MockSession;
import uk.co.octatec.scdds.net.serialize.DefaultSerializer;
import uk.co.octatec.scdds.net.socket.BlockIO;
import uk.co.octatec.scdds.net.socket.BlockIoImpl;
import uk.co.octatec.scdds.net.socket.InitialUpdateReader;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Created by Jeromy Drake on 06/05/16
 */
public class CachePublisherSubscriptionTest {

    // test the publisher handling subscription requests
    // i.e. test the Subscription code in the Publisher
    // the actual tcp/ip connection is mocked-out

    private final Logger log = LoggerFactory.getLogger(CachePublisherSubscriptionTest.class);

    @BeforeClass
    public static void setup() {
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");
        GlobalProperties.exposeHttpServer = false;
    }

    @Test
    public void sendInitialUpdateTest() throws IOException,InterruptedException {

        // test the ServerSideCacheSubscription when it sends an InitialUpdate

        MockSession mockSession = new  MockSession();
        DefaultSerializer<String,SimpleData> serializer = new DefaultSerializer<>();
        AlwayAcceptingCacheFilter<String,SimpleData> filter = new AlwayAcceptingCacheFilter<>();
        long sessionId = 999;

        int initialUpdateMaxBatchSize = 10;
        ServerSideCacheSubscription<String,SimpleData> serverSideCacheSubscription = new ServerSideCacheSubscription<>(mockSession, serializer, filter, sessionId, initialUpdateMaxBatchSize);

        // create a map of data to represent the data in the cache -
        // used a linked hash map just to maintain the order to make
        // checking easier

        LinkedHashMap<String, SimpleData> outputMap = new  LinkedHashMap<>();
        for(int i=1; i<=12; i++){
            // put 12 items in the map to trigger 2 writes (because the match batch size is 10)
            String key = "D"+i;
            SimpleData data = new SimpleData(key, i);
            outputMap.put(key, data);
        }

        // call the method being tested...

        serverSideCacheSubscription.sendInitialUpdate(outputMap);
        for(int i=0; i<20; i++) {
            // wait for data to be written to the Mocke session
            // we expect 2 writes which are saved in the mock session in
            // 'prevLastBytetWrite' and 'lastBytetWrite'
            if( mockSession.prevLastBytetWrite != null ) {
                break;
            }
        }

        log.info("prevLastBytetWrite {}", mockSession.prevLastBytetWrite);
        log.info("lastBytetWrite {}", mockSession.lastBytetWrite);

        Assert.assertNotNull("prevLastBytetWrite not null", mockSession.prevLastBytetWrite);
        Assert.assertNotNull("lastBytetWrite not null", mockSession.lastBytetWrite);

        // copy the bytes to a new buffer minus the header...
        byte[] block1 = new byte[mockSession.prevLastBytetWrite.length- BlockIoImpl.HEADER_LENGTH] ;
        System.arraycopy(mockSession.prevLastBytetWrite, BlockIoImpl.HEADER_LENGTH, block1, 0, block1.length);

        byte[] block2 = new byte[mockSession.lastBytetWrite.length- BlockIoImpl.HEADER_LENGTH] ;
        System.arraycopy(mockSession.lastBytetWrite, BlockIoImpl.HEADER_LENGTH, block2, 0, block2.length);

        // now pass the bytes written by the cache subscriber throug the
        // InitialUpdateReader to check the cache subscriber wrote the correct format

        BlockIO bIO = mockSession.getBlockIO();

        InitialUpdateReader initialUpdateReader = new  InitialUpdateReader(bIO, serializer);

        HashMap<String, SimpleData> inputMap = new  HashMap<>();
        initialUpdateReader.doRreadInitialUpdate(inputMap, block1);

        log.info("(1)input map size after reading block1 [{}]", inputMap.size());
        log.info("(1)input map keys [{}]", inputMap.keySet());
        Assert.assertEquals("first batch of objects is the correct size", 10, inputMap.size());
        for(int i=1; i<=10; i++){
            String key = "D"+i;
            SimpleData data = inputMap.get(key);
            Assert.assertNotNull("(1)object "+key+" is present", data);
        }

        inputMap.clear();

        initialUpdateReader.doRreadInitialUpdate(inputMap, block2);

        log.info("(2)input map size after reading block2 [{}]", inputMap.size());
        log.info("(2)input map keys [{}]", inputMap.keySet());
        Assert.assertEquals("second batch of objects is the correct size", 2, inputMap.size());
        for(int i=11; i<=12; i++){
            String key = "D"+i;
            SimpleData data = inputMap.get(key);
            Assert.assertNotNull("(2)object "+key+" is present", data);
        }
    }
}
