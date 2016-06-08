package uk.co.octatec.scdds.mock;

import uk.co.octatec.scdds.net.registry.CacheLocator;

import java.net.InetSocketAddress;

/**
 * Created by Jeromy Drake on 04/05/16
 */
public class MockCacheLocator implements CacheLocator {

    final int port;
    public final String hostname;

    public MockCacheLocator() {
        this("Server1", 1000);
    }
    public MockCacheLocator(String hostname, int port) {
        this.port = port;
        this.hostname = hostname;
    }

    @Override
    public InetSocketAddress locate(String name, int numRetries) {
        return new InetSocketAddress(hostname, port);
    }

    @Override
    public int getRegistriesContactedCount() {
        return 1;
    }
}
