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
import uk.co.octatec.scdds.GlobalProperties;
import uk.co.octatec.scdds.cache.info.CacheInfoDisplay;
import uk.co.octatec.scdds.cache.info.CacheInfoDisplayFactory;

/**
 * Created by Jeromy Drake on 11/06/2016.
 */
public class HttpServerFactory implements CacheInfoDisplayFactory {
    private static HttpServer httpServer;
    public synchronized CacheInfoDisplay get() {
        if( httpServer == null ) {
            httpServer = new HttpServer(GlobalProperties.httpServerPort);
            httpServer.start();
        }
        return httpServer;
    }
}
