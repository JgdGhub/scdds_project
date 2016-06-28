package uk.co.octatec.scdds.cache;
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
import junit.framework.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.cache.persistence.*;
import uk.co.octatec.scdds.utilities.SimpleData;

import java.io.File;

/**
 * Created by Jeromy Drake on 20/06/2016.
 */

public class KeyBasedCacheLoaderPersisterTest {

    private final static Logger log = LoggerFactory.getLogger(PublishingCacheBuilderTest.class);


    @Test
    public void factoryTest() {
        log.info("### factoryTest");
        KeyBasedCacheLoaderPersisterFactory<String,SimpleData> factory = new KeyBasedCacheLoaderPersisterFactory<>();
        CacheLoader<String,SimpleData> cacheLoader = factory.createCacheLoader("test/Test_Cache");
        Assert.assertNotNull("cacheLoader not null", cacheLoader);
        EntryPersister<String,SimpleData> entryPersister = factory.createEntryPersister("test/Test_Cache");
        Assert.assertNotNull("entryPersister not null", entryPersister);
    }

    @Test
    public void runKeyBasedCacheLoaderPersisterTests() {

        // run 3 'sub' tests, each subsequent tests uses the output from the
        // previous test...

        persisterSubTest();

        cacheLoaderSubTest();

        cachePersisterDeleteSubTest();
    }

    private void persisterSubTest() {

        log.info("### persisterSubTest");
        log.info("cwd=[{}]", System.getProperty("user.dir"));

        KeyBasedEntryPersister<String,SimpleData> entryPersister =  new KeyBasedEntryPersister<>("test/Test_Cache", "YYYYMMDD");
        File dir = new File(entryPersister.getDataDir());
        log.info("check data dir[{}]", entryPersister.getDataDir());
        if( dir.exists() && !dir.isDirectory()) {
            log.error("DIR-NAME EXSISTS [{}] BUT IS NOT A DIRECTORY", entryPersister.getDataDir());
            Assert.assertTrue("bad data dir - dir-name exists but is not a directory", false);
        }

        entryPersister.open();
        Assert.assertTrue("data-dir created", dir.exists() && dir.isDirectory());

        File f1 = new File(entryPersister.getDataDir()+"/K1"+KeyBasedEntryPersister.FILE_SUFFIX);
        File f2 = new File(entryPersister.getDataDir()+"/K2"+KeyBasedEntryPersister.FILE_SUFFIX);
        File f3 = new File(entryPersister.getDataDir()+"/K3"+KeyBasedEntryPersister.FILE_SUFFIX);
        if( f1.exists() ) {
            log.info("delete file for K1");
            f1.delete();
        }
        if( f2.exists() ) {
            log.info("delete file for K2");
            f2.delete();
        }
        if( f3.exists() ) {
            log.info("delete file for K3");
            f3.delete();
        }

        log.info("store K1...");
        entryPersister.store("K1", new SimpleData("K1", 1));
        log.info("store K2...");
        entryPersister.store("K2", new SimpleData("K2", 2));
        log.info("store K3...");
        entryPersister.store("K3", new SimpleData("K3", 3));

        Assert.assertTrue("K1 file exists", f1.exists() );
        Assert.assertTrue("K2 file exists", f2.exists() );
        Assert.assertTrue("K3 file exists", f3.exists() );

        entryPersister.close();
    }


   private void cacheLoaderSubTest() {
        log.info("### cacheLoaderSubTest");
        log.info("cwd=[{}]", System.getProperty("user.dir"));

        KeyBasedCacheLoader<String,SimpleData> cacheLoader =  new KeyBasedCacheLoader<String,SimpleData>("test/Test_Cache", "YYYYMMDD");
        File dir = new File(cacheLoader.getDataDir());
        log.info("check data dir[{}]", cacheLoader.getDataDir());
        if( dir.exists() && !dir.isDirectory()) {
            log.error("DIR-NAME EXSISTS [{}] BUT IS NOT A DIRECTPRY", cacheLoader.getDataDir());
            Assert.assertTrue("bad data dir - dir-name exists but is not a directory", false);
        }
        if( !dir.exists()  ) {
            log.warn("dependent test not yet run, this test will not run");
            return;
        }

        File f1 = new File(cacheLoader.getDataDir()+"/K1"+KeyBasedEntryPersister.FILE_SUFFIX);
        File f2 = new File(cacheLoader.getDataDir()+"/K2"+KeyBasedEntryPersister.FILE_SUFFIX);
        File f3 = new File(cacheLoader.getDataDir()+"/K3"+KeyBasedEntryPersister.FILE_SUFFIX);
        if( !f1.exists() ) {
            log.warn("file for K1 does not exist, it should have been created by a previous test - this will cause the test to fail");
        }
        if( f2.exists() ) {
            log.warn("file for K2 does not exist, it should have been created by a previous test - this will cause the test to fail");
        }
        if( f3.exists() ) {
            log.warn("file for K3 does not exist, it should have been created by a previous test - this will cause the test to fail");
        }

        cacheLoader.open();

        log.info("load cache..."); // the cache entries K1,K2,K3 were saved in the previous test

        Cache<String,SimpleData> cache = CacheImpl.createLocalCache("Test_Cache");
        cacheLoader.loadCache(cache);

        log.info("cache loaded, keys {}", cache.keySet());

        Assert.assertTrue("cache is correct size", cache.size()==3);

        SimpleData k1 = cache.get("K1");
        SimpleData k2 = cache.get("K1");
        SimpleData k3 = cache.get("K1");

        Assert.assertNotNull("cache has K1", k1);
        Assert.assertNotNull("cache has K2", k2);
        Assert.assertNotNull("cache has K3", k3);

        cacheLoader.close();
    }

    private void cachePersisterDeleteSubTest() {
        log.info("### cachePersisterDeleteSubTest");
        log.info("cwd=[{}]", System.getProperty("user.dir"));

        KeyBasedEntryPersister<String,SimpleData> entryPersister =  new KeyBasedEntryPersister<>("test/Test_Cache", "YYYYMMDD");
        log.info("check data dir[{}]", entryPersister.getDataDir());
        File dir = new File(entryPersister.getDataDir());
        if( dir.exists() && !dir.isDirectory()) {
            log.error("DIR-NAME EXSISTS [{}] BUT IS NOT A DIRECTORY", entryPersister.getDataDir());
            Assert.assertTrue("bad data dir - dir-name exists but is not a directory", false);
        }

        entryPersister.open();
        Assert.assertTrue("data-dir created", dir.exists() && dir.isDirectory());

        File f1 = new File(entryPersister.getDataDir()+"/K1"+KeyBasedEntryPersister.FILE_SUFFIX);
        File f2 = new File(entryPersister.getDataDir()+"/K2"+KeyBasedEntryPersister.FILE_SUFFIX);
        File f3 = new File(entryPersister.getDataDir()+"/K3"+KeyBasedEntryPersister.FILE_SUFFIX);
        if( !f1.exists() ) {
            log.info("persistent test not yet run, this will cause the test to fail");
        }

        entryPersister.markDeleted("K1");

        Assert.assertFalse("K1 does not exist", f1.exists() );


        entryPersister.close();

    }

}
