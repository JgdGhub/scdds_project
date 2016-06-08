package uk.co.octatec.scdds;

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
