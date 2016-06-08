package uk.co.octatec.scdds.mock;
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
