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
