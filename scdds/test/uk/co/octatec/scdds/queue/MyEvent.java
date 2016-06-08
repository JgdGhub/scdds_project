package uk.co.octatec.scdds.queue;
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
 * Created by Jeromy Drake on 13/05/2016.
 */
public class MyEvent<T> implements Event<T>  {
    T key;
    String value;
    boolean canBeBatched = true;
    public MyEvent(T key, String value) {
        this.key = key;
        this.value = value;
        if( key == null ) {
            canBeBatched = false;
        }

    }

    @Override
    public T getKey() {
        return key;
    }

    @Override
    public String toString() {
        return "MyEvent{" +
                "key=" + key +
                ", value='" + value + '\'' +
                '}';
    }

    @Override
    public boolean canBeBatched() {
        return canBeBatched;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MyEvent<?> myEvent = (MyEvent<?>) o;

        if (!key.equals(myEvent.key)) return false;
        return value.equals(myEvent.value);

    }

    @Override
    public int hashCode() {
        int result = key.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }
}
