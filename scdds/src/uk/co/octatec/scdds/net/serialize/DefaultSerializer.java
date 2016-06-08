package uk.co.octatec.scdds.net.serialize;
/*
  SC/DDS - simple cached data distribution service

  Copyright 2016 by Jeromy Drake

  This program is free software; you may redistribute and/or modify it under
  the terms of the GNU General Public License Version 2 as published by the
  Free Software Foundation.

  This program is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY, without even the implied warranty of MERCHANTABILITY
  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
  for complete details.
*/
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static uk.co.octatec.scdds.ConditionalCompilation._DBG;
import uk.co.octatec.scdds.queue.EventQueueDefaultImpl;

import java.io.*;
import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * Created by Jeromy Drake on 30/04/2016.
 */
public class DefaultSerializer<K, T> implements Serializer<K,T> {

    private final static Logger log = LoggerFactory.getLogger(EventQueueDefaultImpl.class);

    static final byte version = 0;

    @Override
    public byte[] serialize(K key, T value, int reserveHeaderSpace) {

        // format: [key-length][key-bytes][value-length][value-bytes]


        byte[] bkey = doSerialize(key);
        byte[] nkey = new byte[4];
        SerializerUtils.writeIntToBytes(bkey.length, nkey);
        log.debug("serialize key-len=[{}]", bkey.length);
        byte[] bvalue = doSerialize(value);
        byte[] nvalue = new byte[4];
        SerializerUtils.writeIntToBytes(bvalue.length, nvalue);
        log.debug("serialize value-len=[{}]", bvalue.length);

        byte[] b = new byte[reserveHeaderSpace+1+bkey.length+nkey.length+bvalue.length+nvalue.length];

        int firstPos = reserveHeaderSpace;
        b[firstPos]=version;

        System.arraycopy(nkey, 0,  b, firstPos+1,  nkey.length);
        log.debug("serialize arraycopy nkey=[{}] at pos=[1]", nkey) ;
        int pos = firstPos+1+nkey.length;
        System.arraycopy(bkey,  0, b, pos, bkey.length);
        pos += bkey.length;

        System.arraycopy(nvalue, 0, b, pos, nvalue.length);
        log.debug("serialize arraycopy nvalue=[{}] at pos=[{}]", nvalue,pos);
        pos += nvalue.length;
        System.arraycopy(bvalue, 0, b, pos, bvalue.length);

        log.debug("serialize bkey [{}]", bkey);
        log.debug("serialize nkey [{}]", nkey);
        log.debug("serialize bvalue [{}]", bvalue);
        log.debug("serialize nvalue [{}]", nvalue);

        log.debug("serialize b [{}]", b);

        return b;
    }

    @Override
    public Pair<K,T> deserialize(byte[] buff, int offset) {
        final int posKeyLen = offset+1;
        //byte serializationVersion = buff[0]; // could use this to perform alternative de-serialization
                                             // but currently there is only one version of DefaultSerialization
                                             // if you implement your oewn serialization, its probably a good idea to
                                             // to include some kind of version/flag so you can modify your algorithm
                                             // and detect which one is being used at run-time
        int nkey = SerializerUtils.readIntFromBytes(buff, posKeyLen);
        if( _DBG ) {
            log.debug("de-serialize key-len=[{}] from posKeyLen=[{}]", nkey, posKeyLen);
        }
        final int posValueLen = posKeyLen + nkey + 4;
        int nvalue = SerializerUtils.readIntFromBytes(buff, posValueLen);
        if( _DBG ) {
            log.debug("de-serialize value-len=[{}] from posKeyLen=[{}]", nvalue, posValueLen);
            log.debug("de-serialize buff [{}]", buff);
        }
        Pair<K,T> pair = new Pair<>();
        pair.key = (K)doDeserialize(buff, offset+4+1, nkey) ;
        pair.value =(T)doDeserialize(buff,offset+4+nkey+4+1, nvalue);
        return pair;
    }

    byte[] doSerialize(Object serializableObject) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(serializableObject);
            byte[] buff = bos.toByteArray();
            oos.close();
            return buff;
        }
        catch (Exception x) {
            log.error("SERIALIZATION ERROR from DefaultSerializer, serializableObject=[{}] ", serializableObject, x);
            return null;
        }
    }

    Object doDeserialize(byte[] buff, int offset, int length) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(buff, offset, length);
            ObjectInputStream ois = new ObjectInputStream(bis);
            Object serializableObject = ois.readObject();
            ois.close();
            return (T) serializableObject;
        }
        catch (InvalidClassException x) {
            log.error("DESERIALIZATION ERROR from DefaultSerializer class=[{}], object=[{}]", x.classname, x);
            throw new RuntimeException("DESERIALIZATION ERROR InvalidClass - class version different "+x.classname, x);
        }
        catch (Exception x) {
            log.error("DESERIALIZATION ERROR from DefaultSerializer "+x.getMessage(), x);
            throw new RuntimeException("Deserialization Error: "+x.getMessage(), x);
        }
    }
}
