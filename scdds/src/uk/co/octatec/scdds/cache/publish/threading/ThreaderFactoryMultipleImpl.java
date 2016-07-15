package uk.co.octatec.scdds.cache.publish.threading;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.GlobalDefaults;

/**
 * Created by Jeromy Drake on 15/07/2016.
 *
 * This can be used as an alternative Threader factory so that individual publishers can have their own thread pool, the
 * default is for one single thread-pool to be shared across all publishers
 */
public class ThreaderFactoryMultipleImpl implements ThreaderFactory  {

    private final static Logger log = LoggerFactory.getLogger(ThreaderFactoryMultipleImpl.class);

    private static RunnableQueueFactory runnableQueueFactory = new RunnableQueueFactoryDefaultImpl();

    private int numberOfThreads;

    public ThreaderFactoryMultipleImpl() {
        this(GlobalDefaults.numberOfNetworkSendThreads);
    }

    /**
     *
     * @param numThreads  If this is zero, the network send is performed on the cache's event-notification thread
     */
    public ThreaderFactoryMultipleImpl(int numThreads) {
        numberOfThreads = numThreads;
    }

    public ThreaderFactoryMultipleImpl(RunnableQueueFactory queueFactory) {
        runnableQueueFactory =  queueFactory;
    }


    @Override
    public synchronized Threader getInstance() { // ok to synchronize this is only ever called one time per cache-creation
        if( numberOfThreads == 0 ) {
            log.info("ThreaderFactoryMultipleImpl.getInstance - returning null instance, thread-count = {}", numberOfThreads);
            return null;
        }
        else {
            log.info("ThreaderFactoryMultipleImpl.getInstance - returning instance, thread-count = {}", numberOfThreads);
            return new ThreaderImpl(runnableQueueFactory, numberOfThreads);
        }
    }
}
