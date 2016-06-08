package uk.co.octatec.scdds.net.serialize;
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
 * Created by Jeromy Drake on 30/04/2016.
 */
public interface Serializer<K,T> {
    class Pair<K,T> {
        public K key;
        public T value;
    }

    /**
     * Serialize an object into an array of bytes
     * @param key
     * @param value
     * @param reserveHeaderSpace - extra space added to the start of the buffer, must not used by the serializer
     * @return
     */
    byte[] serialize(K key, T value, int reserveHeaderSpace);

    /**
     * Deserialize an object from a byte array
     * The buffer presented alwyas has any 'reserveHeaderSpace' removed, i.e. the object-data will always start at 'offset'
     * @param buff
     * @param offset
     * @return
     */
    Pair<K,T> deserialize(byte[] buff, int offset);
}
