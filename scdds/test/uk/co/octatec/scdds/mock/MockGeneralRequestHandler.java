package uk.co.octatec.scdds.mock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.cache.publish.CachePublisher;
import uk.co.octatec.scdds.cache.publish.GeneralRequestHandler;
import uk.co.octatec.scdds.net.socket.Session;
import uk.co.octatec.scdds.utilities.AwaitParams;

/**
 * Created by Jeromy Drake on 04/05/16
 */
public class MockGeneralRequestHandler implements GeneralRequestHandler {

    private final static Logger log = LoggerFactory.getLogger(MockGeneralRequestHandler.class);

    public volatile long sessionId;
    public volatile int registerSessionCount;
    public volatile int unRegisterSessionCount = 0;
    @Override
    public void registerSession(long sessionId, CachePublisher cachePublisher, Session sc){
        this.sessionId = sessionId;
        ++registerSessionCount;
    }

    @Override
    public void unRegisterSession(long sessionId) {
        ++unRegisterSessionCount;
    }

    public void awaitRegisteredSessionCount(int n) throws InterruptedException{
        for (int i = 0; i < AwaitParams.AWAIT_LOOP_COUNT; i++) {
            if (registerSessionCount == n) {
                break;
            } else {
                Thread.sleep(AwaitParams.AWAIT_SLEEP_TIME);
            }
        }
        if( registerSessionCount < n ) {
            log.warn("*** awaitRegisteredSessionCount: wait failed");
        }
    }

    public void awaitUnRegisteredSessionCount(int n) throws InterruptedException{
        for (int i = 0; i < AwaitParams.AWAIT_LOOP_COUNT; i++) {
            if (unRegisterSessionCount == n) {
                break;
            } else {
                Thread.sleep(AwaitParams.AWAIT_SLEEP_TIME);
            }
        }
        if( registerSessionCount < n ) {
            log.warn("*** awaitUnRegisteredSessionCount: wait failed");
        }
    }
}
