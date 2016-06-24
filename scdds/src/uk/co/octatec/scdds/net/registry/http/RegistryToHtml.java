package uk.co.octatec.scdds.net.registry.http;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.net.registry.Registry;
import uk.co.octatec.scdds.net.registry.RegistryServerSession;

import java.util.List;
import java.util.Map;

/**
 * Created by Jeromy Drake on 17/06/2016.
 */
public class RegistryToHtml {

    private final static Logger log = LoggerFactory.getLogger(RegistryToHtml.class);

    static public String format(Registry registry) {
        StringBuilder xml = new  StringBuilder();
        if( registry.size() == 0 ) {
            xml.append("<registry number-of-caches=\"0\" />");
        }
        else {
            xml.append("<registry number-of-caches=\"");
            xml.append(registry.size());
            xml.append("\" >\r\n");
            for (Map.Entry<String, List<Registry.Entry>> e : registry.getRegistryMap().entrySet()) {

                for (Registry.Entry entry : e.getValue()) {
                    xml.append("\t");
                    xml.append("<cache name=\"");
                    xml.append(entry.getCacheName());
                    xml.append("\" >\r\n");

                    xml.append("\t");
                    xml.append("\t");
                    xml.append("<server value=\"");
                    xml.append(entry.getHost());
                    xml.append("\" />\r\n");

                    xml.append("\t");
                    xml.append("\t");
                    xml.append("<server-port value=\"");
                    xml.append(entry.getPort());
                    xml.append("\" />\r\n");

                    xml.append("\t");
                    xml.append("\t");
                    xml.append("<mbean-port value=\"");
                    xml.append(entry.getMbeanPort());
                    xml.append("\" />\r\n");

                    xml.append("\t");
                    xml.append("\t");
                    xml.append("<group value=\"");
                    xml.append(entry.getGroup());
                    xml.append("\" />\r\n");
                    xml.append("\t");
                    xml.append("</cache>\r\n");

                }
             }
            xml.append("</registry>\r\n");
        }
        log.info("XML [{}}", xml);
        return xml.toString();
    }
}
