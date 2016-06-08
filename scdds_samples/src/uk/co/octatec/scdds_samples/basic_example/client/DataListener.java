package uk.co.octatec.scdds_samples.basic_example.client;
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
import uk.co.octatec.scdds.cache.CacheListener;
import uk.co.octatec.scdds.cache.subscribe.ImmutableCache;
import uk.co.octatec.scdds_samples.basic_example.Data;

/**
 * Created by Jeromy Drake on 06/06/2016.
 */
public class DataListener implements CacheListener<String, Data>{

    private final static Logger log = LoggerFactory.getLogger(DataListener.class);

    private double startTime;
    private long loopStartTime;
    private double updateCount;
    private long prevTiemstamp;



    @Override
    public void onInitialUpdate(ImmutableCache<String, Data> immutableCache) {
        log.info("onInitialUpdate size={}", immutableCache.size());
        startTime = (double)System.currentTimeMillis();
    }

    @Override
    public void onUpdate(String s, Data data) {

        long now = System.currentTimeMillis();
        long timestamp = data.getMilliseconds();
        long latency = now - timestamp;

        if( prevTiemstamp == 0 ) {
            prevTiemstamp = data.getMilliseconds();
        }
        else if( timestamp < prevTiemstamp ) {
            // this can't happen
            log.error("\r\n\r\n\r\n********** OUT OF ORDER MESSAGE *********************\r\n\r\n\r\n") ;
            System.exit(1);
        }
        prevTiemstamp = timestamp;
        log.info("onUpdate [{}] latency={} ms", data, latency);
    }

    @Override
    public void onRemoved(String s, Data data) {
        log.info("onRemoved [{}]", data);
    }

    @Override
    public void onDataStale() {
        log.info("\r\n*** onDataStale ***\r\n");
    }

    @Override
    public void onActive() {
        log.info("onActive");
    }

    @Override
    public void onFatalError(String s) {
        log.info("onFatalError [{]]", s);
    }
}
