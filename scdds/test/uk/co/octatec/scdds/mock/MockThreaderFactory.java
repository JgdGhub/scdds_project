package uk.co.octatec.scdds.mock;

import uk.co.octatec.scdds.cache.publish.threading.Threader;
import uk.co.octatec.scdds.cache.publish.threading.ThreaderFactory;

/**
 * Created by Jeromy Drake on 17/05/2016.
 */
public class MockThreaderFactory implements ThreaderFactory {
    @Override
    public Threader getInstance() {
        return null;
    }
}
