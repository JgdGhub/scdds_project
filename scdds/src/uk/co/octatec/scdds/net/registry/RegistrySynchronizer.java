package uk.co.octatec.scdds.net.registry;
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
import uk.co.octatec.scdds.net.socket.BlockIO;
import uk.co.octatec.scdds.net.socket.Session;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Created by Jeromy Drake on 02/05/2016.
 */
public class RegistrySynchronizer {

    private final static Logger log = LoggerFactory.getLogger(RegistryServer.class);

    private final String host;
    private final int port;

    public RegistrySynchronizer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void synchronize(Registry registry) throws IOException {
        InetSocketAddress addr = new InetSocketAddress(host, port);
        log.info("syncronizing current registry with remote [{}]", addr);
        Session sc = RegistryOpener.openRegistry(addr, "RegistrySync");
        if( sc == null ) {
            log.error("can't connect to remote registry [{}]", addr);
            throw new IOException("can't connect to registry at ["+addr+"] to do sync");
        }
        sc.write(RegistryServer.PROTO_ID, 0, 1);
        BlockIO bIO = sc.getBlockIO();
        bIO.writeString(RegistryServer.CMD_DUMP);
        String registryDump = bIO.readString();
        sc.close();
        if( !registryDump.startsWith(RegistryServer.RSP_DUMP) ) {
            log.error("didn't get dump response from registry, got [{}]", registryDump);
            throw new IOException("didn't get registry dump response from remote");
        }
        applyRegistryDump(registry, registryDump.substring(RegistryServer.RSP_DUMP.length()+1)) ;
    }

    void applyRegistryDump(Registry registry, String registryDump) {
        registry.clear();
        String[] tableDump = registryDump.split("[\n]");
        for(String entryStr : tableDump ) {
            Registry.Entry newEntry =  Registry.Entry.fromShortString(entryStr);
            registry.addEntry(newEntry);
        }
    }
}
