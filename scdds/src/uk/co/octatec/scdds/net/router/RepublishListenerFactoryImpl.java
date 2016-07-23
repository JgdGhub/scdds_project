package uk.co.octatec.scdds.net.router;
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
import uk.co.octatec.scdds.cache.Cache;
import uk.co.octatec.scdds.cache.CacheListener;

/**
 * Created by Jeromy Drake on 13/07/2016.
 */
public class RepublishListenerFactoryImpl implements RepublishListenerFactory{
    @Override
    public CacheListener create(Cache cache, String inCacheName) {
        return new RepublishListener(cache, inCacheName);
    }
}
