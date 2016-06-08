package uk.co.octatec.scdds.net.socket;

import java.io.IOException;
import java.nio.channels.SelectionKey;

/**
 * Created by Jeromy Drake on 18/05/2016.
 */
public class SingleWriteSelector extends SingleSelector {

    SingleWriteSelector(Session session, String name) throws IOException {
        super(session, SelectionKey.OP_WRITE, name);
    }
}
