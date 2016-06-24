package uk.co.octatec.scdds;
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

/**
 * Created by Jeromy Drake on 18/05/2016.
 */
public class GlobalProperties {

    /////////////////////////////////////////////////////////////////////
    // Http Server
    //
    // by default a simple Http Server is exposed that gives information about caches
    // (http://<hostname>:<port>/scdds/caches)

    public static boolean exposeHttpServer = true; // a simple interface for providing information on the active caches
    public static int httpServerPort = 0; // this can be specified but if left at 0 the system will choose a free port, the port
                                         // chosen is available from the Registry (which can be queried by a RESTful API)

    ////////////////////////////////////////////////////////////////////////
    // latency loggers

    // overall - latency will only be logged if the ConditionalCompilation flag
    // _LOG_LATENCY is defined - if that is defined, then the latencies will
    // be logged using these loggers, so that individual logging can be switched on or off
    // as normal using

    public final static Logger llog_1 = LoggerFactory.getLogger("Latency_1");
    public final static Logger llog_2 = LoggerFactory.getLogger("Latency_2");
    public final static Logger llog_3 = LoggerFactory.getLogger("Latency_3");

}
