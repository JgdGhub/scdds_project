package uk.co.octatec.scdds.net.router;
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
import uk.co.octatec.scdds.cache.*;
import uk.co.octatec.scdds.cache.publish.PropertyUtils;
import uk.co.octatec.scdds.cache.subscribe.ImmutableCache;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Properties;

/**
 * Created by Jeromy Drake on 12/07/2016.
 *
 * This class simply subscribes to a cache and re-publishes the entries in a new cache of the same name but
 * prefixed with "R.". A Streaming Subscription is made to the 'input' cache because an intermediate local
 * copy of the cache is not required. The input-registry (where input cache-name is known) and the output-registry
 * (where the output-cache-name will be advertised) are passed in properties "republish.{in-cache-name}.in-registries"
 * and "republish.{in-cache-name}.out-registries" [In both cases the in-cache-name is used]. These properties can
 * contain a single host:port entry or a comma separated list of entries; both properties can be the same,
 * but don't have to be.
 */
public class Republisher  {

    private final static Logger log = LoggerFactory.getLogger(Router.class);

    private String inCacheName;
    private String outCacheName;
    private List<InetSocketAddress> inRegistries;
    private List<InetSocketAddress> outRegistries;


    public Republisher(String cacheName, Properties properties) throws Exception {
        initFromProperties(cacheName, properties);
    }

    void initFromProperties(String cacheName, Properties properties) throws Exception {

        inCacheName = cacheName;
        outCacheName = "R."+inCacheName;

        String inRegistriesProp = "republish."+cacheName+".in-registries";
        String inRegistriesStr = properties.getProperty(inRegistriesProp);

        // nb: the in-registries and out-registries will likely be the same value,
        // but they don't have to be

        if( inRegistriesStr == null ) {
            log.error("inRegistriesProp [{}] does not exist in properties file", inRegistriesProp);
            throw new RuntimeException("missing property "+inRegistriesProp);
        }
        inRegistries = PropertyUtils.registryListFromString(inRegistriesStr);
        log.info("using input registiries [{}] for [{}]", inRegistries, inCacheName);


        String outRegistriesProp = "republish."+cacheName+".out-registries";
        String outRegistriesStr = properties.getProperty(outRegistriesProp);
        if( outRegistriesStr == null ) {
            log.error("outRegistriesProp [{}] does not exist in properties file", outRegistriesProp);
            throw new RuntimeException("missing property "+outRegistriesProp);
        }
        outRegistries = PropertyUtils.registryListFromString(outRegistriesStr);
        log.info("using output registiries [{}] for [{}]", outRegistries, outCacheName);
    }

    public void start() {
        start(new RepublishListenerFactoryImpl());
    }

    public void start(RepublishListenerFactory listenerFactory) {
        log.info("staring router: republishing [{}] to [{}]", inCacheName, outCacheName);
        PublishingCacheBuilder publisher = new PublishingCacheBuilder(outRegistries);
        Cache cache = publisher.build(outCacheName);
        CacheListener republishListener = listenerFactory.create(cache, inCacheName);
        SubscriptionStreamBuilder subscriber = new SubscriptionStreamBuilder(inRegistries);
        subscriber.subscribe(inCacheName, republishListener);
    }
}
