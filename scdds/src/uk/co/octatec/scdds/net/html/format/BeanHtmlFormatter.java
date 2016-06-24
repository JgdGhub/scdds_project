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
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Created by Jeromy Drake on 12/06/2016.
 *
 * Render a java object as HTML
 */
public class BeanHtmlFormatter<T> {

    static private class MethodNameComparator implements Comparator {
        @Override
        public int compare(Object o1, Object o2) {
            if( o1 == null || o2 == null ) {
                return 0;
            }
            Method m1 = (Method)o1;
            Method m2 = (Method)o2;
            String n1 = m1.getName();
            String n2 = m2.getName();
            return n1.compareTo(n2);
        }
    }

    static private final Comparator METHOD_NAME_COMPARATOR = new MethodNameComparator();

    public BeanHtmlFormatter() {
    }

    public String format(T bean) {
        return format(null, bean);
    }
    public String format(String title, T bean) {

        if(bean==null) {
            return "<b>((null-bean))</b>";
        }

        Class c = bean.getClass();

        StringBuilder sb = new StringBuilder();

        sb.append("<table cellpadding=\"3\" border=\"1\">").append("<tbody>");
        sb.append("\r\n");
        sb.append("<tr "+ FormatConstants.TAB_COLOUR +" >");
        sb.append("<td style=\"text-align:center\" colspan=\"2\">");
        sb.append("&nbsp;&nbsp;<b>");
        if( title == null ) {
            sb.append(c.getSimpleName());
        }
        else {
            sb.append(title);
        }
        sb.append("</b>&nbsp;&nbsp;");
        sb.append("</td>");
        sb.append("</tr>");
        sb.append("\r\n");

        sb.append("<tr "+FormatConstants.STRIPE_1+" >");
        sb.append("<td>").append("<b>Name</b>").append("</td>");
        sb.append("<td>").append("<b>Value</b>").append("</td>");
        sb.append("</tr>");
        sb.append("\r\n");


        Method[] mm = getMethods(c);
        for(Method m : mm ) {
            String name = m.getName();
            if( name.startsWith("get") || name.startsWith("is")) {
                if( name.equals("getClass")) {
                    continue;
                }
                if( m.getParameterTypes().length == 0 ) {
                    if( name.startsWith("g") ) {
                        name = name.substring(3);
                    }
                    else {
                        name = name.substring(2);
                    }
                    sb.append("<tr "+FormatConstants.STRIPE_2+" >");
                    sb.append("<td>").append(name).append("</td>");
                    try  {
                        Object value = m.invoke(bean);
                        sb.append("<td>").append(value).append("</td>");
                    }
                    catch( Exception e) {
                        sb.append("<td>").append(e.getMessage()).append("</td>");
                    }
                    sb.append("</tr>");
                    sb.append("\r\n");
                }
            }
        }
        sb.append("</tbody>").append("</table>");
        sb.append("\r\n");

        String s = sb.toString();
        return s;
    }

    private static Method[] getMethods(Class c) {
        return sortMethods(c.getMethods());
    }

    private static Method[] sortMethods(Method[] mm) {

        Method[] sm = new Method[mm.length]; // copy the array as retured by the class, then sort the copy
        for (int i=0; i < mm.length; i++) {
            sm[i] = mm[i];
        }
        Arrays.sort(sm, METHOD_NAME_COMPARATOR);
        return sm;
    }
}
