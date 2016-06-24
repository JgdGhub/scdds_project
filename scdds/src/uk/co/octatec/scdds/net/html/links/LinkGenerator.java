package uk.co.octatec.scdds.net.html.links;
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
 * Created by Jeromy Drake on 13/06/2016.
 *
 * The LinkGenerator can be passed to the various HtmlFormatters to make the entries clickable
 */
public interface LinkGenerator {
    String getLink(String linkText);
}
