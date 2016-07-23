package uk.co.octatec.scdds.net.router;
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
import uk.co.octatec.scdds.cache.Cache;
import uk.co.octatec.scdds.cache.CacheListener;
import uk.co.octatec.scdds.cache.ImmutableEntry;
import uk.co.octatec.scdds.cache.subscribe.ImmutableCache;

/**
 * Created by Jeromy Drake on 13/07/2016.
 */
public class RepublishListener implements CacheListener {

    private final static Logger log = LoggerFactory.getLogger(RepublishListener.class);

    private Cache cache;
    private String cacheName;

    public RepublishListener(Cache cache, String cacheName) {
        this.cache = cache;
        this.cacheName = cacheName;
    }

    @Override
    public void onInitialUpdate(ImmutableCache initialState) {
        // this callback always has an empty initialState in a Streaming Subscription
        log.info("updates flowing from source cache [{}]", cacheName);
    }

    @Override
    public void onUpdate(Object key, ImmutableEntry value) {
        cache.put(key, value);
    }

    @Override
    public void onRemoved(Object key, ImmutableEntry value) {
        cache.remove(key);
    }

    @Override
    public void onDataStale() {
        log.info("detected stale source cache [{}]", cacheName);
        cache.notifyStale();
    }

    @Override
    public void onActive() {
        log.info("detected active source cache [{}]", cacheName);
        cache.notifyUnStale();
    }

    @Override
    public void onFatalError(String errorText) {
        log.error("fatal error from source cache [{}] [{}]", cacheName, errorText);
        cache.notifyStale();
    }
}
