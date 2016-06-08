package uk.co.octatec.scdds.net.registry;

import java.net.InetSocketAddress;

/**
 * Created by Jeromy Drake on 04/05/16
 */
public interface CacheLocator {
    InetSocketAddress locate(String name, int numRetries);

    int getRegistriesContactedCount();
}
