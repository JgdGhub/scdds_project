package uk.co.octatec.scdds.mock;

import uk.co.octatec.scdds.cache.publish.GeneralRequestHandler;
import uk.co.octatec.scdds.cache.publish.GeneralRequestHandlerFactory;

/**
 * Created by Jeromy Drake on 07/05/16
 */
public class MockGeneralRequestHandlerFactory implements GeneralRequestHandlerFactory {
    @Override
    public GeneralRequestHandler getInstance() {
        return new MockGeneralRequestHandler();
    }
}
