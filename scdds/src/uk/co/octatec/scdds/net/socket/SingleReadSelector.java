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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static uk.co.octatec.scdds.ConditionalCompilation._DBG;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by Jeromy Drake on 12/05/2016.
 */
public class SingleReadSelector extends SingleSelector{

    // this is all to get round the fact that "sc.socket().setSoTimeout(timeoutMs);"
    // doesn't work on SocketChannels!

    SingleReadSelector(Session session, String name) throws IOException{
        super(session, SelectionKey.OP_READ, name);
    }

}
