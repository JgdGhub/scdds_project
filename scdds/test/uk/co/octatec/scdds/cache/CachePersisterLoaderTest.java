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
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.cache.persistence.*;
import uk.co.octatec.scdds.utilities.DirCheck;
import uk.co.octatec.scdds.utilities.SimpleData;
import uk.co.octatec.scdds.odb.ObjectDataStoreTest;

import java.io.File;

/**
 * Created by Jeromy Drake on 07/05/16
 */
public class CachePersisterLoaderTest {

    private final Logger log = LoggerFactory.getLogger(ObjectDataStoreTest.class);

    static final String PATH_NAME_TMP_D = "test/MyTestCacheD_tmp";
    static final String PATH_NAME_D = "test/MyTestCacheD";

    static final String PATH_NAME_TMP = "test/MyTestCache_tmp";
    static final String PATH_NAME = "test/MyTestCache";

    static final String SUFFIX = "YYYYMMDD";
    static final int CACHE_ELEMENT_COUNT = 10;
    static final String KEY_PREFIX = "K:";
    static final int ENTRY_TO_DELETE = 5;

    @BeforeClass
    public static void setup() {

        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO");
        DirCheck.check("test");
    }

    @Test
    public void persisterTest() {

        log.info("##persisterTest");
        doPersisterTest(false/* no delete*/, PATH_NAME_TMP, PATH_NAME);

    }

    @Test
    public void factoryTest() {

        log.info("##factoryTest");

        CacheLoaderPersisterFactory<String,SimpleData> factory =  new ObjectStoreCacheLoaderPersisterFactory<String,SimpleData>();
        CacheLoader<String,SimpleData> loader =   factory.createCacheLoader("TestCacheX");
        Assert.assertNotNull(" CacheLoader created", loader);
        EntryPersister<String,SimpleData> persister = factory.createEntryPersister("TestCacheX");
        Assert.assertNotNull("EntryPersister created", persister);

        String fname1 = ((ObjectDataStoreCacheLoader<String, SimpleData>) loader).getFileName();
        String fname2 = ((ObjectDataStoreEntryPersister<String, SimpleData>) persister).getFileName();
        log.info("file-names et [{}] [{}]", fname1, fname2);
        Assert.assertNotNull(" CacheLoader filename set", fname1);
        Assert.assertNotNull("EntryPersister filename set", fname2);

        Assert.assertEquals("filenamaes created equal", fname1, fname2);
    }


    @Test
    public void persisterTestWithDeletedEntry() {

        log.info("##persisterTestWithDeletedEntry");
        doPersisterTest(true/*delete an wntry*/, PATH_NAME_TMP_D, PATH_NAME_D);

    }
    private void doPersisterTest(boolean deleteAnEntry, String pathNameTmp, String pathName) {

        log.info("doPersisterTest deleteAnEntry={} pathName=[{}]", deleteAnEntry, pathName);

        // test we can write data to the data-store

        CacheImpl<String,SimpleData> cache = new   CacheImpl<String,SimpleData>();
        ObjectDataStoreEntryPersister<String,SimpleData> persister = new ObjectDataStoreEntryPersister<>(pathNameTmp, SUFFIX);
        File fTmp = new File(persister.getFileName());
        if( fTmp.exists() ) {
            fTmp.delete();
        }
        persister.open();
        cache.setEntryPersister(persister);

        for(int i=1; i<=CACHE_ELEMENT_COUNT; i++) {
            // put a number of elements in the cache to be written out
            String key = KEY_PREFIX+i;
            SimpleData data = new SimpleData(key, i*100);
            cache.put(key, data);
            if( deleteAnEntry && i== 8 ) {
                // put the deleted entry an position 9 in the file, we actually delete entry ENTRY_TO_DELETE
                key = KEY_PREFIX+ENTRY_TO_DELETE;
                log.info("remove entry key=[{}]", key);
                cache.remove(key);
            }
        }

        log.info("file: [{}] exists?{} length={}", persister.getFileName(), fTmp.exists(), fTmp.length());

        Assert.assertTrue("file exists", fTmp.exists());
        Assert.assertTrue("file has data", fTmp.length()>0);

        cache.dispose();

        File fSaved = new   File(pathName+"_"+ SUFFIX+".dat");
        if( !fSaved.exists() ) {
            fTmp.renameTo(fSaved);
        }
        else {
            fTmp.delete();
        }

    }

    @Test
    public void loaderTest() {
        log.info("## loaderTest");
        doLoaderTest(false/*no deleted entry*/, PATH_NAME);
    }

    @Test
    public void loaderTestWithDeletedEntry() {
        log.info("## loaderTest");
        doLoaderTest(true/*expect a deleted entry*/, PATH_NAME_D);
    }


    public void doLoaderTest(boolean expectEntryDeleted, String pathName) {

        log.info("doLoaderTest expectEntryDeleted={} pathName=[{}]", expectEntryDeleted, pathName);

        // test we can load data from the data-store

        // relies on the file created in the previous test

        String path =  pathName+"_"+ SUFFIX+".dat";
        File f = new File(path);
        if( !f.exists() ) {
            log.info("data file [{}] does not exists, 'loaderTest' ignore until the file gets created by 'persisterTest' ",path);
            return;
        }
        ObjectDataStoreCacheLoader<String,SimpleData> loader  = new  ObjectDataStoreCacheLoader<>(pathName, SUFFIX);
        CacheImpl<String,SimpleData> cache = new   CacheImpl<String,SimpleData>();
        loader.open();
        loader.loadCache(cache);
        loader.close();

        int expectedCacheSize = expectEntryDeleted?CACHE_ELEMENT_COUNT-1:CACHE_ELEMENT_COUNT ;
        log.info("cache loaded size={} expectedCacheSize={} keys={}", cache.size(), expectedCacheSize, cache.keySet());


        Assert.assertEquals("cache size correct", expectedCacheSize, cache.size() );
        Assert.assertTrue("cache has correct element count", f.exists());
        for(int i=1; i<=CACHE_ELEMENT_COUNT; i++) {
            String key = KEY_PREFIX+i;
            SimpleData data = cache.get(key);
            if( expectEntryDeleted && i == ENTRY_TO_DELETE )  {
                Assert.assertNull("cache shouild not have entry "+key, data);
            }
            else {
                Assert.assertNotNull("cache has entry "+key, data);
            }
        }
    }

}
