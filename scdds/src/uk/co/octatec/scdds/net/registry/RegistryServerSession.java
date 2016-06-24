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
import uk.co.octatec.scdds.net.registry.http.LightweightHttp;
import uk.co.octatec.scdds.net.registry.http.RegistryToHtml;
import uk.co.octatec.scdds.net.socket.BlockIO;
import uk.co.octatec.scdds.net.socket.Session;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Created by Jeromy Drake on 01/05/2016.
 */
public class RegistryServerSession implements Runnable {

    private final static Logger log = LoggerFactory.getLogger(RegistryServerSession.class);

    private Session sc;
    private Registry registry;
    private InetSocketAddress clientAddr;
    private final BlockIO bIO;

    RegistryServerSession(Session sc, Registry registry, InetSocketAddress clientAddr)  {
        this.sc = sc;
        bIO = sc.getBlockIO();
        this.registry = registry;
        this.clientAddr = clientAddr;
    }

    @Override
    public void run() {
        try {
            byte[] protoIdFlag = new byte[1];
            sc.read(protoIdFlag, 0, 1);
            if( protoIdFlag[0] != RegistryServer.PROTO_ID_FLAG ) {
                // not a registry protocol request, so assume its an HTTP request
                handleHttpRequest(protoIdFlag);
            }
            else {
                String request = bIO.readString();
                log.info("processing request [{}] from [{}]", request, clientAddr);
                String reply = processRequest(request);
                log.info("sending reply [{}] to [{}] request=[{}}", reply, clientAddr, request);
                bIO.writeString(reply);
            }
        }
        catch( IOException e) {
            log.error("IOException processing registry session with [{}] [{}]", clientAddr, e.getMessage() );
        }
    }

    private void handleHttpRequest(byte[] buff) throws  IOException{
        LightweightHttp http = new LightweightHttp(sc);
        String target = http.readRequest(buff);
        if( http.getHttpMethod().equalsIgnoreCase("GET") && target.equals("/scdds/registry/") || target.equals("/scdds/registry")) {
            String text = RegistryToHtml.format(registry);
            http.sendXmlReply(text);
        }
        else {
            http.replyNotFound();
        }
    }

    private String processRequest(String request) {

        String[] args = request.split("[:]");
        if( args[0].equals(RegistryServer.CMD_REGISTER)) {
            String name = args[1];
            String host = args[2];
            String port = args[3];
            String htmpPort = args[4];
            String group = args[5];
            registry.add(name, host, port, htmpPort, group);
            return  RegistryServer.RSP_OK;
        }
        else if( args[0].equals(RegistryServer.CMD_FIND)) {
            String name = args[1];
            Registry.Entry instance = registry.find(name);
            if( instance == null ) {
                return RegistryServer.RSP_NOT_FOUND;
            }
            else {
                return RegistryServer.RSP_FOUND+":"+instance.host+":"+instance.port+":"+instance.group+":"+instance.connectionsCount+":"+instance.htmlPort;
            }
        }
        else if( args[0].equals(RegistryServer.CMD_DUMP)) {
            // output the whole registry
            String registryDump = registry.dump();
            return RegistryServer.RSP_DUMP+":"+ registryDump;
        }

        log.error("unknown command [{}] received in registry", args[0]);

        return RegistryServer.RSP_INVALID+" ["+request+"]";
    }
}
