package uk.co.octatec.scdds.cache.publish.threading;

/**
 * Created by Jeromy Drake on 17/05/2016.
 */
public class ThreaderFactoryImpl implements ThreaderFactory {

    private final static int DEFAULT_NUMBER_OF_THREADS = 6;

    private static int numberOfThreads = DEFAULT_NUMBER_OF_THREADS;
    private static RunnableQueueFactory runnableQueueFactory = new RunnableQueueFactoryDefaultImpl();


    public ThreaderFactoryImpl() {
    }

    public ThreaderFactoryImpl(int numThreads) {
        numberOfThreads = numThreads;
    }

    public ThreaderFactoryImpl(RunnableQueueFactory queueFactory, int numThreads) {
        numberOfThreads = numThreads;
        runnableQueueFactory =  queueFactory;
    }

    static Threader instance;

    @Override
    public synchronized Threader getInstance() { // ok to synchronize this is only ever called one time per cache-creation
        if( numberOfThreads == 0 ) {
            return null;
        }
        if( instance == null ) {
            instance = new ThreaderImpl(runnableQueueFactory, numberOfThreads);
        }
        return instance;
    }
}
