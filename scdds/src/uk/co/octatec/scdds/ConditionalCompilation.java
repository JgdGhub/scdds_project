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
/**
 * Created by Jeromy Drake on 12/05/2016.
 */
public final class ConditionalCompilation {

    public static final boolean _DBG = true;
            // If this is true, log.debug() calls are compiled in, otherwise they are excluded
            // if they are compiled-in, then the actually loging, is, of course, still controlled
            // by the usual SLF4J mechanisms

    public static final boolean _LOG_LATENCY = false;
                // if true, then latency is logged using the 3 loggers, llog_1 llog_2 and llog_3
                // if _LOG_LATENCY is true, latency-logging can still be switched off by
                // the normal SLF4J mechanism for these 3 loggers


}
