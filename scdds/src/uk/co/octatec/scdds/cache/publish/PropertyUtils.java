package uk.co.octatec.scdds.cache.publish;
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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by Jeromy Drake on 04/05/16
 */
public class PropertyUtils {

    private final static Logger log = LoggerFactory.getLogger(PropertyUtils.class);

    public static Properties getPropertiesFromString(String string) {
        Properties properties = new Properties();
        String[] initArgs = string.split("[:]");
        for(String arg : initArgs ) {
            String[] ss = arg.split("=");
            if( ss.length == 2 ) {
                properties.put(ss[0], ss[1]);
            }
            else {
                properties.put(arg, null);
            }

        }
        return properties;
    }

    public static String createPropertyString(Object...args) {
        int count = args.length;
        if( count%2 != 0 ) {
            log.error("createPropertyString found un-even number of arguments, last one will be ignored [{}]", args);
            --count;
        }
        StringBuilder sb = new  StringBuilder();
        for(int i=0; i<count; i+=2 ) {
            sb.append(args[i]);
            sb.append("=");
            sb.append(args[i+1]);
            if( i+2 < count ) {
                sb.append(":");
            }
        }
        return sb.toString();
    }

    public static List<InetSocketAddress> registryListFromString(String registries) throws Exception {
        ArrayList<InetSocketAddress>  registryList = new ArrayList<InetSocketAddress>();
        String entryDelim = "[,]";
        if( registries.contains(";") && !registries.contains(",")) {
            entryDelim = "[;]";
        }
        String[] rlist = registries.split(entryDelim);
        for(String rentry : rlist ) {
            String[] hostAndPort = rentry.split("[:]");
            log.info("adding registry entry [{}]",rentry);
            InetSocketAddress addr = new InetSocketAddress(hostAndPort[0], Integer.parseInt(hostAndPort[1]));
            registryList.add(addr);
        }
        return registryList;
    }
}
