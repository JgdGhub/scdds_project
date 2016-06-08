package uk.co.octatec.scdds.cache.publish;

import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.utilities.SimpleData;
import uk.co.octatec.scdds.mock.MockCachePublisher;
import uk.co.octatec.scdds.net.socket.BlockIO;
import uk.co.octatec.scdds.net.socket.ClientConnector;
import uk.co.octatec.scdds.net.socket.ClientConnectorImpl;
import uk.co.octatec.scdds.net.socket.Session;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Jeromy Drake on 07/05/16
 */
public class GeneralRequestHandlerTest {

    // test the GeneralRequestHandler - this runs in the server and processes messages such as
    // 'heartbeat(ping)' requests, 'un-subscribe' requests and 'load-query; requests

    // an actual tcp/ip connection is made to a GeneralRequestHandler  -
    // a MockCachePublisher is used bu this just simplies the tcp/ip connection

    private final  Logger log = LoggerFactory.getLogger(GeneralRequestHandlerTest.class);

    static class RequestClient {
        private static Logger log = LoggerFactory.getLogger(RequestClient.class);

        public RequestClient(Session sc, long sessionId) {
            log.info("RequestClient [{}] sessionId=[{}]", sc, sessionId);
            this.sc = sc;
            this.sessionId = sessionId;
        }

        Session sc;
        long sessionId;
        void sendPingRequest() throws IOException{
            log.info("send ping request");
            sc.getBlockIO().writeString(PropertyUtils.createPropertyString(CachePublisher.ARG_REQUEST, CachePublisher.RQST_HEARTBET,
                    CachePublisher.ARG_SESSION_ID, sessionId, CachePublisher.ARG_TIMESTAMP, System.currentTimeMillis()));
        }
        void sendUnsubscribe() throws IOException{

            String rqst = PropertyUtils.createPropertyString(CachePublisher.ARG_REQUEST, CachePublisher.RQST_UNSUBSCRIBE,
                    CachePublisher.ARG_SESSION_ID, sessionId, CachePublisher.ARG_TIMESTAMP, System.currentTimeMillis());
            log.info("send unsubscribe request [{}]",rqst);
            sc.getBlockIO().writeString(rqst);
        }
        byte[] readResponse() throws IOException {
            BlockIO bIO = sc.getBlockIO();
            BlockIO.Header hdr = bIO.readBlockHeader();
            log.info("fread header [{}]", hdr);
            byte[] buff = bIO.readRestOfBlock();
            return buff;
        }
    }

    static class RequestLoop extends Thread {
        private static Logger log = LoggerFactory.getLogger(RequestLoop.class);
        RequestClient requestClient;
        int count;
        volatile Exception exception;
        volatile ArrayList<byte[]> responses = new ArrayList<>();
        static RequestLoop start(RequestClient requestClient, int count)  {
            RequestLoop requestLoop = new RequestLoop(requestClient, count);
            requestLoop.start();
            return requestLoop;
        }
        void await() throws InterruptedException{
            for(int i=0; i<60; i++) {
                if( responses.size() == count ) {
                    break;
                }
                else {
                    Thread.sleep(20);
                }
            }
        }
        RequestLoop(RequestClient requestClient, int count) {
            this.requestClient = requestClient;
            this.count = count;
        }
        public void run() {
            log.info("RequestLoop start cout={}", count);
            try {
                for (int i = 0; i < count; i++) {
                    requestClient.sendPingRequest();
                    byte[] response = requestClient.readResponse();
                    responses.add(response);
                    Thread.sleep(1);

                }
                log.info("RequestLoop done");
            }
            catch( Exception e) {
                log.error("exception in RequestLoop", e);
                exception = e;
            }
        }

    }

    @BeforeClass
    public static void setup() {
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");
    }

