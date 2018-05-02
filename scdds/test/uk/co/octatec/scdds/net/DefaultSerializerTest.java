package uk.co.octatec.scdds.net;
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
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.net.serialize.*;
import uk.co.octatec.scdds.net.socket.BlockIoImpl;
import uk.co.octatec.scdds.utilities.SimpleData;
import uk.co.octatec.scdds.utilities.SimpleDataSerializer;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Jeromy Drake on 30/04/2016.
 */
public class DefaultSerializerTest {

    private final Logger log = LoggerFactory.getLogger(DefaultSerializerTest.class);

    static class SmallTestClass implements Serializable {
        char a = 'A';
    }

    static class TestClass implements Serializable {
        long serialVersionUID = 1L;
        int n;
        Double d = new Double(1.0);
        String s = "Test";
        ArrayList a;
        Date date = new Date();

        public TestClass() {
            a = new ArrayList();
            a.add("TEST-String");
            a.add(Boolean.TRUE);
            a.add(new BigDecimal(100.1234));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TestClass testClass = (TestClass) o;

            if (serialVersionUID != testClass.serialVersionUID) return false;
            if (n != testClass.n) return false;
            if (!d.equals(testClass.d)) return false;
            if (!s.equals(testClass.s)) return false;
            if (!a.equals(testClass.a)) return false;
            return date.equals(testClass.date);

        }

        @Override
        public int hashCode() {
            int result = (int) (serialVersionUID ^ (serialVersionUID >>> 32));
            result = 31 * result + n;
            result = 31 * result + d.hashCode();
            result = 31 * result + s.hashCode();
            result = 31 * result + a.hashCode();
            result = 31 * result + date.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "TestClass{" +
                    "serialVersionUID=" + serialVersionUID +
                    ", n=" + n +
                    ", d=" + d +
                    ", s='" + s + '\'' +
                    ", a=" + a +
                    ", date=" + date +
                    '}';
        }
    }


    @BeforeClass
    public static void setup() {
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO");
    }

    @Test
    public void smallSerializationTest() {
        log.info("## smallSerializationTest");

        SmallTestClass value1 = new SmallTestClass();
        String key1 = "X";
        DefaultSerializer<String, SmallTestClass> ser = new DefaultSerializer<>();
        byte[] buff = ser.serialize(key1, value1, 0);

        Assert.assertTrue("serialization is not null", buff != null);
        log.info("serialized t1={} k1={} into buff, length={}", value1, key1, buff.length);

        Serializer.Pair<String, SmallTestClass> pair = ser.deserialize(buff, 0);
        Assert.assertTrue("deserialization pair not null", pair != null);
        log.info("deserialized key={} value={} ", pair.key, pair.value);
    }

    @Test
    public void smallSerializationOffsetTest() {
        log.info("## smallSerializationTest");

        SmallTestClass value1 = new SmallTestClass();
        String key1 = "X";
        DefaultSerializer<String, SmallTestClass> ser = new DefaultSerializer<>();
        byte[] buff = ser.serialize(key1, value1, BlockIoImpl.HEADER_LENGTH);
        log.info("... buff", buff);
        buff[0] = 'A';
        buff[1] = 'B';
        buff[2] = 'C';
        buff[3] = 'D';
        buff[4] = 'E';

        Assert.assertTrue("serialization is not null", buff != null);
        log.info("serialized t1={} k1={} into buff, length={}", value1, key1, buff.length);

        Serializer.Pair<String, SmallTestClass> pair = ser.deserialize(buff, BlockIoImpl.HEADER_LENGTH);
        Assert.assertTrue("deserialization pair not null", pair != null);
        log.info("deserialized key={} value={} ", pair.key, pair.value);
    }

    @Test
    public void serializationTest() {
        log.info("## serializationTest");

        TestClass value1 = new TestClass();
        String key1 = "Key1";
        DefaultSerializer<String,TestClass> ser = new DefaultSerializer<>();
        byte[] buff = ser.serialize(key1, value1, 0);

        Assert.assertTrue("serialization is not null", buff!=null);
        log.info("serialized t1={} k1={} into buff, length={}", value1, key1, buff.length);

        Serializer.Pair<String, TestClass> pair = ser.deserialize(buff, 0);
        Assert.assertTrue("deserialization pair not null", pair!=null);
        log.info("deserialized key={} value={} ", pair.key, pair.value );

        Assert.assertTrue("deserialization key is correct", key1.equals(pair.key));

        Assert.assertTrue("deserialization value is correct", value1.equals(pair.value));
     }

