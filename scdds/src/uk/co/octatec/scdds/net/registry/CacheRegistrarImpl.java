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
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by Jeromy Drake on 01/05/2016.
 */
public class CacheRegistrarImpl implements CacheRegistrar {

    private final static Logger log = LoggerFactory.getLogger(CacheRegistrarImpl.class);

    enum Action {OK, WAIT, FAIL, RETRY}; // actions for future development

    Collection<InetSocketAddress> registries;

    private SecureRandom rand = new  SecureRandom();
    private int registriesContactedCount;

    static class ResgistryResponse {
        Action action;
        InetSocketAddress addr;
        public ResgistryResponse(String response, InetSocketAddress addr) {
            if( response.startsWith(RegistryServer.RSP_OK)) {
                this.action = Action.OK;
            }
            else if( response.startsWith(RegistryServer.RSP_WAIT)) {
                this.action = Action.WAIT;
            }
            else if( response.startsWith(RegistryServer.RSP_FAIL)) {
                this.action = Action.FAIL;
            }
            else if( response.startsWith(RegistryServer.RSP_RETRY)) {
                this.action = Action.RETRY;
            }
            else {
                log.error("unknown response from registry [{}] registry-addr=[{}]", response, addr );
                this.action = null;
            }
            this.addr = addr;
        }
    }

    public CacheRegistrarImpl(Collection<InetSocketAddress> registries)  {
        this.registries = registries;
    }

    @Override
    public void registerCache(String name, String host, int port, int numRetries) {
        registerCache(name, host, port, Registry.ANONYMOUS_GROUP, numRetries);
    }

    private Action registerCache(String name, String host, int port, String group, int numRetries) {
        log.info("registering [{}] [{}] [{}] numRetries=[{}]", name, host, port, numRetries);
        registriesContactedCount = 0;
        // keep this method private until support for the group-parameter is added
        if( !group.equals(Registry.ANONYMOUS_GROUP))  {

            log.error("High-Availability Groups are not yet supported group=[{}] cache-name=[{}]", group, name);
            throw new RuntimeException("High-Availability Groups are not yet supported");
        }

        Action a;
        while( true ) {
            a = doRegisterCache(RegistryServer.CMD_REGISTER, name, host, port, group, numRetries);
            // we expect all registries to always agree at the moment, the possibility
            // of disagreement will only arise when High-Availability Groups are implemented
            // (its still probably a useful check, 'just in case...' )
            if( a == Action.RETRY ){
                doRegisterCache(RegistryServer.CMD_UNREGISTER, name, host, port, group, numRetries);
                try {
                    sleepRandom(1000);
                }
                catch( InterruptedException e) {
                    log.error("thread interupted while resgistering cache [{}]", name);
                    throw new RuntimeException("thread interupted while resgistering cache", e);
                }
            }
            else {
                break;
            }
        }

        return a;
    }

    private Action doRegisterCache(String cmd, String name, String host, int port, String group, int numRetries) {

        ArrayList<ResgistryResponse>  results = new ArrayList<>();

        // register in all caches

        int count = 0;
        while( true ) {
            ++count;
            for (InetSocketAddress registryAddr : registries) {
                ResgistryResponse r = doRegisterCache(cmd, name, host, port, group, registryAddr);
                if (r != null) {
                    results.add(r);
                }
            }
            if( results.size() ==  0 ) {
                if (numRetries == 0 || count < numRetries) {
                    try {
                        log.info("cache-registrar: waiting to re-try and register cache...");
                        Thread.sleep(GlobalDefaults.CACHE_REGISTRY_RETRY_WAIT);
                    } catch (InterruptedException e) {
                        log.error("InterruptedException caught while waiting for next registry location retry, will quit register attempt");
                        throw new RuntimeException("Can't register cache " + name + " in any registry");
                    }
                }
                else {
                    log.info("can't contact registry and no more retries allowed");
                    throw new RuntimeException("Can't register cache " + name + " in any registry");
                }
            }
            else {
                break;
            }
        }

        Action currentAction = null;
        for(ResgistryResponse r : results ) {
            if( currentAction == null ) {
                currentAction = r.action;
            }
            else if( currentAction != r.action ) {
                log.warn("Registry disagreement detected [{}] v [{}] on [{}]", currentAction, r.action, r.addr);
            }
        }
        return Action.FAIL;
    }

    private ResgistryResponse doRegisterCache(String cmd, String name, String host, int port, String group, InetSocketAddress registryAddr) {
        Session sd = RegistryOpener.openRegistry(registryAddr, "Registrar");
        if( sd == null ) {
            return null;
        }
        try {
            ++registriesContactedCount;
            String request = cmd + ":" + name + ":" + host + ":" + port + ":" + group;
            log.info("registry request: [{}] to [{}]", request, registryAddr);
            BlockIO bIO = sd.getBlockIO();
            bIO.writeString(request);
            String response = bIO.readString();
            log.info("registry response: [{}] from [{}]", response, registryAddr);
            sd.close();
            return new ResgistryResponse(response, registryAddr);

        }
        catch( IOException x) {
            log.warn("IOException from registry [{}] while registering [{}] ", registryAddr, name);
            return null;
        }
    }

    int nextRandom(int limit) {
        int n = rand.nextInt();
        if( n < 0 ) {
            n = -n;
        }
        return n % limit;
    }

    private void sleepRandom(int limit) throws InterruptedException{
        Thread.sleep(nextRandom(limit));
    }

    @Override
    public int getRegistriesContactedCount() {
        return registriesContactedCount;
    }
}
