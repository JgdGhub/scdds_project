package uk.co.octatec.scdds.cache.publish;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
}
