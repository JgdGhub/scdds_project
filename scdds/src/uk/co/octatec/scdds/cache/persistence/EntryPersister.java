package uk.co.octatec.scdds.cache.persistence;

/**
 * Created by Jeromy Drake on 02/05/2016.
 */
public interface EntryPersister<K,T> {
    void open();
    void store(K key, T value);
    void markDeleted(K key);
    void close();
}
