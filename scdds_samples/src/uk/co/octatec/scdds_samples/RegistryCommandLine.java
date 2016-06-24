package uk.co.octatec.scdds_samples;
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
import java.net.InetSocketAddress;
import java.util.ArrayList;

/**
 * Created by Jeromy Drake on 20/06/2016.
 *
 * Utility to extract the Registry Location from the command line of the samples
 * i.e  -rhost:{host-name}   -rport:{port-number}
 */
public class RegistryCommandLine {

    public static ArrayList<InetSocketAddress> init(String[] args) {

        // get the location of the registry from the command line

        ArrayList<InetSocketAddress> registries = new ArrayList<InetSocketAddress>();

        String registryHost = "localhost";
        int registryPort = 9999; // default registry port for these samples
        for( String arg : args) {
            if( arg.startsWith("-rhost:")) {
                registryHost = arg.substring(7);
            }
            else if( arg.startsWith("-rport:")) {
                registryPort = Integer.parseInt(arg.substring(7));
            }
        }
        registries.add(new  InetSocketAddress(registryHost, registryPort));
        return registries;
    }
}
