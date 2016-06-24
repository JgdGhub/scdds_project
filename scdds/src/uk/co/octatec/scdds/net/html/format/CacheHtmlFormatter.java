package uk.co.octatec.scdds.net.html.format;
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
import uk.co.octatec.scdds.cache.ImmutableEntry;
import uk.co.octatec.scdds.net.html.links.LinkGenerator;

import java.util.Map;
import java.util.Set;

/**
 * Created by Jeromy Drake on 14/06/2016.
 *
 * Render a cache as HTML
 */
public class CacheHtmlFormatter<K,T extends ImmutableEntry> {
    private final MapEntriesHtmlFormatter<K,T> mapEntriesFormatter;

    public CacheHtmlFormatter(Cache<K,T> cache, boolean valueAsString, LinkGenerator linkGenerator) {
        Set<Map.Entry<K,T>> mapEntries = cache==null ? null : cache.entrySet();
        mapEntriesFormatter = new MapEntriesHtmlFormatter<K,T>(mapEntries, valueAsString, linkGenerator);
    }
    public CacheHtmlFormatter(Cache<K,T> cache, boolean valueAsString) {
        this(cache, valueAsString, null);
    }

    public CacheHtmlFormatter(Cache<K,T> cache) {
        this(cache, true, null);
    }

    public String format(String tableTitle) {
        return format(tableTitle, "Name", "Value");
    }
    public String format(String tableTitle, String nameTitle, String valueTitle) {
        return mapEntriesFormatter.format(tableTitle, nameTitle, valueTitle);
    }
}
