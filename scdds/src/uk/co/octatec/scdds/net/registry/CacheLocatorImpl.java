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
import uk.co.octatec.scdds.GlobalDefaults;
import uk.co.octatec.scdds.net.socket.BlockIO;
import uk.co.octatec.scdds.net.socket.BlockIoImpl;
import uk.co.octatec.scdds.net.socket.Session;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

/**
 * Created by Jeromy Drake on 01/05/2016.
 */
public class CacheLocatorImpl implements CacheLocator {

    private final static Logger log = LoggerFactory.getLogger(CacheLocatorImpl.class);

    private List<InetSocketAddress> registries;
    private int registriesContactedCount;
    public CacheLocatorImpl(List<InetSocketAddress> registries)  {
        this.registries = registries;
    }

    @Override
    public InetSocketAddress locate(String name, int numRetries)  {

        // locate the names cache by asking the registry for its location
        registriesContactedCount = 0;
        int count = 0;
        while( true ) {
            ++count;
            log.info("locating [{}] numRetries=[{}] retry-count=[{}]...", name, numRetries, count);
            for (InetSocketAddress registryAddr : registries) {
                InetSocketAddress cacheAddr = doLocate(name, registryAddr);
                if (cacheAddr != null) {
                    // we found a registry that knows about the cache
                    return cacheAddr;
                }
            }
            if( numRetries == 0 || numRetries < count ) {
                try {
                    log.info("cache-locator: waiting to re-try and locate cache...");
                    Thread.sleep(GlobalDefaults.CACHE_REGISTRY_RETRY_WAIT);
                }
                catch( InterruptedException x) {
                    log.error("InterruptedException caught while waiting for next registry location retry, will quit locate attempt");
                    break;
                }
            }
            else {
                log.info("no more retries to locate a registry");
                break;
            }
        }
        log.warn("Can't locate cache "+name+" in any registry");
        return null;
    }

    private InetSocketAddress doLocate(String name, InetSocketAddress registryAddr) {
        Session sd = RegistryOpener.openRegistry(registryAddr, "RegLocator");
        if( sd == null ) {
            return null;
        }
        try {
            ++registriesContactedCount;
            BlockIO bIO = sd.getBlockIO();
            String request = RegistryServer.CMD_FIND + ":" + name;
            log.info("registry request: [{}]", request);
            bIO.writeString(request);
            String reply = bIO.readString();
            sd.close();
            log.info("registry response: [{}] from [{}]", reply, registryAddr);
            if (reply.startsWith(RegistryServer.RSP_NOT_FOUND) || reply.startsWith(RegistryServer.RSP_FAIL) || reply.startsWith(RegistryServer.RSP_INVALID) ) {
                return null;
            }
            else if( reply.startsWith(RegistryServer.RSP_FOUND)){
                String ss[] = reply.split("[:]");
                String host = ss[1];
                int port = Integer.parseInt(ss[2]);
                InetSocketAddress cacheAddr = new InetSocketAddress(host, port);
                log.info("registry at [{}] located cache [{}] at [{}]", registryAddr, name, cacheAddr);
                return cacheAddr;
            }
            else {
                log.error("unexpected response from registry ["+reply+"]");
                return null;
            }
        }
        catch(IOException x) {
            log.warn("IOException from registry [{}] while locating [{}] ", registryAddr, name);
            return null;
        }
    }

    @Override
    public int getRegistriesContactedCount() {
        return registriesContactedCount;
    }
}
