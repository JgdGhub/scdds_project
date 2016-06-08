package uk.co.octatec.scdds.net.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
            String request = bIO.readString();
            log.info("processing request [{}] from [{}]", request, clientAddr);
            String reply = processRequest(request);
            log.info("sending reply [{}] to [{}] request=[{}}", reply, clientAddr, request);
            bIO.writeString(reply);
        }
        catch( IOException e) {
            log.error("IOException processing registry session with [{}] [{}]", clientAddr, e.getMessage() );
        }
    }

    private String processRequest(String request) {
        String[] args = request.split("[:]");
        if( args[0].equals(RegistryServer.CMD_REGISTER)) {
            String name = args[1];
            String host = args[2];
            String port = args[3];
            String group = args[4];
            registry.add(name, host, port, group);
            return  RegistryServer.RSP_OK;
        }
        else if( args[0].equals(RegistryServer.CMD_FIND)) {
            String name = args[1];
            Registry.Entry instance = registry.find(name);
            if( instance == null ) {
                return RegistryServer.RSP_NOT_FOUND;
            }
            else {
                return RegistryServer.RSP_FOUND+":"+instance.host+":"+instance.port+":"+instance.group+":"+instance.connectionsCount;
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
