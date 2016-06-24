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
import uk.co.octatec.scdds.net.html.links.NoOpLinkGenerator;

import java.util.Map;
import java.util.Set;

/**
 * Created by Jeromy Drake on 13/06/2016.
 *
 * Render a set of Map.Entry objects as HTML
 */
public class MapEntriesHtmlFormatter<K,T> {



    private final Set<Map.Entry<K,T>> mapEntries;
    private final boolean valueAsString;
    private final BeanHtmlFormatter<T> beanFormatter;
    private final LinkGenerator linkGenerator;

    public MapEntriesHtmlFormatter(Set<Map.Entry<K,T>> mapEntries, boolean valueAsString, LinkGenerator linkGenerator) {
        this.mapEntries = mapEntries;
        this.valueAsString = valueAsString;
        beanFormatter =   valueAsString ? null : new BeanHtmlFormatter<T>();
        this.linkGenerator = linkGenerator==null ? new NoOpLinkGenerator() : linkGenerator;
    }
    public MapEntriesHtmlFormatter(Set<Map.Entry<K,T>> mapEntries, boolean valueAsString) {
        this(mapEntries, valueAsString, null);
    }

    public MapEntriesHtmlFormatter(Set<Map.Entry<K,T>> mapEntries) {
        this(mapEntries, true, null);
    }

    public String format(String tableTitle) {
        return format(tableTitle, "Name", "Value");
    }
    public String format(String tableTitle, String nameTitle, String valueTitle) {
        StringBuilder sb = new StringBuilder();

        sb.append("<table cellpadding=\"3\" border=\"1\">").append("<tbody>");
        sb.append("\r\n");

        if (tableTitle != null) {
            sb.append("<tr "+ FormatConstants.TAB_COLOUR +" >");
            sb.append("<td style=\"text-align:center\" colspan=\"2\">");
            sb.append("&nbsp;&nbsp;<b>");
            sb.append(tableTitle);
            sb.append("</b>&nbsp;&nbsp;");
            sb.append("</td>");
            sb.append("</tr>");
            sb.append("\r\n");
        }

        sb.append("<tr "+FormatConstants.STRIPE_1+" >");
        sb.append("<td>").append("<b>"+nameTitle+"</b>").append("</td>");
        sb.append("<td>").append("<b>"+valueTitle+"</b>").append("</td>");
        sb.append("</tr>");
        sb.append("\r\n");


        if( mapEntries != null ) {
            int n = 0;
            for (Map.Entry<K,T> e: mapEntries) {
                ++n;
                String name = e.getKey().toString();
                T tvalue = e.getValue();
                String value;
                name = linkGenerator.getLink(name);
                if( valueAsString ) {
                    value = (tvalue == null) ? "<b>((null))</b>" : tvalue.toString();
                }
                else {
                    value = beanFormatter.format(tvalue);
                }
                String stripe = n%2==0 ?  FormatConstants.STRIPE_1 : FormatConstants.STRIPE_2;
                sb.append("<tr "+stripe+" >");
                sb.append("<td>").append(name).append("</td>");
                sb.append("<td>").append(value).append("</td>");
                sb.append("</tr>");
                sb.append("\r\n");
            }
            sb.append("<tr "+ FormatConstants.TAB_COLOUR +" >");
            sb.append("<td colspan=\"2\">element count:"+n+"</td>");
            sb.append("</tr>");
            sb.append("\r\n");
        }
        sb.append("</tbody>").append("</table>");
        sb.append("\r\n");
        String s = sb.toString();
        return s;
    }

}
