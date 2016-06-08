package uk.co.octatec.scdds.cache.persistence;

/**
 * Created by Jeromy Drake on 02/05/2016.
 */
public final class NoOpEntryPersister<K,T> implements  EntryPersister<K,T>{

    @Override
    public void open() {
    }

    @Override
    public void store(K key, T value) {
    }

    @Override
    public void markDeleted(K key) {

    }

    @Override
    public void close() {
    }
}
