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
import uk.co.octatec.scdds.cache.publish.threading.ThreaderFactory;
import uk.co.octatec.scdds.cache.publish.threading.ThreaderFactoryImpl;

/**
 * Created by Jeromy Drake on 18/05/2016.
 */
public class GlobalDefaults {

    //////////////////////////
    // batch size

    public static final int BATCH_SIZE = 128;

    /////////////////////////////////////////////////////////
    // number of threads used for network sending

    // these are just defaults, a threader factory of your own choosing can be  passed in to the publishing cache builder

    public static int numberOfNetworkSendThreads  = 6;
                                        // 0 == single-threaded (without a queue) which could be better in some situations
                                        // The threader queues up the network writes to be processed by a thread pool.
                                        // If this is set to 1, a single thread is used to send messageas to the network, but
                                        // the thread services a queue, if it is set to 0, there is no queue and  the
                                        // sending takes place on the cache's event-notification thread,
                                        // a value of zero might be useful for high-volume publishers with only a few
                                        // subscribers


    public static final  ThreaderFactory threaderFactory = new ThreaderFactoryImpl();

    ///////////////////////////////////////////////////////
    // retry wait period when accessing the cache-registry

    public static long CACHE_REGISTRY_RETRY_WAIT = 15*1000;


}