    @Test
    public void nullObjectTest() {
        log.info("## nullObjectTest");

        TestClass value1 = null;
        String key1 = "Key1";
        DefaultSerializer<String,TestClass> ser = new DefaultSerializer<>();
        byte[] buff = ser.serialize(key1, value1, 0);

        Assert.assertTrue("serialization is not null", buff!=null);
        log.info("serialized t1={} into buff, length={}", value1, buff.length);

        Serializer.Pair<String,TestClass> pair= ser.deserialize(buff, 0);
        log.info("deserialized key={} value={}", pair.key, pair.value);

        Assert.assertTrue("deserialization is null", pair.value==null);
        Assert.assertTrue("deserialization key is not null", pair.key!=null);

        Assert.assertTrue("deserialization key is correct", key1.equals(pair.key));
    }

    @Test
    public void factoryTest() {
        log.info("## factoryTest");

        SerializerFactory<String,Integer> f = new SerializerFactoryDefaultImpl<>();
        Serializer<String, Integer> ser = f.create();
        String s1 = "Key_1";
        Integer i1 = new Integer(1) ;
        byte[] buff = ser.serialize(s1, i1, 0);
        Serializer.Pair pair = ser.deserialize(buff, 0);
        log.info("s1=[{}], pair.key=[{}] [{}] i1=[{}] pair.value=[{}]", s1, pair.key, i1, pair.value);
        Assert.assertEquals("strings serialized ok", s1, pair.key);
        Assert.assertEquals("integers serialized ok", i1, pair.value);

    }

