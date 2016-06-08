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

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Set;

import static uk.co.octatec.scdds.ConditionalCompilation._DBG;

/**
 * Created by Jeromy Drake on 18/05/2016.
 */
public class SingleSelector {

    // this is all to get round the fact that "sc.socket().setSoTimeout(timeoutMs);"
    // doesn't work on SocketChannels!

    private final Logger log = LoggerFactory.getLogger(SingleReadSelector.class);

    private final Selector selector;
    private final SelectionKey selectionKey;
    private final int selectionOp;
    private final String name;

    SingleSelector(Session session, int selectipnOp, String name) throws IOException {
        selector = SelectorProvider.provider().openSelector();
        this.selectionOp = selectipnOp;
        this.name = name;
        selectionKey = session.registerSelector(selector, selectipnOp, null);
    }

    void dispose() {
        try {
            log.info("{}: singleReadSelector: dispose", name);
            selectionKey.cancel();
            selector.close();
        }
        catch( IOException e) {
            log.warn(name+": exception from SingleReadSelector.dispose [{}]", e.getMessage());
        }
    }

    public void awaitReady() throws IOException {
        if( _DBG ) {
            log.debug("{}: awaitReady [{}]...", name, selectionOp);
        }
        while( !awaitReady(0) ) {
            /* wait for a read to be ready*/;
        }
        if( _DBG ) {
            log.debug("{}: awaitReady [{}]...done", name, selectionOp);
        }
    }

    public boolean awaitReady(long timeout) throws IOException {
        if( _DBG  ) {
            if( timeout > 0)  {
                log.debug("{} awaitReady selectionOp={} timeout={}", name, selectionOp, timeout);
            }
        }

        int n = selector.select(timeout);
        if (n == 0) {
            return false;
        }
        int actualCount = 0;
        Set<SelectionKey> keySet = this.selector.selectedKeys();
        Iterator keyIterator = keySet.iterator();
        while (keyIterator.hasNext()) {
            SelectionKey key = (SelectionKey) keyIterator.next();
            keyIterator.remove();
            if( _DBG  ) {
                // this can't happen, but check for it in debug-mode
                if( (key.readyOps() & selectionOp) != selectionOp ) {
                    log.warn("{}: UNEXPECTED WRONG SelectionKey Ready OP, got {} wanted {} ", name, key.readyOps(), selectionOp );
                }
            }
            if ( key.isValid()) {
                ++actualCount;
            }
        }
        if (actualCount > 1) {
            log.error("{}: unexpectedly found more keys than can exist", name);
        }

        if( _DBG ) {
            if( timeout > 0)  {
                log.debug("{}: awaitReady done, selectionOp={} timeout={} ready-count={}", name, selectionOp, timeout, actualCount);
            }
        }

        return actualCount>0;
    }

}

