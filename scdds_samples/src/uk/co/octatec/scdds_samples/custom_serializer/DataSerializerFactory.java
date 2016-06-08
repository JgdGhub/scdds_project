package uk.co.octatec.scdds_samples.custom_serializer;
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
import uk.co.octatec.scdds.net.serialize.Serializer;
import uk.co.octatec.scdds.net.serialize.SerializerFactory;
import uk.co.octatec.scdds_samples.basic_example.Data;

/**
 * Created by Jeromy Drake on 07/06/2016.
 */
public class DataSerializerFactory implements SerializerFactory<String, Data> {
    @Override
    public Serializer<String, Data> create() {
        return new DataSerializer();
    }
}
