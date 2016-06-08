package uk.co.octatec.scdds.net.serialize;

import java.io.Serializable;

/**
 * Created by Jeromy Drake on 30/04/2016.
 *
 * A default serialization implementation using java-serialization, you will get
 * better performance if you write serilizers specific to your classes
 */
public class SerializerFactoryDefaultImpl<K,T> implements SerializerFactory<K,T>{
    @Override
    public Serializer<K,T> create() {
        return new DefaultSerializer<K,T>() ;
    }
}