    @Test
    public void serializerUtilsTest() {

        log.info("## serializerUtilsTest");

        int n = 123456789;
        byte[] b = new byte[4];
        SerializerUtils.writeIntToBytes(n, b);
        int m = SerializerUtils.readIntFromBytes(b);
        log.info("check ints [{}] [{}]", m, n);
        Assert.assertEquals("ints equal", n, m);

        n = Integer.MAX_VALUE;
        b = new byte[4];
        SerializerUtils.writeIntToBytes(n, b);
        m = SerializerUtils.readIntFromBytes(b);
        log.info("check ints max [{}] [{}]", m, n);
        Assert.assertEquals("ints max equal", n, m);

        n = Integer.MIN_VALUE;
        b = new byte[4];
        SerializerUtils.writeIntToBytes(n, b);
        m = SerializerUtils.readIntFromBytes(b);
        log.info("check ints min [{}] [{}]", m, n);
        Assert.assertEquals("ints min equal", n, m);

        n = 0;
        b = new byte[4];
        SerializerUtils.writeIntToBytes(n, b);
        m = SerializerUtils.readIntFromBytes(b);
        log.info("check ints zero [{}] [{}]", m, n);
        Assert.assertEquals("ints zero equal", n, m);

        n = -1;
        b = new byte[4];
        SerializerUtils.writeIntToBytes(n, b);
        m = SerializerUtils.readIntFromBytes(b);
        log.info("check ints -1 [{}] [{}]", m, n);
        Assert.assertEquals("ints -1 equal", n, m);

        long l = System.currentTimeMillis();
        b = new byte[8];
        SerializerUtils.writeLongToBytes(l, b);
        long k = SerializerUtils.readLongFromBytes(b);
        log.info("check longs [{}] [{}]", l, k);
        Assert.assertEquals("longs equal", l, k);

        l = Long.MAX_VALUE;
        b = new byte[8];
        SerializerUtils.writeLongToBytes(l, b);
        k = SerializerUtils.readLongFromBytes(b);
        log.info("check longs (max) [{}] [{}]", l, k);
        Assert.assertEquals("longs equal (max)", l, k);

        l = Long.MIN_VALUE;
        b = new byte[8];
        SerializerUtils.writeLongToBytes(l, b);
        k = SerializerUtils.readLongFromBytes(b);
        log.info("check longs (min) [{}] [{}]", l, k);
        Assert.assertEquals("longs equal (min)", l, k);

        l = 0L;
        b = new byte[8];
        SerializerUtils.writeLongToBytes(l, b);
        k = SerializerUtils.readLongFromBytes(b);
        log.info("check longs (zero) [{}] [{}]", l, k);
        Assert.assertEquals("longs equal (zero)", l, k);

        l = -1L;
        b = new byte[8];
        SerializerUtils.writeLongToBytes(l, b);
        k = SerializerUtils.readLongFromBytes(b);
        log.info("check longs (-1) [{}] [{}]", l, k);
        Assert.assertEquals("longs equal (-1)", l, k);

        float f1 = 1091.12345f;
        b = new byte[4];
        SerializerUtils.writeFloatToBytes(f1, b, 0);
        float f2 =  SerializerUtils.readFloatFromBytes(b, 0);
        log.info("check float [{}] [{}]", f1, f2);
        Assert.assertTrue("floats equal (1091.12345f)", approximatelyEqual(f1,f2));

        f1 = -1091.12345f;
        b = new byte[4];
        SerializerUtils.writeFloatToBytes(f1, b, 0);
        f2 =  SerializerUtils.readFloatFromBytes(b, 0);
        log.info("check float [{}] [{}]", f1, f2);
        Assert.assertTrue("floats equal (-1091.12345f)", approximatelyEqual(f1,f2));

        f1 = Float.MAX_VALUE;
        b = new byte[4];
        SerializerUtils.writeFloatToBytes(f1, b, 0);
        f2 =  SerializerUtils.readFloatFromBytes(b, 0);
        log.info("check float [{}] [{}]", f1, f2);
        Assert.assertTrue("floats equal (MAX_VALUE)", approximatelyEqual(f1,f2));

        f1 = Float.MIN_VALUE;
        b = new byte[4];
        SerializerUtils.writeFloatToBytes(f1, b, 0);
        f2 =  SerializerUtils.readFloatFromBytes(b, 0);
        log.info("check float [{}] [{}]", f1, f2);
        Assert.assertTrue("floats equal (MIN_VALUE)", approximatelyEqual(f1,f2));

        f1 = 0.0f;
        b = new byte[4];
        SerializerUtils.writeFloatToBytes(f1, b, 0);
        f2 =  SerializerUtils.readFloatFromBytes(b, 0);
        log.info("check float [{}] [{}]", f1, f2);
        Assert.assertTrue("floats equal (zero)", approximatelyEqual(f1,f2));

        f1 = -1.0f;
        b = new byte[4];
        SerializerUtils.writeFloatToBytes(f1, b, 0);
        f2 =  SerializerUtils.readFloatFromBytes(b, 0);
        log.info("check float [{}] [{}]", f1, f2);
        Assert.assertTrue("floats equal (-1)", approximatelyEqual(f1,f2));


        double d1 = 1091.12345;
        b = new byte[8];
        SerializerUtils.writeDoubleToBytes(d1, b, 0);
        double d2 =  SerializerUtils.readDoubleFromBytes(b, 0);
        log.info("check double [{}] [{}]", d1, d2);
        Assert.assertTrue("double equal (1091.12345f)", approximatelyEqual(d1,d2));

        d1 = -1091.12345;
        b = new byte[8];
        SerializerUtils.writeDoubleToBytes(d1, b, 0);
        d2 =  SerializerUtils.readDoubleFromBytes(b, 0);
        log.info("check double [{}] [{}]", d1, d2);
        Assert.assertTrue("double equal (-1091.12345f)", approximatelyEqual(d1,d2));

        d1 = Double.MAX_VALUE;
        b = new byte[8];
        SerializerUtils.writeDoubleToBytes(d1, b, 0);
        d2 =  SerializerUtils.readDoubleFromBytes(b, 0);
        log.info("check double [{}] [{}]", d1, d2);
        Assert.assertTrue("double equal (MAX_VALUE)", approximatelyEqual(d1,d2));

        d1 = Double.MIN_VALUE;
        b = new byte[8];
        SerializerUtils.writeDoubleToBytes(d1, b, 0);
        d2 =  SerializerUtils.readDoubleFromBytes(b, 0);
        log.info("check double [{}] [{}]", d1, d2);
        Assert.assertTrue("double equal (MIN_VALUE)", approximatelyEqual(d1,d2));

        d1 = 0.0;
        b = new byte[8];
        SerializerUtils.writeDoubleToBytes(d1, b, 0);
        d2 =  SerializerUtils.readDoubleFromBytes(b, 0);
        log.info("check double [{}] [{}]", d1, d2);
        Assert.assertTrue("double equal (zero)", approximatelyEqual(d1,d2));

        d1 = -1.0;
        b = new byte[8];
        SerializerUtils.writeDoubleToBytes(d1, b, 0);
        d2 =  SerializerUtils.readDoubleFromBytes(b, 0);
        log.info("check double [{}] [{}]", d1, d2);
        Assert.assertTrue("double equal (-1)", approximatelyEqual(d1,d2));

        short i1 = Short.MAX_VALUE;
        b = new byte[2];
        SerializerUtils.writeShortToBytes(i1, b, 0);
        short i2 = SerializerUtils.readShortFromBytes(b, 0);
        log.info("check short [{}] [{}]", i1, i2);
        Assert.assertEquals("short equal (MAX_VALUE)", i1, i2);

        i1 = Short.MIN_VALUE;
        b = new byte[2];
        SerializerUtils.writeShortToBytes(i1, b, 0);
        i2 = SerializerUtils.readShortFromBytes(b, 0);
        log.info("check short [{}] [{}]", i1, i2);
        Assert.assertEquals("short equal (MIN_VALUE)", i1, i2);

        i1 = 0;
        b = new byte[2];
        SerializerUtils.writeShortToBytes(i1, b, 0);
        i2 = SerializerUtils.readShortFromBytes(b, 0);
        log.info("check short [{}] [{}]", i1, i2);
        Assert.assertEquals("short equal (zero)", i1, i2);

        i1 = -1;
        b = new byte[2];
        SerializerUtils.writeShortToBytes(i1, b, 0);
        i2 = SerializerUtils.readShortFromBytes(b, 0);
        log.info("check short [{}] [{}]", i1, i2);
        Assert.assertEquals("short equal (-1)", i1, i2);

        char c1 = 'A';
        b = new byte[2];
        SerializerUtils.writeCharToBytes(c1, b, 0);
        char c2 = SerializerUtils.readCharFromBytes(b, 0);
        log.info("check char [{}] [{}]", c1, c2);
        Assert.assertEquals("char equal (A)", c1, c2);

        c1 = Character.MAX_VALUE;
        b = new byte[2];
        SerializerUtils.writeCharToBytes(c1, b, 0);
        c2 = SerializerUtils.readCharFromBytes(b, 0);
        log.info("check char [{}] [{}]", c1, c2);
        Assert.assertEquals("char equal (MAX)", c1, c2);

        c1 = Character.MAX_VALUE;
        b = new byte[2];
        SerializerUtils.writeCharToBytes(c1, b, 0);
        c2 = SerializerUtils.readCharFromBytes(b, 0);
        log.info("check char [{}] [{}]", c1, c2);
        Assert.assertEquals("char equal (MIN)", c1, c2);

    }

