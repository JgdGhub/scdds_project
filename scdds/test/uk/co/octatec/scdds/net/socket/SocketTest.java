package uk.co.octatec.scdds.net.socket;
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
import uk.co.octatec.scdds.net.serialize.DefaultSerializer;
import uk.co.octatec.scdds.net.serialize.Serializer;

import java.io.Serializable;
import java.net.BindException;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

/**
 * Created by Jeromy Drake on 30/04/2016.
 */
public class SocketTest {

    private final Logger log = LoggerFactory.getLogger(SocketTest.class);

    private static final int TEST_PORT_SERVER = 9999;

    private static final int TEST_PORT_CLIENT = 9998;

    private static final int TEST_BYTE_LEN = 32;  // at 1_024_000, multiple reads and writes occur on windows


    private static class TestObject implements Serializable {
        Date date;
        long l;
        String s;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TestObject that = (TestObject) o;

            if (l != that.l) return false;
            if (!date.equals(that.date)) return false;
            return s.equals(that.s);

        }

        @Override
        public int hashCode() {
            int result = date.hashCode();
            result = 31 * result + (int) (l ^ (l >>> 32));
            result = 31 * result + s.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "TestObject{" +
                    "date=" + date +
                    ", l=" + l +
                    ", s='" + s + '\'' +
                    '}';
        }
    }

    private class TestServer implements Runnable {
        final CountDownLatch clientDone = new CountDownLatch(1);
        final CountDownLatch serverStarted = new CountDownLatch(1);
        final byte[] testData = new byte[TEST_BYTE_LEN];
        final int testInt = 123456789;
        final TestObject testObject = new TestObject();
        final String testString = "This is a test string!";
        volatile int port;
        @Override
        public void run() {
            try {
                String key = "Key.1";
                testObject.date = new Date();
                testObject.l = System.nanoTime();
                testObject.s = "Test Object";
                for(int i=0; i<TEST_BYTE_LEN; i++) {
                    testData[i] = (byte)i;
                }
                ServerSession srv = new ServerSessionImpl("TestSrv");
                srv.start();
                port = srv.getPort();
                serverStarted.countDown();
                log.info("test server started, port = {}", port);
                Session sd = srv.acceptNoneBlockingClient();
                log.info("client connected {}", sd);
                sd.write(testData, 0, TEST_BYTE_LEN);
                log.info("test server written data, will write testInt [{}]", testInt);
                sd.writeInt(testInt);
                log.info("test int written, about to write test-object");
                DefaultSerializer<String,TestObject> ser = new DefaultSerializer<>();
                byte[] b = ser.serialize(key, testObject, 0);
                BlockIO bIO = sd.getBlockIO();
                bIO.writeDataBlock(b, 0, b.length);
                log.info("test object written");
                bIO.writeString(testString);
                log.info("test string written");
                clientDone.await();
                log.info("client test completed");
            }
            catch( Exception e) {
                log.error("Exception From Test Server", e);
                serverStarted.countDown();
                port = -1;
            }
        }
    }

    @BeforeClass
    public static void setup() {

        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");
    }

    @Test
    public void ephemeralPortAllocationTest() throws Exception {
        log.info("## portAllocationTest ");
        ServerSession srv = new ServerSessionImpl("TestSrv");
        srv.start();
        int port = srv.getPort();
        log.info("port = {}", port);
        Assert.assertTrue("port number ", port>0 );
    }

    @Test
    public void specificPortTest() throws Exception {
        log.info("## specificPortTest ");
        ServerSession srv = new ServerSessionImpl("TestSrv");
        int testPort;
        boolean caughtBindException = false;
        // try and find a free port on the system to run the test against - this is just to test passing a port into
        // the start method, ServerSession will use an ephemeral port if none is specified in the method
        // see: ephemeralPortAllocationTest
        for(testPort = TEST_PORT_SERVER; testPort <= TEST_PORT_SERVER+20; testPort++) {
            try {
                srv.start(testPort);
                caughtBindException = false;
                break;
            } catch (BindException x) {
                log.warn("bind exception in test: {} testPort={}", x.getMessage(), testPort);
                caughtBindException = true;
            }
        }
        if( !caughtBindException ) {
            int port = srv.getPort();
            log.info("port set in server session = {}", port);
            Assert.assertEquals("port number ", port, testPort);
        }
        else {
            log.warn("couldn't find a free port to run 'portSetTest' against, test allowed to pass");
        }
    }

    @Test
    public void connectTestWithNoServer() {
        log.info("## connectTestWithNoServer");
        ClientConnectorImpl clientSession = new ClientConnectorImpl("TestCli");
        Session session = null;
        try {
            int port = TEST_PORT_CLIENT;
            session = clientSession.connect(ClientConnector.NONE_BLOCKING_MODE, "localhost", port, 1, 2);
            log.error("unexpectedly can connect - there must be some server using port {} on this box", port);
        }
        catch(Exception e) {
            log.info("exception expected, can't connect [{}] ",e.getMessage());
        }
        Assert.assertNull("connection cannot be made when there is no sever", session);
    }

    @Test
    public void connectAndReadWriteTest() throws Exception{

        log.info("## connectAndReadWriteTest");

        // start a test server

        log.info("# make test connection");
        TestServer testServer = new TestServer();
        Thread t = new Thread(testServer);
        t.start();
        testServer.serverStarted.await();
        log.info("test server connected on port=[{}]", testServer.port);

        // connect to the test server

        ClientConnectorImpl clientSession = new ClientConnectorImpl("TestCli");
        Session session = null;
        try {
            session = clientSession.connect(ClientConnector.NONE_BLOCKING_MODE, "localhost", testServer.port, 5, 2);
            log.info("connected to test server [{}]", session.getAddress());
        }
        catch(Exception e) {
            log.error("exception - can't connect [{}] ",e);
        }

        Assert.assertNotNull("connection made to test server", session);

        // read data

        log.info("#read byte array");

        byte[] b = new byte[TEST_BYTE_LEN];
        log.info("about to read byte[{}]...",TEST_BYTE_LEN);
        session.read(b, 0, TEST_BYTE_LEN);


        log.info("...byte[{}] read {} ",TEST_BYTE_LEN, b.length);

        for(int i=0; i<TEST_BYTE_LEN; i++) {
            if( b[i] != testServer.testData[i] ) {
                log.error("test data mismatch at pos=[{}] got [{}] wanted [{}]", i, b[i], testServer.testData[i]);
            }
            Assert.assertEquals("byte["+i+"]", b[i], testServer.testData[i]);
        }

        log.info("# read single int");

        log.info("about to read testInt...");
        int n = session.readInt();
        log.info("... read testInt [{}]", n);
        Assert.assertEquals("test-int", n, testServer.testInt);


        log.info("# read object");

        log.info("about to read object data block...");
        BlockIO bIO = session.getBlockIO();
        b = bIO.readBlock();
        DefaultSerializer<String,TestObject> ser = new DefaultSerializer<>();
        Serializer.Pair<String, TestObject> pair =  ser.deserialize(b, 0);
        TestObject testObject = pair.value;
        log.info("object read [{}]", testObject);
        Assert.assertNotNull("object not null", testObject);
        Assert.assertEquals("object correct", testObject, testServer.testObject);

        log.info("# read string");
        String testString = bIO.readString();
        log.info("string read [{}]", testString);
        Assert.assertNotNull("string not null", testString);
        Assert.assertEquals("string correct", testString, testServer.testString);


        session.close();

        testServer.clientDone.countDown();
    }

}
