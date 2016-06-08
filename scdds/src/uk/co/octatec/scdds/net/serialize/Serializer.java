package uk.co.octatec.scdds.net.serialize;

/**
 * Created by Jeromy Drake on 30/04/2016.
 */
public interface Serializer<K,T> {
    class Pair<K,T> {
        public K key;
        public T value;
    }

    /**
     * Serialize an object into an array of bytes
     * @param key
     * @param value
     * @param reserveHeaderSpace - extra space added to the start of the buffer, must not used by the serializer
     * @return
     */
    byte[] serialize(K key, T value, int reserveHeaderSpace);

    /**
     * Deserialize an object from a byte array
     * The buffer presented alwyas has any 'reserveHeaderSpace' removed, i.e. the object-data will always start at 'offset'
     * @param buff
     * @param offset
     * @return
     */
    Pair<K,T> deserialize(byte[] buff, int offset);
}
