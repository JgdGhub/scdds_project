package uk.co.octatec.scdds.cache;
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
 * Created by Jeromy Drake on 20/06/2016.
 *
 * All objects placed in the cache must implement this interface. The interface is not actually used for anything
 * by the cache, nor does it have any methods. The only purpose of the interface is to document the fact that
 * objects placed in the cache must be treated as immutable by the code that creates the object - never update
 * the fields of an object placed in the cache, if it needs to change create a new object and add it toi the cache.
 * The hope is that having to implement this interface on an object will emphasis the immutability of the object.
 */
public interface ImmutableEntry {
}
