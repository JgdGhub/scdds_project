package uk.co.octatec.scdds.queue;

/**
 * Created by Jeromy Drake on 30/04/2016.
 */
public interface Event<K> {
    K getKey();
    boolean canBeBatched();
}
