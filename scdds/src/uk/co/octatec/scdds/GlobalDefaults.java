package uk.co.octatec.scdds;

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

    public static int numberOfNetworkSendThreads  = 0;
                                        // 0 == single-threaded (without a queue) which could be better in some situations
                                        // The threader queues up the network writes to be processed by a thread pool


    public static final  ThreaderFactory threaderFactory = new ThreaderFactoryImpl(numberOfNetworkSendThreads);

    ///////////////////////////////////////////////////////
    // retry wait period when accessing the cache-registry

    public static long CACHE_REGISTRY_RETRY_WAIT = 15*1000;


}
