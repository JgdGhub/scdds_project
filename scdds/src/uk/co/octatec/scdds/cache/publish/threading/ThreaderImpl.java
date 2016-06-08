package uk.co.octatec.scdds.cache.publish.threading;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static uk.co.octatec.scdds.ConditionalCompilation._DBG;

/**
 * Created by Jeromy Drake on 17/05/2016.
 */
public class ThreaderImpl implements  Threader {
    // this is basically a thread pool, but It can choose which thread runs the job -
    // this is important so that the order of publications is preserved for a given
    // client

    private final static Logger log = LoggerFactory.getLogger(ThreaderImpl.class);

    private final static int DEFAULT_NUMBER_OF_THREADS = 6;

    private static class Worker implements Runnable {

        final RunnableQueue queue;
        final int id;

        Worker(RunnableQueue queue, int id) {
            this.queue = queue;
            this.id = id;
        }

        @Override
        public void run() {
            log.info("run-queue active");
            try {
                while( true ) {
                    Runnable r = queue.take();
                    try {
                        r.run();
                        r = null;
                    } catch (Throwable t) {
                        log.error("exception running task in runnable queue [{}]", r, t);
                    }
                }
            }
            catch( InterruptedException e) {
                log.error("InterruptedException from runnable queue, this thread will stop id=[{}]", id);
            }
        }
    }


    private static class ThreadItem {
        Thread thread;
        RunnableQueue queue;
    };

    private ThreadItem[] threadItems;

    private final RunnableQueueFactory factory;
    private final int numberOfThreads;

    public ThreaderImpl() {
        this(new RunnableQueueFactoryDefaultImpl(), DEFAULT_NUMBER_OF_THREADS);
    }

    public ThreaderImpl(int numOfThreads) {
        this(new RunnableQueueFactoryDefaultImpl(), numOfThreads);
    }

    public ThreaderImpl(RunnableQueueFactory factory, int numberOfThreads) {
        this.factory = factory;
        this.numberOfThreads = numberOfThreads;
        log.info("Threader created #={}", System.identityHashCode(this));
    }

    @Override
    public void start() {

        synchronized( this ) {
            if( threadItems != null ) {
                log.info("threader already started, thread-count=[{}]", threadItems.length);
                return;
            }
            log.info("starting threader, thread-count=[{}] #={}", numberOfThreads, System.identityHashCode(this));
            threadItems = new ThreadItem[numberOfThreads];
            for (int i = 0; i < numberOfThreads; i++) {
                threadItems[i] = new ThreadItem();
                threadItems[i].queue = factory.create();
                Worker worker = new Worker(threadItems[i].queue, i);
                threadItems[i].thread = new Thread(worker);
                log.info("...start runnable queue thread [{}]", i);
                threadItems[i].thread.setName("RunnableQ:" + i);
                threadItems[i].thread.setDaemon(true);
                threadItems[i].thread.start();
            }
        }
    }

    @Override
    public void run(int i, Runnable runnable) throws InterruptedException{
        threadItems[i].queue.put(runnable);
    }

    @Override
    public void stop() {
       try {
            for(ThreadItem t : threadItems) {
                t.queue.clear();
                t.thread.interrupt();
            }
           threadItems = null;
       }
       catch( Throwable t ) {
           log.error("error stopping Threader", t);
       }
    }

    @Override
    public int getNumberOfThreads() {
        return numberOfThreads;
    }
}
