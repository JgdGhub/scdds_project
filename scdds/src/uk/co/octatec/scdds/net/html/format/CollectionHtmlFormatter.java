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
import java.util.Collection;

/**
 * Created by Jeromy Drake on 12/06/2016.
 *
 * Render a collection as HTML
 */
public class CollectionHtmlFormatter<T> {

    private final Collection<T> collection;
    private final boolean valueAsString;
    private final BeanHtmlFormatter<T> beanFormatter;

    public CollectionHtmlFormatter(Collection<T> collection, boolean valueAsString) {
        this.collection = collection;
        this.valueAsString = valueAsString;
        this.beanFormatter = valueAsString ? null : new BeanHtmlFormatter<T>();
    }
    public CollectionHtmlFormatter(Collection<T> collection) {
        this(collection, true);
    }

    public String format(String tableTitle) {
        StringBuilder sb = new StringBuilder();

        sb.append("<table cellpadding=\"3\" border=\"1\">").append("<tbody>");
        sb.append("\r\n");

        if (tableTitle != null) {
            sb.append("<tr "+ FormatConstants.TAB_COLOUR +" >");
            sb.append("<td style=\"text-align:center\" >");
            sb.append("&nbsp;&nbsp;<b>");
            sb.append(tableTitle);
            sb.append("</b>&nbsp;&nbsp;");
            sb.append("</td>");
            sb.append("</tr>");
            sb.append("\r\n");
        }

        if( collection != null ) {
            int n = 0;
            for (T t : collection) {
                ++n;
                String value;
                if( valueAsString ) {
                    value = (t == null) ? "<b>((null))</b>" : t.toString();
                }
                else {
                    value = beanFormatter.format(t);
                }
                String stripe = n%2==0 ?  FormatConstants.STRIPE_1 : FormatConstants.STRIPE_2;
                sb.append("<tr "+stripe+" >");
                sb.append("<td>").append(value).append("</td>");
                sb.append("</tr>");
                sb.append("\r\n");
            }
            sb.append("<tr "+ FormatConstants.TAB_COLOUR +" >");
            sb.append("<td>row count:"+n+"</td>");
            sb.append("</tr>");
            sb.append("\r\n");
        }
        sb.append("</tbody>").append("</table>");
        sb.append("\r\n");
        String s = sb.toString();
        return s;
    }
}
