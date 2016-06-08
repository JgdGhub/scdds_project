package uk.co.octatec.scdds;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Jeromy Drake on 18/05/2016.
 */
public class GlobalProperties {

    ////////////////////////////////////////////////////////////////////////
    // latency loggers

    // overall - latency will only be logged if the ConditionalCompilatione flag
    // _LOG_LATENCY is defined - if that ios defined, then the latencies will
    // be logged using these loggers, so that individual logging can be switched off

    public final static Logger llog_1 = LoggerFactory.getLogger("Latency_1");
    public final static Logger llog_2 = LoggerFactory.getLogger("Latency_2");
    public final static Logger llog_3 = LoggerFactory.getLogger("Latency_3");

}
