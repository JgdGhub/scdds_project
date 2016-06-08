package uk.co.octatec.scdds.cache;

import uk.co.octatec.scdds.cache.ListenerEvent;
import uk.co.octatec.scdds.cache.ListenerEventFactory;

/**
 * Created by Jeromy Drake on 07/05/16
 */
public class ListenerEventFactoryDefaultImpl<K,T> implements ListenerEventFactory<K,T> {
    @Override
    public ListenerEvent<K,T>  create() {
        return new ListenerEvent<K,T>()  ;
    }
}
