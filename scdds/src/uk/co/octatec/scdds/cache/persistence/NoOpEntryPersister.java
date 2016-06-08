package uk.co.octatec.scdds.cache.persistence;
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
 * Created by Jeromy Drake on 02/05/2016.
 */
public final class NoOpEntryPersister<K,T> implements  EntryPersister<K,T>{

    @Override
    public void open() {
    }

    @Override
    public void store(K key, T value) {
    }

    @Override
    public void markDeleted(K key) {

    }

    @Override
    public void close() {
    }
}
