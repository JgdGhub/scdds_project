package uk.co.octatec.scdds_samples.basic_example.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.cache.SubscriptionCacheBuilder;
import uk.co.octatec.scdds.cache.subscribe.ImmutableCache;
import uk.co.octatec.scdds_samples.basic_example.Data;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jeromy Drake on 06/06/2016.
 */
public class Client {

    private final static Logger log = LoggerFactory.getLogger(Client.class);

    private static final String CACHE_NAME = "basic-data";

    public static void main(String[] args) throws InterruptedException{

        log.info("### CLIENT STARTINGG args=[{}]", args);

        // get the location of the registry from the command line

        String registryHost = "localhost";
        int registryPort = 9999;
        for( String arg : args) {
            if( arg.startsWith("-rhost:")) {
                registryHost = arg.substring(7);
            }
            else if( arg.startsWith("-rport:")) {
                registryPort = Integer.parseInt(arg.substring(7));
            }
        }

        // NOTE: a registry must be running at registryHost:registryPort on the network
        // to run a registry, dp thid...
        //   java -cp scdds-1.0.0.jar:slf4j-api-1.7.21.jar:slf4j-simple-1.7.21.jar  uk.co.octatec.scdds.net.registry.RegistryServer <port-number>

        ArrayList<InetSocketAddress> registries = new ArrayList<>();
        registries.add(new  InetSocketAddress(registryHost, registryPort));
        Client client = new Client();
        client.start(registries);
    }

    private void start(List<InetSocketAddress> registries) throws InterruptedException{

        log.info("### SUBSCRIBE TO THE CACHE");

        // subscribe to a cache
        SubscriptionCacheBuilder<String, Data> subscriber = new SubscriptionCacheBuilder<>(registries);
        ImmutableCache<String,Data> cache = subscriber.subscribe(CACHE_NAME); // the cache name must match the name ine the Server

        // add a listener - the listener will get an initial update and a stream of updates

        log.info("### LISTENING FOR DATA");

        DataListener dataListener = new DataListener();
        cache.addListener(dataListener);


    }
}
