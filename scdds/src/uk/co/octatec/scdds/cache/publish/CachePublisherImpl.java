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

import uk.co.octatec.scdds.cache.CacheImpl;
import uk.co.octatec.scdds.cache.ImmutableEntry;
import uk.co.octatec.scdds.cache.ListenerEvent;
import uk.co.octatec.scdds.cache.publish.threading.Threader;
import uk.co.octatec.scdds.net.serialize.Serializer;
import uk.co.octatec.scdds.net.serialize.SerializerFactory;
import uk.co.octatec.scdds.net.socket.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Jeromy Drake on 03/05/2016.
 */
public class CachePublisherImpl<K,T extends ImmutableEntry> implements CachePublisher<K,T> {

    private final Logger log = LoggerFactory.getLogger(CachePublisherImpl.class);

    final private CacheImpl<K,T> cache;
    final private SerializerFactory<K, T> serializerFactory;
    final private Serializer<K, T> serializer;
    final private ServerSession serverSession;
    final private GeneralRequestHandler generalRequestHandler;
    final private CopyOnWriteArrayList<ServerSideCacheSubscription<K,T>> clients = new CopyOnWriteArrayList<>();
    final private ArrayList<ServerSideCacheSubscription<K,T>> clientsInErrorState = new ArrayList<>();
    final private Threader threader;
    private int serverPort; // this should always be zero except for tests

    private Thread serverThread;

    private volatile boolean stopped;

    static AtomicLong nextSessionId = new AtomicLong(0);


    public CachePublisherImpl(CacheImpl<K, T> cache, SerializerFactory<K, T> serializerFactory, GeneralRequestHandler generalRequestHandler) {
        this(cache, serializerFactory, generalRequestHandler, new ServerSessionImpl("CachePubSrvr"));
    }

    public CachePublisherImpl(CacheImpl<K, T> cache, SerializerFactory<K, T> serializerFactory, GeneralRequestHandler generalRequestHandler, Threader threader) {
        this(cache, serializerFactory, generalRequestHandler, new ServerSessionImpl("CachePubSrvr"), threader);
    }

    public CachePublisherImpl(CacheImpl<K, T> cache, SerializerFactory<K, T> serializerFactory, GeneralRequestHandler generalRequestHandler, ServerSession serverSession ) {
        this.cache = cache;
        this.serializerFactory = serializerFactory;
        this.serializer =  serializerFactory.create();
        this.generalRequestHandler = generalRequestHandler;
        this.serverSession = serverSession;
        this.threader = null;
    }

    public CachePublisherImpl(CacheImpl<K, T> cache, SerializerFactory<K, T> serializerFactory, GeneralRequestHandler generalRequestHandler,
                                                            ServerSession serverSession, Threader threader ) {
        this.cache = cache;
        this.serializerFactory = serializerFactory;
        this.serializer =  serializerFactory.create();
        this.generalRequestHandler = generalRequestHandler;
        this.serverSession = serverSession;
        this.threader = threader;
    }

    @Override
    public int initializePort() throws IOException {
        serverSession.start(serverPort);
        return serverSession.getPort();
    }

    @Override
    public void start() {
        log.info("start cache publisher threader=[{}]", threader);
        serverThread = new Thread(){
            public void run() {
                serverLoop();
            }
        };
        serverThread.setName("CachePublisher:"+cache.getName());
        serverThread.setDaemon(true);
        serverThread.start();
    }

    @Override
    public void stop() {
        log.info("stop cache publisher, serverThread=[{}]", serverThread);
        stopped = true;
        serverThread.interrupt();
        log.info("stop server session");
        serverSession.stop();
        for(ServerSideCacheSubscription<K,T> client : clients ) {
            log.info("unregister client [{}]", client.getSessionId());
            generalRequestHandler.unRegisterSession(client.getSessionId());
        }
        log.info("stop done");
    }

    @Override
    public String getCacheName() {
        return cache.getName();
    }

    private void serverLoop() {
        try {
            while (true) {
                doServerLoop();
            }
        }
        catch( RuntimeException e) {
            if( stopped ) {
                log.info("Publisher is stopped, (Exception from Serverloop) stopped={} [{}]", stopped, e.getMessage());
            }
            else {
                log.error("Exception from Serverloop (stopped={})", stopped, e);
            }
        }
    }

