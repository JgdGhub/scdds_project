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
 * Created by Jeromy Drake on 14/06/2016.
 *
 * When a cache is being displayed as HTML using the Compact-Form, i.e. the cache is
 * large so just the keys are being displayed, this LinkGenerator is used to make the
 * key clickable and display the cache-entry associated with the key
 */
public class CacheEntryDisplayLinkGenerator implements LinkGenerator{
    // clicking on the link will go to the page that will display the cache entry

    private final String cacheName;

    public CacheEntryDisplayLinkGenerator(String cacheName) {
        this.cacheName = cacheName;
    }

    @Override
    public String getLink(String linkText) {
        String target =  "/scdds/cache/"+cacheName+"/"+linkText;
        String html = "<a href=\""+target+"\">"+linkText+"</a>";
        return html;
    }
}
