package uk.co.octatec.scdds.odb;
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
import uk.co.octatec.scdds.net.serialize.Serializer;
import uk.co.octatec.scdds.net.serialize.SerializerFactory;
import uk.co.octatec.scdds.net.serialize.SerializerFactoryDefaultImpl;
import uk.co.octatec.scdds.net.serialize.SerializerUtils;

import java.io.*;
import java.util.Map;

/**
 * Created by Jeromy Drake on 01/05/2016.
 *
 * Simple Object Database, used mainly for testing the saving and restoring of cache values
 */
public class ObjectDataStore<K,T> {

    private final static Logger log = LoggerFactory.getLogger(ObjectDataStore.class);

    private final Serializer<K,T> serializer;
    private DataOutputStream out;
    private DataInputStream in;

    public static class Entry<K,T>  {

        private final K key;
        private final T value;
        private final long timeStamp;
        private final int userData;

        public K getKey() {
            return key;
        }

        public T getValue() {
            return value;
        }

        public long getTimeStamp() {
            return timeStamp;
        }

        public int getUserData() {
            return userData;
        }

        public Entry(K key, T value, long timeStamp, int userData) {
            this.key = key;
            this.value = value;
            this.timeStamp = timeStamp;
            this.userData = userData;
        }

        @Override
        public String toString() {
            return "Entry{" + key +":"+value+":"+timeStamp+":"+userData+"}";
        }
    }

    public ObjectDataStore() {
        this(new SerializerFactoryDefaultImpl<K,T>());
    }

    public ObjectDataStore(SerializerFactory<K,T> serializerFactory)  {
        serializer = serializerFactory.create();
    }

    public void openForWrite(String path) throws IOException {
        //log.info("ObjectDataStore.openForWrite [{}]", path);
        out = new DataOutputStream(new FileOutputStream(path));
    }
    public void openForRead(String path) throws IOException {
        //log.info("ObjectDataStore.openForWrite [{}]", path);
        in = new DataInputStream(new FileInputStream(path));
    }

    public boolean isOpenForRead() {
        return in != null;
    }

    public boolean isOpenForWrite() {
        return out != null;
    }

    public void close() {
        //log.info("ObjectDataStore.close");
        try {
            if( out != null ) {
                out.close();
                out = null;
            }
            if( in != null ) {
                in.close();
                in = null;
            }
        }
        catch(Exception e) {
            log.info("exception during ObjectDataStore close [{}]", e.getMessage());
        }
    }

    public void store(K key, T value) throws IOException{
        store(key, value, 0);
    }

    public void store(K key, T value, int userData) throws IOException{

        long timeStamp = System.currentTimeMillis();
        byte[] bTime = new byte[8];
        SerializerUtils.writeLongToBytes(timeStamp, bTime);
        out.write(bTime);

        int flags = 0; // for future use
        byte[] bFlags = new byte[4];
        SerializerUtils.writeIntToBytes(flags, bFlags);
        out.write(bFlags);

        byte[] bUsr = new byte[4];
        SerializerUtils.writeIntToBytes(userData, bUsr);
        out.write(bUsr);

        byte[] bData = serializer.serialize(key, value, 0);
        writeBlock(bData);

        out.flush();
    }

    public void markDeleted(K key) throws IOException{
        store(key, null);
    }

    public Entry<K,T> readEntry() throws IOException{

        byte[] bTime = new byte[8];
        int len = in.read(bTime);
        if( len == -1 || len == 0) {
            // normal EOF
            log.info("met normal EOF reading Object Data Store");
            return null;
        }
        if( len < bTime.length ) {
            log.error("short timestamp read, wanted [8] got [{}]", len);
            throw new IOException("short timestamp read");
        }
        long timeStamp = SerializerUtils.readLongFromBytes(bTime);

        byte[] bInt = new byte[4];
        len = in.read(bInt);
        if( len < bInt.length ) {
            log.error("short flags read, wanted [4] got [{}]", len);
            throw new IOException("short flags read");
        }
        int flags = SerializerUtils.readIntFromBytes(bInt);  // for future use

        len = in.read(bInt);
        if( len < bInt.length ) {
            log.error("short user-data read, wanted [4] got [{}]", len);
            throw new IOException("short user-data read");
        }
        int userData = SerializerUtils.readIntFromBytes(bInt);

        byte[] buff = readBlock();
        if( buff == null ) {
            return null;
        }
        Serializer.Pair<K,T> pair =  serializer.deserialize(buff, 0);

        return new Entry<>(pair.key, pair.value, timeStamp, userData);
    }

    private void writeBlock(byte[] buff) throws IOException{
        byte[] bLen  = new byte[4];
        SerializerUtils.writeIntToBytes(buff.length, bLen);
        out.write(bLen);
        out.write(buff);
    }

    private byte[] readBlock() throws IOException{
        byte[] len  = new byte[4];
        int m = in.read(len);
        if( m < 4 ) {
            log.error("short object-size read, wanted [4] got [{}]", m);
            throw new IOException("short object-size read");
        }
        int n = SerializerUtils.readIntFromBytes(len);
        byte[] buff = new byte[n];
        m = in.read(buff);
        if(  m < n ) {
            log.error("short object read, wanted [{}] got [{}]", m, n);
            throw new IOException("short object read");
        }
        return buff;
    }

}
