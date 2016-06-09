package uk.co.octatec.scdds_samples.basic_example.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.cache.Cache;
import uk.co.octatec.scdds.cache.PublishingCacheBuilder;
import uk.co.octatec.scdds_samples.basic_example.Data;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Jeromy Drake on 06/06/2016.
 */
public class Server {

    /* REMEMBER TO START THE RegistryServer FIRST (on port 9999 or some other port of your choosing */
    /* this can be done using the "Registry-Server" configuration from the InteliJ Menu-bar */

    private final static Logger log = LoggerFactory.getLogger(Server.class);

    protected static final String CACHE_NAME = "basic-data";

    private static String information = (new Date()).toString();

    public static void main(String[] args) throws InterruptedException{

        log.info("### SERVER STARTING args=[{}] [{}]", args, information);

        // first, get the location of the registry from the command line

        String registryHost = "localhost";
        int registryPort = 9999; // default registry port for these samples
        for( String arg : args) {
            if( arg.startsWith("-rhost:")) {
                registryHost = arg.substring(7);
            }
            else if( arg.startsWith("-rport:")) {
                registryPort = Integer.parseInt(arg.substring(7));
            }
        }

        // NOTE: a registry must be running at registryHost:registryPort on the network
        // this can be done using the "Registry-Server" configuration from the InteliJ Menu-bar, alternatively...
        //
        // to run a registry, do this...
        //   java -cp scdds-1.0.0.jar:slf4j-api-1.7.21.jar:slf4j-simple-1.7.21.jar  uk.co.octatec.scdds.net.registry.RegistryServer <port-number>
        //
        // both the client and server needed the address of the registry
        //
        // note: for resilience, multiple registries can be running on multiple machines, in which case
        // the client and server should be given the full list of all registries available

        ArrayList<InetSocketAddress> registries = new ArrayList<>();
        registries.add(new InetSocketAddress(registryHost, registryPort));

        Server server = new Server();
        server.start(registries);
    }

    protected void start(List<InetSocketAddress> registries) throws InterruptedException {

        // create a cache

        log.info("### CREATE PUBLISHING CACHE");
        PublishingCacheBuilder<String, Data> builder = new PublishingCacheBuilder<>(registries);
        Cache<String, Data> cache = builder.build(CACHE_NAME); // clients will subscribe to this cache-name

        // publish data to the cache

        publishData(cache);
    }

    protected void publishData(Cache<String, Data> cache) throws InterruptedException{

        log.info("### READY TO PUBLISH DATA");

        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

        // now publish data
        while(true) {
            for(int i = 1; i<=1000; i++) {
                String key = "data_"+i;
                cache.put(key, new Data(i, information));
                Thread.sleep(0);
            }
            Thread.sleep(10);
        }

        // any client that subscribes will get an initial download of the current state of the cache followed by a stream of updates

    }
}
