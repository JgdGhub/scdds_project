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
 * When a list is being rendered, this LinkFormater is used to make the entries clickable,
 * i.e. clicking on the link will call the page to display the cache
 */
public class CacheDisplayLinkGenerator implements LinkGenerator{
    // clicking on the link will go to the page that will display the cache
    @Override
    public String getLink(String linkText) {
        String target =  "/scdds/cache/"+linkText;
        String html = "<a href=\""+target+"\">"+linkText+"</a>";
        return html;
    }
}