    @Test
    public void multipleResponseTest() throws Exception {

        // test that the GeneralRequestHandler can receive multiple requests from multiple clients
        // and respond correctly

        // the GeneralRequestHandler handles things like heartbeat-requests and load-requests, there is one
        // single request handler only in an application, it uses NIO/Select to handle all input requests

        log.info("##generalTest");

        GeneralRequestHandlerImpl generalRequestHandler = new GeneralRequestHandlerImpl();

        MockCachePublisher<String, SimpleData> cahePublisher = new MockCachePublisher<String, SimpleData>(generalRequestHandler);
                                                        // simplified version of the CachePublisher for this test

        int port = cahePublisher.initializePort();
        log.info("server-port=[{}]", port);
        cahePublisher.start();
        boolean started = cahePublisher.waitForStart();
        Assert.assertTrue("server has started", started);

        // connect to the mock CachePublisher with a client, that will pass the client on
        // to the GeneralRequestHandler to listen for requests from the client

        long sessionId1 = 0;

        ClientConnector connector1 = new ClientConnectorImpl("GenRqTest");
        Session sc1 = connector1.connect(ClientConnector.BLOCKING_MODE, "localhost", port, 1, 1);
        Assert.assertNotNull("connection made to server", sc1);
        BlockIO bIO1 = sc1.getBlockIO();
        bIO1.writeString("START-ONE");

        cahePublisher.awaitSessionIdGt(sessionId1);

        sessionId1 = cahePublisher.getLastSessionId();
        log.info("sessionId1 = [{}]", sessionId1);
        Assert.assertTrue("session1 allocated by MockCachePublisher ", sessionId1 > 0);

        // send a Heartbeat request to the GeneralRequestHandler and read its response

        RequestClient rqstClient1 = new RequestClient(sc1, sessionId1);
        rqstClient1.sendPingRequest();
        byte[] b = rqstClient1.readResponse();
        Assert.assertEquals("(1.1)ping response correct", b[0], BlockIO.HEARTBEAT_ARG);

        // connect to the mock CachePublisher with a 2nd client, that will pass the 2nd client on
        // to the GeneralRequestHandler to listen for requests from the client

        long sessionId2 = 0;

        ClientConnector connector2 = new ClientConnectorImpl("GenRqTest");
        Session sc2 = connector2.connect(ClientConnector.BLOCKING_MODE, "localhost", port, 1, 1);
        Assert.assertNotNull("connection made to server", sc2);
        BlockIO bIO2 = sc2.getBlockIO();
        bIO2.writeString("START-TWO");

        cahePublisher.awaitSessionIdGt(sessionId1);

        sessionId2 = cahePublisher.getLastSessionId();
        log.info("sessionId2 = [{}]", sessionId2);
        Assert.assertTrue("session2 allocated by MockCachePublisher ", sessionId2 > sessionId1);

        // send a Heartbeat request from the 2nd client to the GeneralRequestHandler and read its response

        RequestClient rqstClient2 = new RequestClient(sc2, sessionId2);
        rqstClient2.sendPingRequest();
        b = rqstClient2.readResponse();
        Assert.assertEquals("(1.2)ping response correct", b[0], BlockIO.HEARTBEAT_ARG);

        // creatae 2 threads and send multiple requests to the GeneralRequestHandler, save its responses and
        // then check them...

        RequestLoop requestLoop1 = RequestLoop.start(rqstClient1, 10);
        RequestLoop requestLoop2 = RequestLoop.start(rqstClient2, 10);
        requestLoop1.await();
        requestLoop2.await();

        log.info("request-loops finished size1=[{}] size2=[{}]", requestLoop1.responses.size(), requestLoop2.responses.size());
        Assert.assertEquals("responses-2 size correct", 10, requestLoop1.responses.size());
        Assert.assertEquals("responses-2 size correct", 10, requestLoop2.responses.size());
        for(byte[] rsp1 : requestLoop1.responses ) {
            Assert.assertEquals("ping response-1 set correct", rsp1[0], BlockIO.HEARTBEAT_ARG);
        }
        for(byte[] rsp2 : requestLoop2.responses ) {
            Assert.assertEquals("ping response-2 set correct", rsp2[0], BlockIO.HEARTBEAT_ARG);
        }

        sc1.close();

        generalRequestHandler.unRegisterSession(sessionId1);

        rqstClient2.sendPingRequest();
        b = rqstClient2.readResponse();
        Assert.assertEquals("(2.2)ping response correct", b[0], BlockIO.HEARTBEAT_ARG);

        rqstClient2.sendUnsubscribe();

        cahePublisher.awaitRemovedSessionIdGt(0);

        log.info("removed session id = {}",  cahePublisher.removedSessionId) ;
        Assert.assertEquals("removed session id is correct", sessionId2, cahePublisher.removedSessionId);

        sc2.close();
    }

}
