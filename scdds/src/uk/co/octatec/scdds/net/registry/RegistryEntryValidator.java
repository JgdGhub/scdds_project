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
import java.util.List;

/**
 * Created by Jeromy Drake on 01/05/2016.
 */
public interface RegistryEntryValidator {
    interface Validatable {

        String getHost();
        int getPort();
        int getMbeanPort();
        String getCacheName();
        String getGroup();
        void setInvalid();
        void setConnectionCount(int count);
        int getConnectionCount();

    }

    void validate(Validatable instance);
}
