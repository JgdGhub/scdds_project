package uk.co.octatec.scdds.net.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.cache.publish.CachePublisher;
import uk.co.octatec.scdds.cache.publish.PropertyUtils;
import uk.co.octatec.scdds.net.socket.ClientConnector;
import uk.co.octatec.scdds.net.socket.ClientConnectorImpl;
import uk.co.octatec.scdds.net.socket.Session;

import java.util.Properties;

/**
 * Created by Jeromy Drake on 01/05/2016.
 */
public class RegistryEntryValidatorImpl implements RegistryEntryValidator {

    private final Logger log = LoggerFactory.getLogger(RegistryEntryValidatorImpl.class);

    private ClientConnector connector;

    public RegistryEntryValidatorImpl(ClientConnector connector) {
        this.connector = connector;
    }

    public RegistryEntryValidatorImpl() {
        this(new ClientConnectorImpl("RegValidator"));
    }

    @Override
    public void validate(Validatable instance) {
        // returns current connection count, or  INVALID if the cache doesn't respond
        //return Registry.Instance.INVALID_FLAG;

        Session sc = null;
        try {
            long readTimeoutMs = 5000;
            log.info("validate: open connection to cache server instance for [{}] on [{}:{}]", instance.getCacheName(), instance.getHost(), instance.getPort());
            //sc = connector.connect(ClientConnector.BLOCKING_MODE, instance.getHost(), instance.getPort(), 1, 1);
            sc = connector.connect(ClientConnector.NONE_BLOCKING_MODE, instance.getHost(), instance.getPort(), 1, 1);
            sc.enableReadTimeout();
            String loadRequest = PropertyUtils.createPropertyString(CachePublisher.ARG_REQUEST, CachePublisher.RQST_LOAD_CHECK,
                    CachePublisher.ARG_CACHE_NAME, instance.getCacheName(),
                    CachePublisher.ARG_PORT, instance.getPort(),
                    CachePublisher.ARG_TIMESTAMP, System.currentTimeMillis());

            log.info("validate: request load-check [{}] from [{}:{}]", loadRequest, instance.getHost(), instance.getPort());
            sc.getBlockIO().writeString(loadRequest);
            boolean readready = sc.awaitReadReady(readTimeoutMs);
            if ( readready) {
                String response = sc.getBlockIO().readString();
                log.info("validate: response from server [{}]", response);
                Properties properties = PropertyUtils.getPropertiesFromString(response);
                String argRequest = properties.getProperty(CachePublisher.ARG_REQUEST);
                String cacheName = properties.getProperty(CachePublisher.ARG_CACHE_NAME);
                String error = properties.getProperty(CachePublisher.ARG_ERROR);
                String load = properties.getProperty(CachePublisher.ARG_LOAD);
                if (argRequest == null || !argRequest.equals(CachePublisher.RQST_LOAD_CHECK)) {
                    log.error("validate: unexpected response, assume invalid");
                    instance.setInvalid();
                } else if (cacheName == null || !cacheName.equals(instance.getCacheName())) {
                    log.error("validate: unexpected cache-name, assume invalid");
                    instance.setInvalid();
                } else if (error != null) {
                    log.error("validate: unexpected error, set invalid [{}]", error);
                    instance.setInvalid();
                } else if (load != null) {
                    instance.setConnectionCount(Integer.parseInt(load));
                    log.info("validate: set active-connectionsCount for [{}] on [{}:{}] to ", instance.getCacheName(), instance.getHost(), instance.getPort(), instance.getConnectionCount());
                }
            }
            else { //(SocketTimeoutException x){
                log.info("validate: SocketTimeoutException while awaiting response, entry is invalid");
                instance.setInvalid();
            }
        }
        catch( Throwable t ) {
            log.info("validate: unexpected exception [{}] while checking load, entry is invalid", t.getMessage());
            instance.setInvalid();
        }
        finally {
            if( sc != null ) {
                log.info("validate: close connection to cache server instance [{}] [{}:{}]", instance.getCacheName(), instance.getHost(), instance.getPort());
                sc.close();
            }
        }
    }
}
