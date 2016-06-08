package uk.co.octatec.scdds.cache.publish;

import uk.co.octatec.scdds.cache.CacheImpl;
import uk.co.octatec.scdds.net.socket.BlockIO;
import uk.co.octatec.scdds.net.socket.Session;

/**
 * Created by Jeromy Drake on 04/05/16
 */
public interface GeneralRequestHandler {
    void registerSession(long sessionId, CachePublisher cachePublisher, Session sc);
    void unRegisterSession(long sessionId);
}
