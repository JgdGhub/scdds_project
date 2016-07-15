package uk.co.octatec.scdds.cache.publish.threading;
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
import uk.co.octatec.scdds.GlobalDefaults;

/**
 * Created by Jeromy Drake on 17/05/2016.
 *
 * This is the default Threader factory, which creates on Threader (thread-pool) for the entire application.
 * Different publishers within the same app could be given different thread-pools using the alternative
 * ThreaderFactoryMultipleImpl
 */
public class ThreaderFactoryImpl implements ThreaderFactory {

    private final static Logger log = LoggerFactory.getLogger(ThreaderFactoryImpl.class);

    private static RunnableQueueFactory runnableQueueFactory = new RunnableQueueFactoryDefaultImpl();

    public ThreaderFactoryImpl() {
    }


    public ThreaderFactoryImpl(RunnableQueueFactory queueFactory) {
        runnableQueueFactory =  queueFactory;
    }

    static Threader instance;

    @Override
    public synchronized Threader getInstance() { // ok to synchronize this is only ever called one time per cache-creation
        if( GlobalDefaults.numberOfNetworkSendThreads == 0 ) {
            log.info("ThreaderFactoryImpl.getInstance - returning null Threader - single threaded none queuing [see GlobalDefaults.numberOfNetworkSendThreads]");
            return null;
        }
        if( instance == null ) {
            log.info("ThreaderFactoryImpl.getInstance - creating new instance, thread-count = {} [see GlobalDefaults.numberOfNetworkSendThreads]",  GlobalDefaults.numberOfNetworkSendThreads);
            instance = new ThreaderImpl(runnableQueueFactory, GlobalDefaults.numberOfNetworkSendThreads);
        }
        else {
            log.info("ThreaderFactoryImpl.getInstance - using existing instance, thread-count = [{}]", instance.getNumberOfThreads());
            if( instance.getNumberOfThreads() != GlobalDefaults.numberOfNetworkSendThreads ) {
                log.warn("THE NUMBER OF THREADS DOES NOT MATCH GlobalDefaults.numberOfNetworkSendThreads THIS INDICATES IT WAS CHANGED AFTER THE THREADER WAS CREATED");
                log.warn("...the above message is not a problem, but it indicates you tried to change the number of threads too late in the startup");

            }
        }
        return instance;
    }
}
