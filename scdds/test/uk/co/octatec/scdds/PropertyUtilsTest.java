package uk.co.octatec.scdds;
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
import uk.co.octatec.scdds.cache.publish.PropertyUtils;

import java.util.Properties;

/**
 * Created by Jeromy Drake on 04/05/16
 */
public class PropertyUtilsTest {

    private final Logger log = LoggerFactory.getLogger(PropertyUtilsTest.class);

    @Test
    public void utilsPropertiesTest1() {
        log.info("## utilsPropertiesTest1");
        String args = "aaa=AAA:bbb=BBB:";
        Properties properties = PropertyUtils.getPropertiesFromString(args);
        log.info("properteis (1) [{}] {}", args, properties);
        Assert.assertTrue("properties not null", properties != null);
        Assert.assertTrue("properties correct size", properties.size()==2);
        Assert.assertTrue("properties value AAA", properties.getProperty("aaa").equals("AAA"));
        Assert.assertTrue("properties value BBB", properties.getProperty("bbb").equals("BBB"));
    }

    @Test
    public void utilsPropertiesTest2() {
        log.info("## utilsPropertiesTest2");
        String args = "aaa=AAA:bbb=BBB";
        Properties properties = PropertyUtils.getPropertiesFromString(args);
        log.info("properteis (2) [{}] {}", args, properties);
        Assert.assertTrue("properties not null", properties != null);
        Assert.assertTrue("properties correct size", properties.size()==2);
        Assert.assertTrue("properties value AAA", properties.getProperty("aaa").equals("AAA"));
        Assert.assertTrue("properties value BBB", properties.getProperty("bbb").equals("BBB"));
    }

    @Test
    public void utilsStringTest() {

        log.info("## utilsStringTest");
        String value = PropertyUtils.createPropertyString("xxx", "XXX", "yyy", "YYY");
        String expected = "xxx=XXX:yyy=YYY";
        log.info("expected [{}] got [{}]", expected, value);
        Assert.assertEquals("string as expected (1)", expected, value);

        value = PropertyUtils.createPropertyString("xxx", "XXX", "yyy");
        expected = "xxx=XXX";
        log.info("expected [{}] got [{}]", expected, value);
        Assert.assertEquals("string as expected (2)", expected, value);

        value = PropertyUtils.createPropertyString("xxx", "XXX", "yyy", "YYY", "zzz");
        expected = "xxx=XXX:yyy=YYY";
        log.info("expected [{}] got [{}]", expected, value);
        Assert.assertEquals("string as expected (2)", expected, value);
    }
}
