package uk.co.octatec.scdds_samples.stream_based_client;
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
import uk.co.octatec.scdds.cache.SubscriptionStreamBuilder;
import uk.co.octatec.scdds.cache.publish.CacheFilter;
import uk.co.octatec.scdds.cache.subscribe.ImmutableCache;
import uk.co.octatec.scdds_samples.RegistryCommandLine;
import uk.co.octatec.scdds_samples.basic_example.Data;
import uk.co.octatec.scdds_samples.basic_example.client.Client;
import uk.co.octatec.scdds_samples.basic_example.client.DataListener;
import uk.co.octatec.scdds_samples.filtered_client.OddFilter;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jeromy Drake on 21/06/2016.
 */
public class StreamedClient extends Client {

    private final static Logger log = LoggerFactory.getLogger(StreamedClient.class);

    public static void main(String[] args) throws InterruptedException{

        log.info("### STREAM BASED CLIENT STARTINGG args=[{}]", args);

        // NOTE: a registry must be running at registryHost:registryPort on the network
        // to run a registry, dp thid...
        //   java -cp scdds-1.0.0.jar:slf4j-api-1.7.21.jar:slf4j-simple-1.7.21.jar  uk.co.octatec.scdds.net.registry.RegistryServer <port-number>

        ArrayList<InetSocketAddress> registries = RegistryCommandLine.init(args);

        Client client = new StreamedClient();
        client.start(registries);
    }

    public void start(List<InetSocketAddress> registries) throws InterruptedException{

        log.info("### SUBSCRIBE TO THE CACHE USING A STREAM BASED CLIENT");

        // subscribe to a cache

        // the 'streamed client' does not get a local copy of the cache, instead it it just gets
        // a stream of updates that go to the listener - thats the only differennce, the memory footprint
        // of the client will be less as it doesn't have a local copy of the cache, but it won't be able
        // to query the cache for a particular value

        SubscriptionStreamBuilder<String, Data> subscriber = new SubscriptionStreamBuilder<>(registries);
        DataListener dataListener = new DataListener();
        subscriber.subscribe(CACHE_NAME, dataListener); // the cache name must match the name ine the Server

        // the listener will now get updates as soon as they are available
    }
}
