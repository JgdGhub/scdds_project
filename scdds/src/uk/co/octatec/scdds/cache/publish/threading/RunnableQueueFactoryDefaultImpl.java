package uk.co.octatec.scdds.cache.publish.threading;

/**
 * Created by Jeromy Drake on 17/05/2016.
 */
public class RunnableQueueFactoryDefaultImpl implements RunnableQueueFactory {
    @Override
    public RunnableQueue create() {
        return new RunnableQueueDefaultImpl() ;
    }
}
