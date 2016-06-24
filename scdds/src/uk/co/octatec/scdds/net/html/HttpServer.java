package uk.co.octatec.scdds.net.html;
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
import com.sun.net.httpserver.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.cache.Cache;
import uk.co.octatec.scdds.cache.info.CacheInfoDisplay;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Created by Jeromy Drake on 11/06/2016.
 *
 * A light-weight http server to report information about the active caches
 *
 * The  port is allocated automatically, and logged  - in the case of a publisher it is also entered in the registry.
 * You can select a port in advance by setting GlobalProperties.httpServerPort. You can completely disable the Http
 * mechanism by setting  GlobalProperties.exposeHttpServer to false.
 *
 * You can implement your own display mechanism by implementing CacheInfoDisplay and passing in an appropriate
 * CacheInfoDisplayFactory to the builder/subscriber. For example you could implement an MBean based display
 * mechanism - the reason that isn't done by default is due to re-distribution issues with the jmxtools jar.
 */
public class HttpServer implements CacheInfoDisplay {

    private static final Logger log = LoggerFactory.getLogger(HttpServer.class);

    static final String BASE_CONTEXT = "scdds";

    final private int port;
    final private int socketBacklog;
    final private int numHttpThreads;

    private ScddsHttpHandler scddsHttpHandler;

    public HttpServer(int port) {
        this(selectPort(port), 5, 1);
    }

    public HttpServer(int port, int numHttpThreads, int socketBacklog) {
        this.port = port;
        this.socketBacklog = socketBacklog;
        this.numHttpThreads = numHttpThreads;
    }

    static int selectPort(int port) {
        if( port > 0 ) {
            log.info("http-port: {}", port);
            return port;
        }
        try {
            InetSocketAddress addr = new InetSocketAddress(0);
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.socket().setReuseAddress(true);
            serverChannel.socket().bind(addr);
            addr = (InetSocketAddress) serverChannel.getLocalAddress();
            port = addr.getPort();
            serverChannel.close();
            log.info("http-port: {}", port);
            return port;
        }
        catch( IOException x ) {
            log.info("http-port: none");
            log.error("can't find a free port for http server");
            throw new RuntimeException("can't find a free http port") ;
        }
    }

    public void addCache(Cache cache) {
        log.info("http: add cache to viewer [{}]", cache.getName());
        scddsHttpHandler.addCache(cache);
    }

    @Override
    public int getHttpPort() {
        return port;
    }

    public void removeCache(Cache cache) {
        scddsHttpHandler.removeCache(cache);
    }

    public void start() {

        // e.g. http://localhost:9999/scdds/caches

        log.info("starting http-server port=[{}] socket-backlog=[{}] numThreads=[{}]", port, socketBacklog, numHttpThreads);
        try {
            InetSocketAddress addr = new InetSocketAddress(port);
            com.sun.net.httpserver.HttpServer srv = com.sun.net.httpserver.HttpServer.create(addr, socketBacklog);
            HttpContext ctx = srv.createContext("/"+BASE_CONTEXT);
            scddsHttpHandler = new ScddsHttpHandler(BASE_CONTEXT);
            ctx.setHandler(scddsHttpHandler);
            srv.setExecutor(Executors.newFixedThreadPool(numHttpThreads));
            srv.start();
        }
        catch(IOException x) {
            log.error("failed to create HttpServer on port [{}]", port, x);
            throw new RuntimeException("failed to create HttpServer on port "+port, x) ;
        }
    }
}