    void setServerport(int port) {
        serverPort = port;
    }

    private void doServerLoop() {
        try {
            // the accept loop is blocking, but the client returned is none blocking

            log.info("listening for client subscription request tid={} ...", Thread.currentThread().getId());

            Session sc = null;
            try {
                 sc = serverSession.acceptNoneBlockingClient();
            }
            catch( IOException e ) {
                if( stopped ) {
                    log.info("Exception from accept method, publisher is stopped (stopped={}) [{}]", stopped, e.getMessage());
                    throw new RuntimeException("IOException from accept call,Publisher is stopped");
                }
                else {
                    log.error("Exception from accept method, unable to continue processing (stopped={})", stopped, e);
                    throw new RuntimeException("Unrecoverable IOException from accep call");
                }

            }

            InetSocketAddress addr = sc.getAddress();

            sc.enableWriteWaiting();
            BlockIO bio = sc.getBlockIO();
            log.info("got client connection from [{}]", sc.getAddress());
            String initialization = bio.readString();
            log.info("got client initialization string [{}]", initialization);
            Properties properties = PropertyUtils.getPropertiesFromString(initialization);
            String argRequest = properties.getProperty(ARG_REQUEST);

            if( argRequest == null  ) {
                log.error("null request");
                String errorText = PropertyUtils.createPropertyString(ARG_ERROR, "wrong-or-missing-request");
                bio.writeString(errorText);
                sc.close();
            }
            else if( argRequest.equals(RQST_LOAD_CHECK) ) {
                // this has come from the registry
                String argCacheName = properties.getProperty(ARG_CACHE_NAME);
                String reply;
                if( !argCacheName.equals(cache.getName()))  {
                    reply = PropertyUtils.createPropertyString(CachePublisher.ARG_REQUEST, CachePublisher.RQST_LOAD_CHECK,
                            CachePublisher.ARG_CACHE_NAME, argCacheName,
                            CachePublisher.ARG_ERROR, "wrong-cache-name",
                            ARG_EXPECED_CACHE_NAME, cache.getName(),
                            CachePublisher.ARG_TIMESTAMP, System.currentTimeMillis());
                }
                else {
                    reply = PropertyUtils.createPropertyString(CachePublisher.ARG_REQUEST, CachePublisher.RQST_LOAD_CHECK,
                            CachePublisher.ARG_CACHE_NAME, getCacheName(),
                            CachePublisher.ARG_LOAD, clients.size(),
                            CachePublisher.ARG_TIMESTAMP, System.currentTimeMillis());
                }
                log.info("sending reply to load-check [{}]", reply);
                bio.writeString(reply);
                sc.close();
            }
            else if( argRequest.equals(RQST_SUBSCRIBE) ) {

                // this has come from a client

                String argCacheName = properties.getProperty(ARG_CACHE_NAME);
                if (argCacheName == null || !argCacheName.equals(cache.getName())) {
                    log.error("got subscription for wrong cache wanted [{}] got [{}]", argCacheName, cache.getName());
                    String errorText = PropertyUtils.createPropertyString(ARG_ERROR, "wrong-cache-name", ARG_EXPECED_CACHE_NAME, cache.getName());
                    bio.writeString(errorText);
                    sc.close();
                    return;
                }
                String filterClassName = properties.getProperty(ARG_FILTER);
                String filterInitArg = properties.getProperty(ARG_FILTER_ARG);
                CacheFilter<K, T> filter = null;
                if (filterClassName != null && !filterClassName.equals("null")) {
                    log.info("creating server-side filter instance [{}]", filterClassName);
                    try {
                        Class clz = Class.forName(filterClassName);
                        filter = (CacheFilter) clz.newInstance();
                    } catch (Exception e) {
                        log.error("failed to create filter", e);
                        throw new RuntimeException("failed to create Filter class", e);
                    }

                } else {
                    log.info("using default always-accepting server-side filter");
                    filter = ServerSideCacheSubscription.ALWAYS_ACCEPTING_FILTER;
                }

                filter.init(filterInitArg);

                long sessionId = nextSessionId.addAndGet(1);
                String initReply = PropertyUtils.createPropertyString(ARG_SERIALIZER_FACTORY, serializerFactory.getClass().getName(),
                        ARG_CONNECTION_COUNT, clients.size(),
                        ARG_SESSION_ID, sessionId);
                bio.writeString(initReply);
                ServerSideCacheSubscription<K, T> client = new ServerSideCacheSubscription<K, T>(sc, serializer, filter, sessionId, threader, this);
                log.info("add new subscription client: sessionId=[{}] from=[{}] cache=[{}]", sessionId, addr, cache.getName());
                clients.add(client);
                generalRequestHandler.registerSession(sessionId, this, sc);
                cache.requestInitialUpdate(client);

            } else {
                log.error("unknown request [{}]", argRequest);
                String errorText = PropertyUtils.createPropertyString(ARG_ERROR, "wrong-or-missing-request");
                bio.writeString(errorText);
                sc.close();
                return;
            }
        }

        catch( IOException e) {
            log.error("exception while publishing cache [{}] stopped=[{}]", cache.getName(), stopped, e);
            if( stopped ) {
                throw new RuntimeException("Exception publishing cache (Cache Publisher Is Stopped"); // we probab;ly got a ClosedChannelException
            }
        }
    }

