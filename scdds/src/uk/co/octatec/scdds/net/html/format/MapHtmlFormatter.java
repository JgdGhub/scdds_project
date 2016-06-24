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
import uk.co.octatec.scdds.net.html.links.LinkGenerator;

import java.util.Map;
import java.util.Set;

/**
 * Created by Jeromy Drake on 12/06/2016.
 *
 * Render a map as HTML
 */
public class MapHtmlFormatter<K,T> {

    private final MapEntriesHtmlFormatter<K,T> mapEntriesFormatter;

    public MapHtmlFormatter(Map<K,T> map, boolean valueAsString, LinkGenerator linkGenerator) {
        Set<Map.Entry<K,T>> mapEntries = map==null ? null : map.entrySet();
        mapEntriesFormatter = new MapEntriesHtmlFormatter<K,T>(mapEntries, valueAsString, linkGenerator);
    }
    public MapHtmlFormatter(Map<K,T> map, boolean valueAsString) {
        this(map, valueAsString, null);
    }

    public MapHtmlFormatter(Map<K,T> map) {
        this(map, true, null);
    }

    public String format(String tableTitle) {
        return format(tableTitle, "Name", "Value");
    }
    public String format(String tableTitle, String nameTitle, String valueTitle) {
        return mapEntriesFormatter.format(tableTitle, nameTitle, valueTitle);
    }
}
