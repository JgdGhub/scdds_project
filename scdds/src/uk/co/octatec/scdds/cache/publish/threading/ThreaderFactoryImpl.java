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
