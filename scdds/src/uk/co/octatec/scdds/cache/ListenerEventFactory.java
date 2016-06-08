package uk.co.octatec.scdds.cache;

/**
 * Created by Jeromy Drake on 07/05/16
 */
public interface ListenerEventFactory<K,T> {
    ListenerEvent<K,T> create();
}
