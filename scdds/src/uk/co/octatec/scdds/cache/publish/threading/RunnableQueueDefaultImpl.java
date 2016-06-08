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
