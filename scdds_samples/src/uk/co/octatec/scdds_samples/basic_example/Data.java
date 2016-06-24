package uk.co.octatec.scdds_samples.basic_example;
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
import uk.co.octatec.scdds.cache.ImmutableEntry;

import java.io.Serializable;

/**
 * Created by Jeromy Drake on 06/06/2016.
 */
public class Data implements Serializable, ImmutableEntry /*needed for the default serializer*/ {

    static final long serialVersionUID = 1; // do this if you are using default serialization

    private static final double CONSTANT = 2.345;

    final int id;
    final long milliseconds;
    final String information;
    final double value;

    public Data(int id, String information) {
        this.id = id;
        this.milliseconds = System.currentTimeMillis();
        this.value = id * CONSTANT;
        this.information = information;
    }

    public Data(int id, long milliseconds, double value, String information) {
        // useful to have if you are doing custom serialization
        this.id = id;
        this.milliseconds = milliseconds;
        this.information = information;
        this.value = value;
    }

    public int getId() {
        return id;
    }

    public double getValue() {
        return value;
    }

    public long getMilliseconds() {
        return milliseconds;
    }

    public String getInformation() {
        return information;
    }

    @Override
    public String toString() {
        return "Data{" +
                "id=" + id +
                ", milliseconds=" + milliseconds +
                ", information='" + information + '\'' +
                ", value=" + value +
                '}';
    }
}
