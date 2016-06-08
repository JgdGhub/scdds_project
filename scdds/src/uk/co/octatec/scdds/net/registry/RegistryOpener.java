package uk.co.octatec.scdds.net.registry;

import uk.co.octatec.scdds.GlobalDefaults;
import uk.co.octatec.scdds.net.socket.ClientConnector;
import uk.co.octatec.scdds.net.socket.ClientConnectorImpl;
import uk.co.octatec.scdds.net.socket.Session;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;

/**
 * Created by Jeromy Drake on 01/05/2016.
 */
public class RegistryOpener {

    static public Session openRegistry(InetSocketAddress addr, String name) {
        try {
            ClientConnector con = new ClientConnectorImpl(name);
            return con.connect(ClientConnector.BLOCKING_MODE, addr.getHostName(), addr.getPort(), 1, 1);
        }
        catch( Exception e) {
            return null;
        }
    }
}
