package uk.co.octatec.scdds_samples.basic_example.client;
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
import uk.co.octatec.scdds.cache.SubscriptionCacheBuilder;
import uk.co.octatec.scdds.cache.subscribe.ImmutableCache;
import uk.co.octatec.scdds_samples.RegistryCommandLine;
import uk.co.octatec.scdds_samples.basic_example.Data;
import uk.co.octatec.scdds_samples.basic_example.server.Server;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jeromy Drake on 06/06/2016.
 */
public class Client {

    /* REMEMBER TO START THE RegistryServer FIRST (on port 9999 or some other port of your choosing */
    /* this can be done using the "Registry-Server" configuration from the InteliJ Menu-bar */

    private final static Logger log = LoggerFactory.getLogger(Client.class);

    protected static final String CACHE_NAME = "basic-data";

    public static void main(String[] args) throws InterruptedException{

        log.info("### CLIENT STARTING args=[{}]", args);

        // NOTE: a registry must be running at registryHost:registryPort on the network
        // to run a registry, dp thid...
        //   java -cp scdds-1.0.0.jar:slf4j-api-1.7.21.jar:slf4j-simple-1.7.21.jar  uk.co.octatec.scdds.net.registry.RegistryServer <port-number>

        ArrayList<InetSocketAddress> registries = RegistryCommandLine.init(args);

        Client client = new Client();
        client.start(registries);
    }


    public void start(List<InetSocketAddress> registries) throws InterruptedException{

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
