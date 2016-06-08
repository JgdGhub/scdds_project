package uk.co.octatec.scdds_samples.custom_serializer;
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
import uk.co.octatec.scdds.net.serialize.Serializer;
import uk.co.octatec.scdds.net.serialize.SerializerUtils;
import uk.co.octatec.scdds_samples.basic_example.Data;

/**
 * Created by Jeromy Drake on 07/06/2016.
 *
 * Custom  serializers are not required, but they will perform much better than the default.
 * The Custom serializer and factory must be available on the class-path of both the server and client
 */
public class DataSerializer implements Serializer<String,Data> {

    private static final int VERSION = 1;

    @Override
    public byte[] serialize(String key, Data data, int reserveHeaderSpace) {

        // the order of serialization is completer arbitrary, just
        // do it in the same order in the deserialize method

        int sizeOfVersionFlag = SerializerUtils.SIZE_OF_INT; // a version flag might be useful if the implementation changes

        // in many situations the key might be also part of the Data, in which case it wouldn't need to be separately serialized;

        int sizeOfKey = SerializerUtils.SIZE_OF_STRING(key); // could chose SHORT_STRING or LONG_STRING depending on the length of strings,
                                                             // using STRING allows 65536 characters

        int sizeOfId = SerializerUtils.SIZE_OF_INT;
        int sizeOfMilliseconds =  SerializerUtils.SIZE_OF_LONG;
        int sizeOfValue =  SerializerUtils.SIZE_OF_DOUBLE;
        int sizeOfHostname =  SerializerUtils.SIZE_OF_SHORT_STRING(data.getInformation()); // hostname are not likely to be more than 128 chars

        int buffSize = reserveHeaderSpace + sizeOfVersionFlag + sizeOfKey + sizeOfId +  sizeOfMilliseconds  +sizeOfValue + sizeOfHostname;

        byte[] buff = new byte[buffSize];

        int pos = reserveHeaderSpace;

        SerializerUtils.writeIntToBytes(VERSION, buff, pos);
        pos += sizeOfVersionFlag;

        SerializerUtils.writeStringToBytes(key, buff, pos);
        pos += sizeOfKey;

        SerializerUtils.writeIntToBytes(data.getId(), buff, pos);
        pos += sizeOfId;
        SerializerUtils.writeLongToBytes(data.getMilliseconds(), buff, pos);
        pos += sizeOfMilliseconds;
        SerializerUtils.writeDoubleToBytes(data.getValue(), buff, pos);
        pos += sizeOfValue;
        SerializerUtils.writeShortStringToBytes(data.getInformation(), buff, pos);

        return buff;
    }

    @Override
    public Pair<String, Data> deserialize(byte[] bytes, int offset) {

        int versionFlag =  SerializerUtils.readIntFromBytes(bytes, offset);
        // we could make a decision based on the versionFlag as to how to proceed

        offset +=  SerializerUtils.SIZE_OF_INT;
        String key = SerializerUtils.readStringFromBytes(bytes, offset);
        offset +=  SerializerUtils.SIZE_OF_STRING(key);
        int id =  SerializerUtils.readIntFromBytes(bytes, offset);
        offset +=  SerializerUtils.SIZE_OF_INT;
        long ms = SerializerUtils.readLongFromBytes(bytes, offset);
        offset +=  SerializerUtils.SIZE_OF_LONG;
        double value = SerializerUtils.readDoubleFromBytes(bytes, offset);
        offset += SerializerUtils.SIZE_OF_DOUBLE;
        String hostName =  SerializerUtils.readShortStringFromBytes(bytes, offset);

        Pair<String, Data> pair = new Pair<>();
        pair.key = key;
        pair.value = new Data(id, ms, value, hostName);

        return pair;
    }
}
