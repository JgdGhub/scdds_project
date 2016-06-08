package uk.co.octatec.scdds.net.socket;

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
