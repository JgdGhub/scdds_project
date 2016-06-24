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
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.net.www.protocol.http.HttpURLConnection;
import uk.co.octatec.scdds.cache.Cache;
import uk.co.octatec.scdds.net.html.format.*;
import uk.co.octatec.scdds.net.html.links.CacheDisplayLinkGenerator;
import uk.co.octatec.scdds.net.html.links.CacheEntryDisplayLinkGenerator;
import uk.co.octatec.scdds.net.html.links.LinkGenerator;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Jeromy Drake on 11/06/2016.
 *
 * Http handler used internally to provide a visual display of the caches. The port is allocated automatically, and
 * logged  - in the case of a publisher it is also entered in the registry. You can select a port in advance by setting
 * GlobalProperties.httpServerPort. You can completely disable the Http mechanism by setting  GlobalProperties.exposeHttpServer
 * to false.
 *
 * You can implement your own display mechanism by implementing CacheInfoDisplay and passing in an appropriate
 * CacheInfoDisplayFactory to the builder/subscriber
 */
public class ScddsHttpHandler implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(ScddsHttpHandler.class);

    private final String context;
    private final Map<String, Cache> cacheList = new ConcurrentHashMap<>();

    ScddsHttpHandler(String context) {
        log.info("new ScddsHttpHandler [{}]", context);
        this.context = context;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        try {

            URI uri = httpExchange.getRequestURI();

            log.info("http request: [{}] uri=[{}]  ", httpExchange, uri);

            Reply reply = processRequest(uri.getPath());
            byte[] responseBytes = reply.getBytes();

            Headers headers = httpExchange.getResponseHeaders();
            headers.add("Content-Type", "text/html");
            httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, (long) responseBytes.length);
            OutputStream out = httpExchange.getResponseBody();
            out.write(responseBytes);
            httpExchange.close();
        }
        catch( RuntimeException e) {
            // if we are here something must have gone wrong with the http response output mechanism
            // so there is no point trying to output more error-information to the client
            log.error("error from web service, failed to write output", e);
        }
    }

    public void  addCache(Cache cache) {
        cacheList.put(cache.getName(),cache);
    }

    public synchronized void  removeCache(Cache cache) {
        cacheList.remove(cache.getName(),cache);
    }

    public Reply processRequest(String request)  {

        String[] args = request.split("[/]");

        log.info("http: [path-elements count={}{}] cache-list-size=[{}]", args.length, args, cacheList.size());

        if (!args[1].equals(context)) { // args[0] is empty!
            // we shouldn't be able to get this
            log.info("bad usage [{}]", request);
            return new Reply("incorrect usage, unknown context [" + args[1] + "], required context is [" + context + "]");
        }

        if( args.length == 2 ) { // main-page:  /scdds
            HashMap<String, Integer> cachesSummary = new HashMap<>();
            for(Cache cache : cacheList.values()) {
                log.info("http: found cache [{}]", cache.getName());
                cachesSummary.put(cache.getName(), cache.size());
            }
            CacheDisplayLinkGenerator   caheDisplayLinkGenerator = new CacheDisplayLinkGenerator();
            MapHtmlFormatter<?,?> mapFormatter = new MapHtmlFormatter<>(cachesSummary, FormatConstants.DISPLAY_VALUE_AS_STRING, caheDisplayLinkGenerator);
            return new Reply( mapFormatter.format("List Of Caches", "cache-name", "size") );
        }
        else if( args.length == 4 &&  args[2].equals("cache") ) { // cache-display-page:  /scdds/cache/{cache-name}
                // we are to display a particular cache, if the cache is small enough, display it in full, otherwise
                // display a compact-list of its keys
                String cacheName = args[3];
                Cache cache = cacheList.get(cacheName);
                if( cache == null ) {
                    log.warn("unknown cache [{]] in url request [{}]", cacheName, request);
                    return new Reply("unknown cache name ["+request+"]");
                }
                if( cache.size()>500 ) {
                    LinkGenerator linkGenerator =  new CacheEntryDisplayLinkGenerator(cacheName);
                    CompactCollectionHtmlFormatter<?> compactCollectionFormatter = new CompactCollectionHtmlFormatter<>(cache.keySet(),linkGenerator);
                    return new Reply( compactCollectionFormatter.format("cache keys: "+cache.getName()) );
                }
                else {
                    CacheHtmlFormatter<?,?> cacheFormatter = new CacheHtmlFormatter<>(cache, FormatConstants.DISPLAY_VALUE_AS_STRING);
                    return new Reply( cacheFormatter.format("cache: "+cache.getName(), "key", "value") );
                }
        }
        else if( args.length == 5 &&  args[2].equals("cache") ) { // cache-eentry-display-page:  /scdds/cache/{cache-name}/{cache-key}

            String cacheName = args[3];
            String cacheKey = args[4];
            Cache cache = cacheList.get(cacheName);
            if( cache == null ) {
                log.warn("unknown cache [{]] in url request [{}]", cacheName, request);
                return new Reply("unknown cache name ["+request+"]");
            }
            Object bean = cache.get(cacheKey) ;
            if( bean == null ) {
                log.warn("unknown cache-entry  key=[{]] cache-name=[{}]in url request [{}]", cacheKey, cacheName, request);
                return new Reply("unknown cache-entry name ["+request+"]");
            }
            String title = "cache: "+cacheName+", key="+ cacheKey;
            BeanHtmlFormatter<Object> beanFormater = new BeanHtmlFormatter();
            return new Reply( beanFormater.format(title, bean));
        }
        else {
            log.warn("unknown url request [{}]", request);
            return new Reply("["+request+"] is not implemented");
        }
    }


}