    @Override
    public boolean removeSubscription(long sessionId) {

        ArrayList<ServerSideCacheSubscription<K,T>> tmp = new ArrayList<ServerSideCacheSubscription<K,T>>();
        int n = clients.size();
        for(int i=0; i<n; i++) {
            ServerSideCacheSubscription<K,T> client = clients.get(i);
            if( client.getSessionId() == sessionId)  {
                tmp.add(client);
                log.info("removed subscription sessionId=[{}] from [{}]", sessionId, client.getAddress());
                client.dispose();
            }
        }
        if( tmp.size() > 0 ) {
            // there should only be one of gthese
            log.warn("removed matching sessions {}", tmp);
            clients.removeAll(tmp);
        }
        else {
            log.warn("no session to remove for sessionId=[{}]", sessionId);
        }
        return tmp.size()>0;
    }

    void  handleClientSendError(ServerSideCacheSubscription<K, T> client) {
        // should be a rare situation and should always be on the same thread
        log.warn("exception sending to client [{}], client removed ", client.getAddress());
        client.dispose();
        clientsInErrorState.add(client);
        generalRequestHandler.unRegisterSession(client.getSessionId());
    }

    void  processClientInErrorState() {
        // should be a rare situation and should always be on the same thread
        if( clientsInErrorState.size() > 0 ) {
            clients.removeAll(clientsInErrorState);
            clientsInErrorState.clear();
        }
    }

    @Override
    public void onInitialUpdate(ServerSideCacheSubscription<K, T> theClient, Map<K, T> data) {
        log.info("onInitialUpdate theClient=[{}] client-count=[{}] data-size=[{}]", theClient, clients.size(), data.size());
        // just send the initial-update to the client who made the subscription request -
        // the call to "cache.requestInitialUpdate()" makes this happen
        try {
            if( _DBG ) {
                log.debug("send onInitialUpdate to theClient data-size={}", data.size());
            }
            theClient.sendInitialUpdate(data);
        } catch (Throwable t) {
            log.error("exception sending initial update to client session-id[{}] at [{}], client removed ", theClient.getSessionId(), theClient.getAddress());
            log.error("Exception", t);
            removeSubscription(theClient.getSessionId());
            generalRequestHandler.unRegisterSession(theClient.getSessionId());
        }
    }

    //@Override
    //public void __onBatchUpdate(Collection<ListenerEvent<K, T>> listenerEvents) {
    //    for(ListenerEvent<K, T> e : listenerEvents) {
    //        onUpdate(e.key, e.value);
    //    }
    //}

