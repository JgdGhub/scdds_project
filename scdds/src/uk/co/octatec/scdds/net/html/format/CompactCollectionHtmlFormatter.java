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

import java.util.Collection;

/**
 * Created by Jeromy Drake on 14/06/2016.
 *
 * Render a collection as HTML in a compact-form, i.e. use multiple
 * columns to reduce the overall page-length
 */
public class CompactCollectionHtmlFormatter<T> {

    // format the collection in columns so that the page is not too large

    private static final int DFLT_ROW_LENGTH = 8;

    private final Collection<T> collection;
    private final boolean valueAsString;
    private final BeanHtmlFormatter<T> beanFormatter;
    private final LinkGenerator linkGenerator;


    public CompactCollectionHtmlFormatter(Collection<T> collection, LinkGenerator linkGenerator) {
        this.collection = collection;
        this.valueAsString = true;
        this.beanFormatter = valueAsString ? null : new BeanHtmlFormatter<T>();
        this.linkGenerator = linkGenerator==null ? new NoOpLinkGenerator() : linkGenerator;
    }

    public CompactCollectionHtmlFormatter(Collection<T> collection, boolean valueAsString) {
        this.collection = collection;
        this.valueAsString = valueAsString;
        this.beanFormatter = valueAsString ? null : new BeanHtmlFormatter<T>();
        this.linkGenerator = new NoOpLinkGenerator();
    }
    public CompactCollectionHtmlFormatter(Collection<T> collection) {
        this(collection, true);
    }

    public String format(String tableTitle) {
        return format(tableTitle, DFLT_ROW_LENGTH);
    }

    public String format(String tableTitle, int numColumns) {

        StringBuilder sb = new StringBuilder();

        sb.append("<table cellpadding=\"3\" border=\"1\">").append("<tbody>");
        sb.append("\r\n");

        if (tableTitle != null) {
            sb.append("<tr "+ FormatConstants.TAB_COLOUR +" >");
            sb.append("<td style=\"text-align:center\" colspan=\""+numColumns+"\">");
            sb.append("&nbsp;&nbsp;<b>");
            sb.append(tableTitle);
            sb.append("</b>&nbsp;&nbsp;");
            sb.append("</td>");
            sb.append("</tr>");
            sb.append("\r\n");
        }

        if( collection != null ) {
            int rows = 1;
            int columns = 1;
            for (T t : collection) {
                String value;
                if( valueAsString ) {
                    value = (t == null) ? "<b>((null))</b>" : t.toString();
                    value = linkGenerator.getLink(value);

                }
                else {
                    // no link is permitted if we are displaying a bean
                    value = beanFormatter.format(t);
                }
                String stripe = rows%2==0 ?  FormatConstants.STRIPE_1 : FormatConstants.STRIPE_2;
                if( columns == 1 ) {
                    sb.append("<tr " + stripe + " >");
                }
                sb.append("<td>").append(value).append("</td>");
                if( columns == numColumns ) {
                    sb.append("</tr>");
                    sb.append("\r\n");
                    columns = 1;
                    ++rows;
                }
                else {
                    sb.append(" ");
                    ++columns;
                }

            }
            sb.append("<tr "+ FormatConstants.TAB_COLOUR +" >");
            sb.append("<td colspan=\""+numColumns+"\">entry-count:"+collection.size()+", row-count:"+rows+"</td>");
            sb.append("</tr>");
            sb.append("\r\n");
        }
        sb.append("</tbody>").append("</table>");
        sb.append("\r\n");
        String s = sb.toString();
        return s;
    }
}
