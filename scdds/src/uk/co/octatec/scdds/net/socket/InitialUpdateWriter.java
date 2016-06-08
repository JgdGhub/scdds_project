package uk.co.octatec.scdds.net.socket;
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
import uk.co.octatec.scdds.net.serialize.SerializerUtils;

/**
 * Created by Jeromy Drake on 05/05/16
 */
public class InitialUpdateWriter {

    private final Logger log = LoggerFactory.getLogger(InitialUpdateWriter.class);

    // data looks like this
    // [ MMMM H KKKK { NNNN D <data>  } repeated KKKK times ]  MMMM = total length of message in bytes

    private static final int INITAIL_BUFFER_SIZE = 2048;

    private byte[] buff = new byte[2048];
    private int pos;
    private int initialUpdateMessageCount;

    public int getStartingDataPos() {  // mostly for use in tests
        return  BlockIoImpl.HEADER_LENGTH + SerializerUtils.SIZE_OF_INT; // (space for the number of items )
    }

    public void prepareInitialUpdateMessage() {
        pos = getStartingDataPos(); // HEADER + 4 bytes = number of items
        buff[BlockIoImpl.HEADER_FLAG_POS] = BlockIO.INITIAL_UPDATE_FLAG;
        initialUpdateMessageCount = 0;
    }

    public void addInitialUpdateEntry(byte[] serializedObject) {
        if( pos + BlockIoImpl.HEADER_LENGTH +serializedObject.length > buff.length ) {
            realloc();
        }
        if( _DBG ) {
            log.debug("copy header to [{}] serializedObject.length=[{}]", pos, serializedObject.length);
        }
        BlockIoImpl.copyHeaderToBytes(buff, pos, BlockIO.DATA_FLAG, serializedObject.length);
        pos += BlockIoImpl.HEADER_LENGTH;
        if( _DBG ) {
            log.debug("copy data to [{}]", pos);
        }
        System.arraycopy(serializedObject, 0, buff, pos, serializedObject.length);
        pos +=  serializedObject.length;
        ++initialUpdateMessageCount;
        if( _DBG ) {
            log.debug("after copy, pos=[{}], initialUpdateMessageCount=[{}]", pos, initialUpdateMessageCount);
        }
    }

    public byte[] getInitialUpdateMessage() {
        SerializerUtils.writeIntToBytes(pos-BlockIoImpl.HEADER_LENGTH, buff);
        // write the number of items immediately after the header
        SerializerUtils.writeIntToBytes(initialUpdateMessageCount, buff, BlockIoImpl.HEADER_LENGTH);
        return buff;
    }

    public int getInitialUpdateMessageLength() {
        return pos;
    }

    public void completeInitialUpdateMessage() {
        buff[BlockIoImpl.HEADER_FLAG_POS] = BlockIO.INITIAL_UPDATE_COMPLETED_FLAG;

    }
    private void realloc() {
        byte[] tmp = new byte[buff.length+INITAIL_BUFFER_SIZE];
        System.arraycopy(buff, 0, tmp, 0, pos);
        buff = tmp;
    }

    byte[] getBuff() {
        return buff;
    }
}