    @Override
    public void onBatchUpdate(Collection<ListenerEvent<K, T>> listenerEvents) {

        int numEvents = listenerEvents.size();
        byte[][] data = new byte[numEvents][];

        int j =0;
        int batchByteSize = 0;
        for(ListenerEvent<K, T> event : listenerEvents) {
            data[j] =  serializer.serialize(event.key, event.value, BlockIoImpl.HEADER_LENGTH);
            BlockIoImpl.copyHeaderToBytes(data[j], BlockIoImpl.DATA_FLAG);
            batchByteSize += data[j].length;
            ++j;
        }

        byte[] batchBuff = new byte[batchByteSize];
        int batchPos = 0;
        for(int i=0; i<numEvents; i++) {
            System.arraycopy(data[i], 0, batchBuff, batchPos, data[i].length);
            batchPos +=  data[i].length;
        }


        int n = clients.size();
        boolean errors = false;
        for(int i=0; i<n; i++) {
            ServerSideCacheSubscription<K,T> client = clients.get(i);
            try {
                if( client.isUnfiltered() ) {
                    // if the client is unfiltered, we can just send the whole batch
                    if (_DBG) {
                        log.debug("sendBatchUpdate to client[{}] numEvents=[{}]", i, numEvents);
                    }
                    client.sendBatchUpdate(batchBuff);
                }
                else {
                    // if the client is applying a filter, we fall back to individual sends to that client
                    if (_DBG) {
                        log.debug("sendBatchUpdate - individual updates to client[{}] numEvents=[{}]", i, numEvents);
                    }
                    int pos = 0;
                    for(ListenerEvent<K, T> event : listenerEvents) {
                        client.send(event.key, event.value, data[pos]);
                        ++pos;
                    }
                }
            }
            catch( Throwable t) {
                // this is unlikely to happen when the sending method actually runs in a separate thread now
                log.error("error sending toi client", t);
                errors = true;
                handleClientSendError(client);
            }
        }
        if( errors ) {
            processClientInErrorState();
        }
    }

    @Override
    public void onUpdate(K key, T value) {
        if( _DBG ) {
            log.debug("PUBLISH onUpdate key=[{}] client-count=[{}]", key, clients.size());
        }
        byte[] data = serializer.serialize(key, value, BlockIoImpl.HEADER_LENGTH);
        BlockIoImpl.copyHeaderToBytes(data, BlockIoImpl.DATA_FLAG);

        int n = clients.size();
        boolean errors = false;
        for(int i=0; i<n; i++) {
            ServerSideCacheSubscription<K,T> client = clients.get(i);
            try {
                if( _DBG ) {
                    log.debug("send onUpdate to client [{}]", key);
                }
                client.send(key, value, data);
            }
            catch( Throwable t) {
                log.error("error sending toi client", t);
                errors = true;
                handleClientSendError(client);
            }
        }
        if( errors ) {
            processClientInErrorState();
        }
    }

    @Override
    public void onDataStale() {
        log.info("notify all clients: data-stale") ;
        int n = clients.size();
        for(int i=0; i<n; i++) {
            ServerSideCacheSubscription<K,T> client = clients.get(i);
            try {
                client.sendDataStale(true);
            }
            catch( Throwable t) {
                handleClientSendError(client);
            }
        }
        processClientInErrorState();
    }

    @Override
    public void onActive() {
        log.info("notify all clients: active") ;
        int n = clients.size();
        for(int i=0; i<n; i++) {
            ServerSideCacheSubscription<K,T> client = clients.get(i);
            try {
                client.sendDataStale(false);
            }
            catch( Throwable t) {
                handleClientSendError(client);
            }
        }

        processClientInErrorState();
    }

    @Override
    public void onRemoved(K key, T value) {
        log.info("notify all clients: removed [{}]", key) ;
        byte[] data = serializer.serialize(key, null, BlockIoImpl.HEADER_LENGTH);
        int n = clients.size();
        for(int i=0; i<n; i++) {
            ServerSideCacheSubscription<K,T> client = clients.get(i);
            try {
                client.sendRemoved(key, value, data);
            }
            catch( Throwable t) {
                handleClientSendError(client);
            }
        }

        processClientInErrorState();
    }

    @Override
    public synchronized void onFailedClient(ServerSideCacheSubscription<K, T> client) {
        log.info("onFailedClient sessionId=[{}]", client.getSessionId());
        handleClientSendError(client);
        processClientInErrorState();
    }
}
