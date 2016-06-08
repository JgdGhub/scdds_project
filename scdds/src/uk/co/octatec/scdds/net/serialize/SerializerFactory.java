package uk.co.octatec.scdds.net.serialize;

/**
 * Created by Jeromy Drake on 30/04/2016.
 */
public interface SerializerFactory<K,T>{
    Serializer<K,T> create();
 }
