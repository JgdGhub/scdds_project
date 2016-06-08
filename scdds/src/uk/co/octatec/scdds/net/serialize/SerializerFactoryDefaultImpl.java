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
import java.io.Serializable;

/**
 * Created by Jeromy Drake on 30/04/2016.
 *
 * A default serialization implementation using java-serialization, you will get
 * better performance if you write serilizers specific to your classes
 */
public class SerializerFactoryDefaultImpl<K,T> implements SerializerFactory<K,T>{
    @Override
    public Serializer<K,T> create() {
        return new DefaultSerializer<K,T>() ;
    }
}
