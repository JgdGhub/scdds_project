package uk.co.octatec.scdds.net.registry;
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
import uk.co.octatec.scdds.net.socket.ServerSession;
import uk.co.octatec.scdds.net.socket.ServerSessionImpl;
import uk.co.octatec.scdds.net.socket.Session;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

/**
 * Created by Jeromy Drake on 01/05/2016.
 */
public class RegistryServer implements Runnable{
    private final static Logger log = LoggerFactory.getLogger(RegistryServer.class);

    static final byte PROTO_ID_FLAG = (byte) '$';
    static final byte[] PROTO_ID = new byte[] { PROTO_ID_FLAG };

    static final String CMD_FIND = "find";
    static final String CMD_REGISTER = "register";
    static final String CMD_UNREGISTER = "unregister";
    static final String CMD_DUMP = "dump";
    static final String CMD_UPLOAD = "upload";

    static final String RSP_NOT_FOUND = "not-found";
    static final String RSP_FOUND     = "found";
    static final String RSP_OK        = "ok";
    static final String RSP_WAIT      = "wait";
    static final String RSP_FAIL      = "fail";
    static final String RSP_RETRY     = "retry";
    static final String RSP_INVALID   = "invalid-command";
    static final String RSP_DUMP      = "dump";

    private volatile int port;
    private volatile boolean running;
    private ServerSession serverSession;
    final private Registry registry;

    private static Thread mainThread;

    public RegistryServer(int port) {
        this(port, null);
    }

    RegistryServer(int port, RegistryEntryValidator registryEntryValidator) {
        this.port = port;
        registry = registryEntryValidator == null ? new Registry() : new Registry(registryEntryValidator);
    }

    public static RegistryServer startInThread() { // mainly for unit tests
        return startInThread(0, null);
    }

    public static void stopInThread(RegistryServer registryServer) { // mainly for unit tests
        log.info("--- stop registry server in thread ---");
        registryServer.running = false;
        mainThread.interrupt();
        registryServer.serverSession.stop();
    }

    public static RegistryServer startInThread(RegistryEntryValidator registryEntryValidator) { // mainly for unit tests
        return startInThread(0, registryEntryValidator);
    }

    public static RegistryServer startInThread(int port) { // mainly for unit tests
        return startInThread(port, null);
    }

    static RegistryServer startInThread(int port, RegistryEntryValidator registryEntryValidator) { // mainly for unit tests
        log.info("--- start registry server in thread on port {} with registryEntryValidator=[{}} ---", port, registryEntryValidator);
        RegistryServer reg = new RegistryServer(port, registryEntryValidator);
        mainThread = new Thread(reg) ;
        mainThread.setName("RegistryServer");
        mainThread.start();
        return reg;
    }

    public static int awaitPortAllocation(RegistryServer registryServer) throws InterruptedException{ // for unit tests
        int maxLoopCount = 100;
        long sleepTime = 10;
        for (int i = 0; i < maxLoopCount; i++) {
            if( registryServer.getPort() != 0) {
                break;
            }
            Thread.sleep(sleepTime);
        }
        if( registryServer.getPort() == 0) {
            log.warn("no port set in server after waiting {}ms", maxLoopCount * sleepTime);
        }
        log.info("registry port allocated [{}] after maxLoopCount=[{}]", registryServer.getPort(), maxLoopCount);
        return registryServer.getPort();
    }

    public static void main(String[] args) {

        if( args.length == 0 ) {
            log.error("registry: port-number must be specified as an argument, will exit now");
            System.exit(-1);
        }

        String port = args[0];

        log.info("registry: starting on port[{}]", port);
        RegistryServer reg = new RegistryServer(Integer.parseInt(port));

        if( args.length > 1 ) {

            // if one registry-server is already running, you can restart an existing one or
            // start a new one, and have it synchronize its contents with the already-running
            // registry using  -sync:<hostname>:<port>

            if( args[1].startsWith("-sync:")) {
                String[] ss = args[1].split("[:]");
                if( ss.length != 3 ) {
                    log.info("wrong arguments to synchronization option, required:  -sync:host:port, will exit now");
                    System.exit(-1);
                }
                String syncHost = args[1];
                int syncPort = Integer.parseInt(args[2]);
                reg.syncRegistry(syncHost, syncPort);
            }
        }

        reg.run();
    }

    public void syncRegistry(String syncHost, int syncPort) {
        try {
            log.info("synchronizing registry with [{}:{}]", syncHost, syncPort);
            RegistrySynchronizer rsync = new RegistrySynchronizer(syncHost, syncPort);
            rsync.synchronize(registry);
        }
        catch( IOException x ) {
            log.error("synchronization failed, will exit now [{}]", x.getMessage());
            System.exit(-1);
        }
    }

    @Override
    public void run() {

        running  = true;

        log.info("running registry server, current entry count [{}]", registry.getEntryCount());

        try {
            serverSession = new ServerSessionImpl("RegistrySvr");
            if (port == 0) {
                serverSession.start();
                port = serverSession.getPort();
            } else {
                serverSession.start(port);
            }

            while(running) {
                Session sd = serverSession.acceptBlockingClient();
                InetSocketAddress addr = sd.getAddress();
                log.info("starting registry session for [{}] port=[{}]", addr, port);
                RegistryServerSession registrySession = new RegistryServerSession(sd, registry, addr);
                Thread t = new Thread(registrySession);
                t.setDaemon(true);
                t.setName("RegSession_"+addr.getHostName());
                t.start();
            }
        }
        catch( IOException e) {
            log.error("IOException exception while running registry, registry will exit", e);
        }
    }

    int getPort() { // for unit tests
        return port;
    }

    public Registry.Entry find(String cacheName) {
        return registry.find(cacheName);
    }

    Map<String, List<Registry.Entry>> getRegistryMap() { // for unit tests
        return registry.getRegistryMap();
    }

    int getEntryCount()  {
        return  registry.getEntryCount();
    }
}
