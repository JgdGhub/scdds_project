package uk.co.octatec.scdds.cache.publish;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.net.socket.BlockIO;
import uk.co.octatec.scdds.net.socket.Session;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
/**
 * Created by Jeromy Drake on 04/05/16
 *
 * There is a single instance of this class per process, it listens for
 * 'general' requests, i.e. requests for a heartbeat and requests to check the
 * number of active subscriptions
 */

public class GeneralRequestHandlerImpl implements GeneralRequestHandler {

    private final Logger log = LoggerFactory.getLogger(GeneralRequestHandlerImpl.class);
    private final ConcurrentMap<Long, ClientConnection> connections = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, String> servers = new ConcurrentHashMap<>();
    private Selector selector;

    volatile boolean threadRunning;
    volatile ClientConnection connectionToRegister;

    Thread readThread = null;

    private static class ClientConnection {
        final long sessionId;
        final CachePublisher cachePublisher;
        final Session sc;

        SelectionKey selectionKey;

        public ClientConnection(long sessionId, CachePublisher cachePublisher, Session sc) {
            this.sessionId = sessionId;
            this.cachePublisher = cachePublisher;
            this.sc = sc;
        }

        public String toString() {
            return  "sessionId="+sessionId+";"+"cache="+cachePublisher.getCacheName();
        }
    }

    public GeneralRequestHandlerImpl() {
        try {
            selector = SelectorProvider.provider().openSelector();
        }
        catch( IOException x) {
            log.error("GeneralRequestHandlerImpl constructor got exception creating Selector", x);
        }
    }

    @Override
    public void registerSession(long sessionId, CachePublisher cachePublisher, Session sc) {

        log.info("registerSession sessionId=[{}] sc=[{}] cache=[{}]", sessionId, sc, cachePublisher.getCacheName());

        ClientConnection connection = new ClientConnection(sessionId, cachePublisher, sc);
        connections.put(sessionId, connection);

        synchronized (this) {
            // this will not be called very often...
            if (!threadRunning) {
                SelectionKey selectionKey = sc.registerSelector(selector, SelectionKey.OP_READ, connection);
                connection.selectionKey = selectionKey;
                log.info("start GeneralRequestHandlerImpl main loop");
                readThread = new Thread() {
                    public void run() {
                        mainLoop();
                    }
                };
                readThread.setDaemon(true);
                readThread.setName("GeneralRequestHandler");
                threadRunning = true;
                readThread.start();
            }
            else {
                connectionToRegister = connection;
                selector.wakeup();
            }
        }
    }

    @Override
    public void unRegisterSession(long sessionId) {
        try {
            ClientConnection connection = connections.remove(sessionId);
            log.info("unRegisterSession {} connection=[{}]", sessionId, connection);
            if( connection != null ) {
                connection.selectionKey.cancel();
                connection.selectionKey.attach(null);
                connection.sc.close();
            }
        }
        catch( RuntimeException e) {
            log.error("exception from unRegisterSession sessionId=[{}]", sessionId);
        }
    }

    private void mainLoop() {
        try {
            log.info("general request handler in main loop");
            while (threadRunning) {
                int n = selector.select();
                if( connectionToRegister != null ) {
                    log.info("delayed registration of selector for [{}]", connectionToRegister);
                    SelectionKey selectionKey = connectionToRegister.sc.registerSelector(selector, SelectionKey.OP_READ ,connectionToRegister);
                    connectionToRegister.selectionKey = selectionKey;
                    connectionToRegister = null;
                }
                if( n == 0 ) {
                    log.warn("selected: zero return from select");
                    continue;
                }
                Set<SelectionKey> keySet = this.selector.selectedKeys();
                log.info("selected: number of connections ready for read = [{}]", keySet.size());
                Iterator keyIterator  = keySet.iterator();
                int readyCount=0;
                while( keyIterator.hasNext() ) {
                    ++readyCount;
                    log.info("readyCount this iteration [{}]",readyCount);
                    SelectionKey key = (SelectionKey) keyIterator.next();
                    keyIterator.remove();
                    if (!key.isValid()) {
                        continue;
                    }
                    if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
                        ClientConnection connection = (ClientConnection) key.attachment();
                        if (connection == null) {
                            // this can't happen!
                            log.error("null connection attachment on key [{}]", key);
                            key.cancel();
                            continue;
                        }
                        try {
                            BlockIO bIO = connection.sc.getBlockIO();
                            String request = bIO.readString();
                            log.info("read request [{}] from session-id [{}] for cache [{}]", request, connection.sessionId, connection.cachePublisher.getCacheName());
                            Properties properties = PropertyUtils.getPropertiesFromString(request);
                            processRequest(properties, bIO);

                        } catch (Exception e) {
                            log.error("exception while processing connection [{}]", connection, e);
                            key.cancel();
                        }
                    }
                    else {
                        log.warn("unexpected ready operation [{}]", key.readyOps());
                    }
                    log.info("end of selection processing");
                }
            }
        }
        catch( IOException e ) {
            log.error("IOException from GeneralREquestHandler maim-loop, thread will exit", e);
            threadRunning = false;
        }
    }

    void processRequest(Properties properties, BlockIO bIO) throws IOException {

        String argRequest = properties.getProperty(CachePublisher.ARG_REQUEST);
        if( argRequest == null ){
            log.warn("input message is not a request {}", properties);
            return;
        }

        String argSessionId = properties.getProperty(CachePublisher.ARG_SESSION_ID);
        if( argSessionId == null ){
            log.warn("input message has no session-id {}", properties);
            return;
        }

        if( argRequest.equals(CachePublisher.RQST_HEARTBET)) {
            bIO.sendHeartbeat();
            return;
        }

        if( argRequest.equals(CachePublisher.RQST_UNSUBSCRIBE)) {
            Long sessionId = Long.parseLong(argSessionId);
            log.info("process unsubscribe argSessionId=[{}]", sessionId) ;
            ClientConnection clientConnection = connections.remove(sessionId);
            log.info("removed connection [{}]", clientConnection);
            clientConnection.cachePublisher.removeSubscription(sessionId);
            return;
        }
    }

}
