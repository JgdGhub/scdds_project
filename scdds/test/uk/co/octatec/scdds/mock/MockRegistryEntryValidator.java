package uk.co.octatec.scdds.mock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.net.registry.RegistryEntryValidator;
import uk.co.octatec.scdds.utilities.AwaitParams;

/**
 * Created by Jeromy Drake on 10/05/2016.
 */
public class MockRegistryEntryValidator implements RegistryEntryValidator {

    private static final Logger log = LoggerFactory.getLogger(MockRegistryEntryValidator.class);

    volatile public boolean validateCalled;
    volatile public String invalidServer = "";

    @Override
    public void validate(RegistryEntryValidator.Validatable instance) {
        log.info("validate: [{}]", instance);

        if (instance.getHost().equals(invalidServer) || invalidServer.equals("*")) {
            instance.setInvalid();
            log.info("MockRegistryEntryValidator setting invalid instance [{}]", instance);
        }
        validateCalled = true;
    }

    public void awaitValidateCalled()  throws InterruptedException {
        for(int i=0; i < AwaitParams.AWAIT_LOOP_COUNT; i++) {
            if( validateCalled ) {
                break;
            }
            Thread.sleep(AwaitParams.AWAIT_SLEEP_TIME);
        }
        if( !validateCalled ) {
            log.warn("*** awaitValidateCalled: wait failed");
        }
    }
}
