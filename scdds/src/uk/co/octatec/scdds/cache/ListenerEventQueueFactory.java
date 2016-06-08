package uk.co.octatec.scdds.cache;

import uk.co.octatec.scdds.queue.Event;
import uk.co.octatec.scdds.queue.EventQueueFactory;

/**
 * Created by Jeromy Drake on 07/05/16
 */

public interface ListenerEventQueueFactory<K,E extends Event<K>> extends EventQueueFactory<K,E> {

}
