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
public interface Threader {
    // this is basically a thread pool, but It can choose which thread runs the job -
    // this is important so that the order of publications is preserved for a given
    // client
    void start();
    void run(int i, Runnable runnable) throws InterruptedException;
    void stop();
    int getNumberOfThreads();
}
