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
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.utilities.DirCheck;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * Created by Jeromy Drake on 01/05/2016.
 */
public class ObjectDataStoreTest {
    private final Logger log = LoggerFactory.getLogger(ObjectDataStoreTest.class);

    @BeforeClass
    public static void setup() {

        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");
        DirCheck.check("test");
    }

    static class Value implements Serializable {
        static long serialVersionUID = 1L;
        String name;
        int i;
        Date date;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Value value = (Value) o;

            if (i != value.i) return false;
            if (name != null ? !name.equals(value.name) : value.name != null) return false;
            return date != null ? date.equals(value.date) : value.date == null;

        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + i;
            result = 31 * result + (date != null ? date.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Value{" +
                    ", name='" + name + '\'' +
                    ", i=" + i +
                    ", date=" + date +
                    '}';
        }
    }

    @Test
    public void readWriteDeleteTest() throws IOException{

        log.info("## readWriteTest");
        ObjectDataStore<String, Value> objectDataStore = new ObjectDataStore<>();

        log.info("open odb_test.dat for write");

        objectDataStore.openForWrite("test/odb_test.dat");

        long timeCheck = System.currentTimeMillis();

        String key1 = "A1";
        Value value1 = new Value();
        value1.name = "NAME-A1";
        value1.i = 100;
        value1.date = new Date();
        log.info("store [{}] [{}]", key1, value1);
        objectDataStore.store(key1, value1);

        String key2 = "A2";
        Value value2 = new Value();
        value2.name = "NAME-A2";
        value2.i = 99999;
        value2.date = null;
        log.info("store [{}] [{}]", key2, value2);
        objectDataStore.store(key2, value2, 42);

        String key3 = "A3";
        Value value3 = new Value();
        value3.name = "NAME-A3";
        value3.i = 77;
        value3.date = null;
        log.info("store [{}] [{}]", key3, value3);
        objectDataStore.store(key3, value3);

        objectDataStore.markDeleted(key2);
        objectDataStore.close();

        log.info("open odb_test.dat for read");
        objectDataStore.openForRead("test/odb_test.dat");

        ObjectDataStore.Entry<String,Value> entry1 = objectDataStore.readEntry();
        log.info("entry1 [{}]", entry1);

        Assert.assertNotNull("entry is not null", entry1);
        String odbKey = entry1.getKey();
        Value odbValue = entry1.getValue();
        Assert.assertNotNull("entry1.key is not null", odbKey);
        Assert.assertNotNull("entry1.value is not null", odbValue);
        Assert.assertEquals("entry1.key is correct", odbKey, key1);
        Assert.assertEquals("entry1.value is correct", odbValue, value1);
        Assert.assertNotNull("entry1.timeStamp is correct", entry1.getTimeStamp()>timeCheck && entry1.getTimeStamp()<timeCheck+2000);
        Assert.assertEquals("entry1.userData is correct", entry1.getUserData(), 0);

        ObjectDataStore.Entry<String,Value> entry2 = objectDataStore.readEntry();
        log.info("entry2 [{}]", entry2);

        Assert.assertNotNull("entry is not null", entry1);
        odbKey = entry2.getKey();
        odbValue = entry2.getValue();
        Assert.assertNotNull("entry2.key is not null", odbKey);
        Assert.assertNotNull("entry2.value is not null", odbValue);
        Assert.assertEquals("entry2.key is correct", odbKey, key2);
        Assert.assertEquals("entry2.value is correct", odbValue, value2);
        Assert.assertNotNull("entry2.timeStamp is correct", entry2.getTimeStamp()>timeCheck && entry2.getTimeStamp()<timeCheck+2000);
        Assert.assertEquals("entry2.userData is correct", entry2.getUserData(), 42);

        ObjectDataStore.Entry<String,Value> entry3 = objectDataStore.readEntry();
        log.info("entry3 [{}]", entry3);

        Assert.assertNotNull("entry is not null", entry1);
        odbKey = entry3.getKey();
        odbValue = entry3.getValue();
        Assert.assertNotNull("entry3.key is not null", odbKey);
        Assert.assertNotNull("entry3.value is not null", odbValue);
        Assert.assertEquals("entry3.key is correct", odbKey, key3);
        Assert.assertEquals("entry3.value is correct", odbValue, value3);
        Assert.assertNotNull("entry3.timeStamp is correct", entry3.getTimeStamp()>timeCheck && entry3.getTimeStamp()<timeCheck+2000);
        Assert.assertEquals("entry3.userData is correct", entry3.getUserData(), 0);

        ObjectDataStore.Entry<String,Value> entry2again = objectDataStore.readEntry();
        log.info("entry2again [{}]", entry2again);

        Assert.assertNull("object is now marked as deleted", entry2again.getValue());
                // a null value indicates the object is now marked as deleted

        log.info("check read at EOF returns null");

        entry2 = objectDataStore.readEntry();
        log.info("entry (1) [{}] read at EOF", entry2);
        Assert.assertNull("null read at EOF (1)", entry2);

        log.info("check 2nd ead at EOF returns null");

        entry1 = objectDataStore.readEntry();
        log.info("entry (2) [{}] read at EOF", entry1);
        Assert.assertNull("null read at EOF (2)", entry1);

        objectDataStore.close();

    }
}
