package uk.co.octatec.scdds.net.router;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.queue.EventQueueDefaultImpl;

import java.io.FileInputStream;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Created by Jeromy Drake on 12/07/2016.
 *
 * The purpose of this class is to relieve the burned of publishing to multiple subscribers from
 * individual publishers. This class can be configured to subscribe to multiple publishers, and
 * 're-publisher' the data in a different cache (with the same name but prefixed with "R."
 * This could be useful if there are a large number of subscribers and there is a very powerful
 * machine that can be made available to run the Router. It could also be useful if a Cache
 * is behind a firewall and only one machine has access through the firewall.
 *
 * It is entirely optional whether or not you choose to use a Router, if you do use a Router,
 * you might want to consider eliminating the queuing in the source cache, i.e. pass in
 * ListenerNoneQueuingEventQueueFactory to your PublishingCacheBuilder and also set
 * GlobalDefaults.numberOfNetworkSendThreads = 0. (if you do use a Router, then all subscribers should
 * use the router-republished cache-name (prefixed with R.) rather than the actual cache-name
 *
 * To republish a cache named "CacheOne", these properties should be specified in the properties file that
 * is passed on the command line
 *
 *      republish.cache-name.CacheOne=CacheOne
 *      republish.CacheOne.in-registries=reg_1_host:port1,reg_2_host:port2
 *      republish.CacheOne.out-registries=reg_A_host:portA,reg_B_host:portB
 *
 * The registry specifications above can be a comma separated list of hots:port entries or just a single host:port entry.
 *
 * The in-registries property specified the registry(s) that know about the input cache, and the out-registries
 * specify the registry(s) that will be told about the output cache. These can be the same set of registries, but don't
 * have to be
 */
public class Router {

    private final static Logger log = LoggerFactory.getLogger(Router.class);

    public static void main(String[] args) throws Exception{
        if( args.length == 0 ) {
            log.error("usage: Router <properties-file>");
            System.exit(1);
        }

        log.info("loading properties file [{}]", args[0]);
        FileInputStream fis = new FileInputStream(args[0]);
        Properties properties = new Properties();
        properties.load(fis);
        fis.close();

        Set<Map.Entry<Object,Object>> entrySet = properties.entrySet();
        for(Map.Entry<Object,Object> e : entrySet ) {
            String key = e.getKey().toString();
            if( key.startsWith("republish.cache-name.")){
                String cacheName = e.getValue().toString();
                if( key.equals("republish.cache-name."+cacheName)) {
                    // we have found a property specifying a cache to republish, e.g.
                    //      republish.cache-name.Cache-A=Cache-A
                    log.info("found cache-name to republish [{}]", cacheName);
                    try {
                        Republisher republisher = new Republisher(cacheName, properties);
                        republisher.start();
                    }
                    catch( Exception x) {
                        log.error("failed to start a replublisher for [{}]", cacheName);
                    }
                }
            }
        }
        log.info("all re-publishers started");
    }
}
