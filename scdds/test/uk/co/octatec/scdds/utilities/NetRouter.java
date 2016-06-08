package uk.co.octatec.scdds.utilities;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Jeromy Drake on 11/05/2016.
 */
public class NetRouter implements Runnable{

    private final static Logger log = LoggerFactory.getLogger(NetRouter.class);

    private int localPort;
    private String remoteHost;
    private int remotePort;

    private Socket localSocket;
    private Socket remoteSocket;

    private ServerSocket serverSocket;

    private volatile boolean connected;
    private volatile boolean running;

    private Thread mainThread;
    private Thread localThread;
    private Thread remoteThread;

    public static NetRouter start(String remoteHost, int remotePort) throws IOException {
        return start(remoteHost, remotePort, 0);
    }

    public static NetRouter start(String remoteHost, int remotePort, int localPort) throws IOException {

        log.info("@@starting router, target -> [{}:{}]", remoteHost, remotePort);
        NetRouter app = new NetRouter(remoteHost, remotePort);
        int port = app.initializeLocalPort(localPort);
        Thread thread = new Thread(app);
        thread.setDaemon(true);
        thread.setName("NetRouter");
        thread.start();
        log.info("@@router running on local port [{}]", port);
        app.mainThread = thread;
        return app;
    }

    private NetRouter(String remoteHost, int remotePort) {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

    public void  abort() {
        log.info("@@abort!");
        running = false;
        mainThread.interrupt();
        localThread.interrupt();
        remoteThread.interrupt();

        try {
            log.info("@@close serverSocket");
            serverSocket.close();
        }
        catch( Exception e) {

        }

        try {
            log.info("@@close remoteSocket");
            remoteSocket.close();
        }
        catch( Exception x){

        }
        try {
            log.info("@@close localSocket");
            localSocket.close();
        }
        catch( Exception e) {

        }

        try {
            log.info("@@check thread states...");
            Thread.sleep(50);
        }
        catch( InterruptedException x) {
        }
        log.info("@@thread states: main [{}] local [{}] remote [{}]", mainThread.getName(), localThread.getState(), remoteThread.getState());
    }

    public int getLocalPort() {
        return localPort;
    }

    public void run() {
        try {
            running = true;
            waitForLocalClient();

        }
        catch( Exception e) {
            log.error("@@exception while waiting for local client", e) ;

        }
        log.info("@@main-thread - thread done");
    }

    private int initializeLocalPort(int port)throws IOException  {
        if( port != 0 ) {
            log.info("@@using specific local port [{}]", port);
        }
        InetSocketAddress addr = new InetSocketAddress(port);
        log.info("@@selected local port [{}]", addr.getPort());
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(addr);
        localPort = addr.getPort();
        return localPort;
    }

    private void waitForLocalClient() throws IOException {

        try {
            while (running) {
                log.info("@@waiting for local client");
                localSocket = serverSocket.accept();
                log.info("@@local client connected: " + localSocket.getInetAddress());
                if (connected) {
                    log.warn("@@already connected, reject connection  attempt from " + localSocket.getInetAddress());
                    localSocket.close();
                    continue;
                }
                connected = true;
                log.info("@@connecting to remote [{}:{}]", remoteHost, remotePort);
                remoteSocket = new Socket(remoteHost, remotePort);
                log.info("@@remote connected: " + remoteSocket.getInetAddress());
                runSession();
            }

        }
        catch( IOException e) {
            log.info("IOException in thread {}", Thread.currentThread().getName());
        }
        log.info("@@waitForLocalClient - thread done");

    }

    private void runSession() {
        localThread = new  Thread() {
            public void run() {
                forward(localSocket, remoteSocket, "local");
                log.info("@@runSession - 'local' thread done");
            }
        };
        localThread.setName("Router:LocalClient");
        localThread.setDaemon(true);
        localThread.start();
        log.info("@@client listener started");

        remoteThread = new  Thread() {
            public void run() {
                forward(remoteSocket, localSocket, "remote");
                log.info("@@runSession - 'remote' thread done");
            }
        };
        remoteThread.setName("Router:Remote");
        remoteThread.setDaemon(true);
        remoteThread.start();
        log.info("@@remote forwarder started");
    }

    private void forward(Socket from, Socket sendTo, String role) {

        int byteCount = 0;

        try {
            byte[] buff = new byte[20480];
            InputStream in = from.getInputStream();
            OutputStream out = sendTo.getOutputStream();
            while(running) {
                int n = in.read(buff, 0, buff.length);
                if( n < 0 ) {
                    log.error("@@read error {}: {}", role, n);
                }
                out.write(buff, 0, n);
                if( byteCount == 0 ) {
                    log.info("@@process {} bytes count={}", role, n);
                    byteCount += n;
                }
                else {
                    byteCount += n;
                    if( (byteCount%10000)==0 ) {
                        log.info("@@process {} bytes count={}", role, byteCount);
                    }
                }
            }
        }
        catch( Exception e) {
            log.error("@@connection error {} ",role, e);
            closeAll();
            connected = false;
        }
    }

    void closeAll() {
        try {
            localSocket.close();
        }
        catch( Exception e) {

        }
        try {
            remoteSocket.close();
        }
        catch( Exception e) {

        }
    }

}
