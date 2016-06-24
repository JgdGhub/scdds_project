package uk.co.octatec.scdds.utilities;
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
import java.util.Date;

/**
 * Created by Jeromy Drake on 09/05/2016.
 */
public class DataWithDate implements Serializable, ImmutableEntry {

    long serialVersionUID = 1;

    int id;
    Date date = new Date();

    public DataWithDate(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataWithDate data = (DataWithDate) o;

        if (id != data.id) return false;
        return date != null ? date.equals(data.date) : data.date == null;

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (date != null ? date.hashCode() : 0);
        return result;
    }
}
