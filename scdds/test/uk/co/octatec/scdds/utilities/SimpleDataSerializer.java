package uk.co.octatec.scdds.utilities;
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

/**
 * Created by Jeromy Drake on 15/05/2016.
 */
public class SimpleDataSerializer implements Serializer<String, SimpleData> {

    // example of how a specialized serializer could be written, this specialized
    // serializer is almost 100X faster than the default serializer, see "SimpleDataSerializerTest()"

    static final byte versionNumber = 1;

    @Override
    public byte[] serialize(String key, SimpleData value, int reserveHeaderSpace) {

        // the length of thew version flag
        int nversion =1 ;
        // the length of the key
        int nkey = SerializerUtils.SIZE_OF_STRING(key);
        // length of the value=-null flag
        int nflag = 1;

        // lengths of the the actual value  fields
        int ndata1;
        int ndata2;
        if( value == null ) {
            ndata1 =  ndata2 = 0;
        }
        else {
            ndata1 = SerializerUtils.SIZE_OF_STRING(value.data1);
            ndata2 = SerializerUtils.SIZE_OF_INT;
        }

        int sizeOfBuffer=  reserveHeaderSpace + nversion + nkey + nflag + ndata1 + ndata2;

        byte[] buff = new byte[sizeOfBuffer];

        buff[reserveHeaderSpace] = versionNumber;

        SerializerUtils.writeStringToBytes(key, buff, reserveHeaderSpace+nversion); // serialize the key

        if( value == null) {
            buff[reserveHeaderSpace+1+nkey] = 1;  // flag to indicate a null value object
        }
        else {
            buff[reserveHeaderSpace+1+nkey] = 0;
            SerializerUtils.writeStringToBytes(value.data1, buff, reserveHeaderSpace + 1 + nkey + nflag);
            SerializerUtils.writeIntToBytes(value.data2, buff, reserveHeaderSpace + 1 + nkey + nflag + ndata1);
        }

        return buff;
    }

    @Override
    public Pair<String, SimpleData> deserialize(byte[] buff, int offset) {
        Pair<String, SimpleData> pair = new Pair<>();
        byte version = buff[offset];
        int nversion =1 ;
        pair.key = SerializerUtils.readStringFromBytes(buff, offset+nversion);
        int nkey = SerializerUtils.SIZE_OF_STRING(pair.key);
        byte nullFlag =  buff[offset+nversion+nkey];
        int nflag = 1;
        if( nullFlag == 0 ) {
            String data1 = SerializerUtils.readStringFromBytes(buff, offset+nversion+nkey+nflag);
            int ndata1 = SerializerUtils.SIZE_OF_STRING(data1);
            int data2 = SerializerUtils.readIntFromBytes(buff, offset+nversion+nkey+nflag+ndata1);
            pair.value = new SimpleData(data1, data2);
        }
        return pair;
    }
}
