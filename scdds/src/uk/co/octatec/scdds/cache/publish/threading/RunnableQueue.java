package uk.co.octatec.scdds.cache.publish.threading;

/**
 * Created by Jeromy Drake on 17/05/2016.
 */
public interface RunnableQueue {
    void put(Runnable runnable) throws InterruptedException ;
    Runnable take() throws InterruptedException ;
    void clear();
}
