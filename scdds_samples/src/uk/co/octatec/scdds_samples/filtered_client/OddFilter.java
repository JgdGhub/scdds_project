package uk.co.octatec.scdds_samples.filtered_client;
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
import uk.co.octatec.scdds.cache.publish.CacheFilter;
import uk.co.octatec.scdds_samples.basic_example.Data;

/**
 * Created by Jeromy Drake on 20/06/2016.
 */
public class OddFilter implements CacheFilter<String, Data> {
    @Override
    public void init(String data) {

    }

    @Override
    public boolean accept(String key, Data value) {
        int id = value.getId();
        return id%2==0; // filter out all values with ODD Id's
    }
}
