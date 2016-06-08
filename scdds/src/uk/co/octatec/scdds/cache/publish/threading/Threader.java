package uk.co.octatec.scdds.cache.publish.threading;

/**
 * Created by Jeromy Drake on 17/05/2016.
 */
public interface Threader {
    // this is basically a thread pool, but It can choose which thread runs the job -
    // this is important so that the order of publications is preserved for a given
    // client
    void start();
    void run(int i, Runnable runnable) throws InterruptedException;
    void stop();
    int getNumberOfThreads();
}
