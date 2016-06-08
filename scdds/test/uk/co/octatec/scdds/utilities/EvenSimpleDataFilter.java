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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.cache.publish.CacheFilter;

/**
 * Created by Jeromy Drake on 24/05/2016.
 */
public class EvenSimpleDataFilter implements CacheFilter<String,SimpleData> {

    private final static Logger log = LoggerFactory.getLogger(EvenSimpleDataFilter.class);

    @Override
    public void init(String data) {
    }

    @Override
    public boolean accept(String key, SimpleData value) {
        if( key == null || value == null ) {
            log.error("*** NULL VALUES PASSED TO EvenSimpleDataFilter *** [{}] [[]]", key, value);
            throw new NullPointerException("NULL VALUES PASSED TO EvenSimpleDataFilter");
        }
        return value.data2%2==0;
    }
}
