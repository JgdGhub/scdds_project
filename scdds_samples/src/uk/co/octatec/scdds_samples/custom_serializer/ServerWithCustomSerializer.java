package uk.co.octatec.scdds_samples.custom_serializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.cache.Cache;
import uk.co.octatec.scdds.cache.PublishingCacheBuilder;
import uk.co.octatec.scdds_samples.basic_example.Data;
import uk.co.octatec.scdds_samples.basic_example.server.Server;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * Created by Jeromy Drake on 07/06/2016.
 */
public class ServerWithCustomSerializer extends Server {

    private final static Logger log = LoggerFactory.getLogger(Server.class);

    protected void start(List<InetSocketAddress> registries) throws InterruptedException {

        // once the custom serializer is written, it is is very simple to have it used - no code change
        // is required at all in the client and the only code change in the server is to pass in
        // the 'DataSerializerFactory'

        // create a cache
        log.info("### CREATE PUBLISHING CACHE USING CUSTOM SERIALIZER");
        PublishingCacheBuilder<String, Data> builder = new PublishingCacheBuilder<>(registries, new DataSerializerFactory());
        Cache<String, Data> cache = builder.build(CACHE_NAME); // clients will subscribe to this cache-name

        publishData(cache);

        // NB: both Server and  ServerWithCustomSerializer can run, they will both register themselves with the registry
        // and the registry will load-balance connections to them  - the same client-code can connect to either version
        // (its not recommended to have different types of serialization active at the same time, but it doesn't cause problems)
    }

}
