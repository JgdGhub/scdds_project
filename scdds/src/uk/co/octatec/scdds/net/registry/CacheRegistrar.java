package uk.co.octatec.scdds.net.registry;

/**
 * Created by Jeromy Drake on 04/05/16
 */
public interface CacheRegistrar {
    void registerCache(String name, String host, int port, int numRetries);

    int getRegistriesContactedCount();
}