    @Test
    public void serializerUtilsStringSizeTest() {

        String s = "1234567890";
        Assert.assertEquals("size of string", s.length()+SerializerUtils.SIZE_OF_SHORT, SerializerUtils.SIZE_OF_STRING(s));
        Assert.assertEquals("size of short string", s.length()+1, SerializerUtils.SIZE_OF_SHORT_STRING(s));
        Assert.assertEquals("size of long string", s.length()+SerializerUtils.SIZE_OF_INT, SerializerUtils.SIZE_OF_LONG_STRING(s));

        s = null;
        Assert.assertEquals("size of null string", SerializerUtils.SIZE_OF_SHORT, SerializerUtils.SIZE_OF_STRING(s));
        Assert.assertEquals("size of null short string", 1, SerializerUtils.SIZE_OF_SHORT_STRING(s));
        Assert.assertEquals("size of null long string", SerializerUtils.SIZE_OF_INT, SerializerUtils.SIZE_OF_LONG_STRING(s));
    }

    @Test
    public void serializerUtilsStringTest() {

        int ofset = 5;

        byte[] buff = new byte[64];
        String s1 = "this is a small test string";
        SerializerUtils.writeShortStringToBytes(s1, buff, ofset);
        String x1 = SerializerUtils.readShortStringFromBytes(buff, ofset);
        Assert.assertEquals("string s1 correct", s1, x1);

        buff = new byte[96];
        String s2 = "this is a slightly longer string test string, still short realy";
        SerializerUtils.writeStringToBytes(s2, buff, ofset);
        String x2 = SerializerUtils.readStringFromBytes(buff, ofset);
        Assert.assertEquals("string s2 correct", s2, x2);

        buff = new byte[256];
        String s3 = "a much longer string, but not really a long one..." + s2 + s1;
        SerializerUtils.writeLongStringToBytes(s3, buff, ofset);
        String x3 = SerializerUtils.readLongStringFromBytes(buff, ofset);
        Assert.assertEquals("string s3 correct", s3, x3);
    }

