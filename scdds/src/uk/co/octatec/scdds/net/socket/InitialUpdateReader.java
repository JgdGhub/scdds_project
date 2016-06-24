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
import uk.co.octatec.scdds.net.serialize.Serializer;
import uk.co.octatec.scdds.net.serialize.SerializerUtils;

import java.io.IOException;
import java.util.Map;

/**
 * Created by Jeromy Drake on 05/05/16
 *
 * This class is used internally on the client side to read the initial update sent by a server
 */
public class InitialUpdateReader<K,T> {

    private final Logger log = LoggerFactory.getLogger(InitialUpdateReader.class);

    private final BlockIO bIO;
    private final Serializer<K,T> serializer;

    public InitialUpdateReader(BlockIO bIO, Serializer<K,T> serializer) {
        this.bIO = bIO;
        this.serializer = serializer;
    }

    public void readInitialUpdate(Map<K,T> data) throws IOException {

        // we have already read the header, we now need to read the data

        if( _DBG ) {
            log.debug("readRestOfBlock len={}", bIO.getCurrentHeader().dataLength);
        }
        byte[] buff = bIO.readRestOfBlock();
        if( _DBG ) {
            log.debug("readRestOfBlock done");
        }
        doRreadInitialUpdate(data, buff);
    }

    public void doRreadInitialUpdate(Map<K,T> data, byte[] buff) throws IOException {

        // first 4 bytes are the number of records
        int numOfRecords = SerializerUtils.readIntFromBytes(buff);

        log.info("read initial update bytes [{}] numOfRecords=[{}]", buff.length, numOfRecords);

        int pos = BlockIoImpl.OFFSET_PAST_BLOCK_SIZE; // read past 1st 4 bytes that is the number of reocrds
        for (int i = 0; i < numOfRecords; i++) {
            int dataSize = SerializerUtils.readIntFromBytes(buff, pos);
            if( _DBG ) {
                log .debug("...read[{}] data-size=[{}]", i, dataSize);
            }
            pos += BlockIoImpl.HEADER_LENGTH-1;
            byte flag = buff[pos];
            ++pos;
            if( _DBG ) {
                log.debug("...read[{}] de-serialize from pos=[{}]", i, pos);
            }
            Serializer.Pair<K, T> pair = serializer.deserialize(buff, pos);
            applyUpdate(pair.key, pair.value, data);
            pos += dataSize;
            if( _DBG ) {
                log.debug("read object key=[{}], next pos = [{}]", pair.key, pos);
            }

        }
    }

    protected void applyUpdate(K key, T value, Map<K,T> data) {
        data.put(key, value);
    }
}
