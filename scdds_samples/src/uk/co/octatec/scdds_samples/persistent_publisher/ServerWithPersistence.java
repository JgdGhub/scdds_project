package uk.co.octatec.scdds_samples.persistent_publisher;
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
import uk.co.octatec.scdds.cache.Cache;
import uk.co.octatec.scdds.cache.PublishingCacheBuilder;
import uk.co.octatec.scdds.cache.persistence.CacheLoaderPersisterFactory;
import uk.co.octatec.scdds.cache.persistence.KeyBasedCacheLoaderPersisterFactory;
import uk.co.octatec.scdds.cache.persistence.ObjectStoreCacheLoaderPersisterFactory;
import uk.co.octatec.scdds_samples.RegistryCommandLine;
import uk.co.octatec.scdds_samples.basic_example.Data;
import uk.co.octatec.scdds_samples.basic_example.server.Server;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jeromy Drake on 20/06/2016.
 */
public class ServerWithPersistence extends Server {

    private final static Logger log = LoggerFactory.getLogger(ServerWithPersistence.class);

    public static void main(String[] args) throws InterruptedException{

        log.info("### SERVER STARTING WITH PERSISTENCE args=[{}] [{}]", args, information);

        // first, get the location of the registry from the command line

        ArrayList<InetSocketAddress> registries = RegistryCommandLine.init(args);

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

        Server server = new ServerWithPersistence();
        server.start(registries);
    }

    public void start(List<InetSocketAddress> registries) throws InterruptedException {

        CacheLoaderPersisterFactory<String, Data> loaderPersisterFactory = new KeyBasedCacheLoaderPersisterFactory<>();

        // create a cache

        log.info("### CREATE PUBLISHING CACHE WITH PERSISTENCE");

        PublishingCacheBuilder<String, Data> builder = new PublishingCacheBuilder<>(registries, null, loaderPersisterFactory );
                                    // its always safe to pass null into the constructor and the default implementation will be used

        // using the default Loader/Persister means that all published entries are saved to a "flat-file data-store" using the
        // defined serializer, and at startup, the cache is pre-populated from the data-store.
        // Note: the flat-file  used for the data-store has the data-stamp in its name so that only 'todays' entries
        // will be populated back into the cache after a re-start

        Cache<String, Data> cache = builder.build(CACHE_NAME); // clients will subscribe to this cache-name

        // publish data to the cache

        publishData(cache);
    }
}