    @Test
    public void serializerUtilsNullStringTest() {

        int ofset = 5;

        byte[] buff = new byte[64];
        String s1 = null;
        SerializerUtils.writeShortStringToBytes(s1, buff, ofset);
        String x1 = SerializerUtils.readShortStringFromBytes(buff, ofset);
        Assert.assertEquals("null string s1 correct", s1, x1);

        String s2 = null;
        SerializerUtils.writeStringToBytes(s2, buff, ofset);
        String x2 = SerializerUtils.readStringFromBytes(buff, ofset);
        Assert.assertEquals("null string s2 correct", s2, x2);

        String s3 = null;
        SerializerUtils.writeLongStringToBytes(s3, buff, ofset);
        String x3 = SerializerUtils.readLongStringFromBytes(buff, ofset);
        Assert.assertEquals("null string s3 correct", s3, x3);
    }

    @Test
    public void SimpleDataSerializerTest() {

        SimpleDataSerializer serializer = new SimpleDataSerializer();

        SimpleData data = new SimpleData("ATEST", 12000);
        Serializer.Pair<String, SimpleData> pair = null ;

        long start = 0;
        long end = 0;
        for(int i=1; i<10; i++) {  // warm up, use last loop as the mearsure
            start = System.nanoTime();
            byte[] buff  = serializer.serialize("KEY.1", data, 5);
            pair = serializer.deserialize(buff, 5);
            end = System.nanoTime();
        }
        long t1 = (end-start);
        log.info("TIME (specialized serializer) [{}]", t1);
        Assert.assertEquals("key check", "KEY.1", pair.key);
        Assert.assertEquals("value check", data, pair.value);
        log.info("SimpleDataSerializerTest: key [{}] value [{}]", pair.key, pair.value);

        DefaultSerializer<String, SimpleData> defaultSerializer = new DefaultSerializer<>();
        for(int i=1; i<10; i++) {  // warm up, use last loop as the mearsure
            start = System.nanoTime();
            byte[] buff = defaultSerializer.serialize("KEY.1", data, 5);
            pair = defaultSerializer.deserialize(buff, 5);
            end = System.nanoTime();
        }
        long t2 = (end-start);
        log.info("TIME defaultSerializer=[{}] specializedSerializer=[{}]", t2, t1);
        Assert.assertEquals("key check", "KEY.1", pair.key);
        Assert.assertEquals("value check", data, pair.value);
        log.info("SimpleDataSerializerTest: key [{}] value [{}]", pair.key, pair.value);

        Assert.assertTrue("specialized serializer is faster than default", t2 > t1);
                // not the best type of a performance test, but the difference is so large...

        data = null;
        byte[] buff  = serializer.serialize("KEY.1", data, 5);
        pair = serializer.deserialize(buff, 5);
        Assert.assertEquals("key check", "KEY.1", pair.key);
        Assert.assertNull("value check", pair.value);
        log.info("SimpleDataSerializerTest(null-value): key [{}] value [{}]", pair.key, pair.value);

    }

    static boolean approximatelyEqual(double f1, double f2) {
        double diff = f1 -f2;
        if( diff < 0 ) {
            diff = -diff;
        }
        return diff < 0.0000000001;
    }
}
