package uk.co.octatec.scdds.cache.publish.threading;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Jeromy Drake on 17/05/2016.
 */
public class RunnableQueueDefaultImpl implements RunnableQueue {

    private final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();

    @Override
    public void put(Runnable runnable) throws InterruptedException {
        queue.put(runnable);
    }

    @Override
    public Runnable take() throws InterruptedException {
        return queue.take();
    }

    @Override
    public void clear() {
        queue.clear();
    }
}
