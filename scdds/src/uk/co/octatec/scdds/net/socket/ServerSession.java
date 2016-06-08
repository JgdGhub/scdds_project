package uk.co.octatec.scdds.net.socket;
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
import java.io.IOException;

/**
 * Created by Jeromy Drake on 30/04/2016.
 */
public interface ServerSession {
    void start() throws IOException;
    void stop();
    void start(int port) throws IOException;
    Session acceptBlockingClient() throws IOException;
    Session acceptNoneBlockingClient() throws IOException;
    int getPort();

}