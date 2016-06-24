package uk.co.octatec.scdds.net.html;
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
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.net.DefaultSerializerTest;
import uk.co.octatec.scdds.net.html.format.BeanHtmlFormatter;
import uk.co.octatec.scdds.net.html.format.CollectionHtmlFormatter;
import uk.co.octatec.scdds.net.html.format.CompactCollectionHtmlFormatter;
import uk.co.octatec.scdds.net.html.format.MapHtmlFormatter;
import uk.co.octatec.scdds.utilities.SimpleData;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Jeromy Drake on 12/06/2016.
 */
public class FormatterTest {
    private final Logger log = LoggerFactory.getLogger(DefaultSerializerTest.class);

    @Test
    public void mapFormatterTest() {
        log.info("##mapFormatterTest");
        HashMap<String, SimpleData>  map = new  HashMap<>();
        map.put("R1", new SimpleData("Data1", 1));
        map.put("R2", new SimpleData("Data2", 2));
        map.put("R3", new SimpleData("Data3", 3));
        MapHtmlFormatter<String, SimpleData> mapFormatter = new MapHtmlFormatter<>(map);
        String html =  mapFormatter.format("TEST", "Name", "Value");
        log.info("HTML\r\n{}", html);
        Assert.assertNotNull("html not null", html);
        Assert.assertNotNull("html correct start", html.startsWith("<table cellpadding=\"3\" border=\"1\"><tbody>"));
        Assert.assertNotNull("htmlcorrect end", html.startsWith("</tbody></table>"));
    }

    @Test
    public void mapFormatterExpandedTest() {
        log.info("##mapFormatterExpandedTest");
        HashMap<String, SimpleData>  map = new  HashMap<>();
        map.put("R1", new SimpleData("Data1", 1));
        map.put("R2", new SimpleData("Data2", 2));
        map.put("R3", new SimpleData("Data3", 3));
        MapHtmlFormatter<String, SimpleData> mapFormatter = new MapHtmlFormatter<>(map, false/*don't use toString() on values*/);
        String html =  mapFormatter.format("TEST", "Name", "Value");
        log.info("HTML\r\n{}", html);
        Assert.assertNotNull("html not null", html);
        Assert.assertNotNull("html correct start", html.startsWith("<table cellpadding=\"3\" border=\"1\"><tbody>"));
        Assert.assertNotNull("htmlcorrect end", html.startsWith("</tbody></table>"));
    }

    @Test
    public void collectionFormatterTest() {
        log.info("##collectionFormatterTest");
        ArrayList<String> list = new  ArrayList<>();
        list.add("String Number 1");
        list.add("String Number 2");
        list.add("String Number 3");
        list.add("String Number 4");
        CollectionHtmlFormatter<String> collectionFormatter = new CollectionHtmlFormatter<>(list);
        String html =  collectionFormatter.format("Test String List");
        log.info("HTML\r\n{}", html);
        Assert.assertNotNull("html not null", html);
        Assert.assertNotNull("html correct start", html.startsWith("<table cellpadding=\"3\" border=\"1\"><tbody>"));
        Assert.assertNotNull("htmlcorrect end", html.startsWith("</tbody></table>"));
    }

    @Test
    public void collectionFormatterExpandedTest() {
        log.info("##collectionFormatterExpandedTest");
        ArrayList<SimpleData> list = new  ArrayList<>();
        list.add(new SimpleData("data-1", 1));
        list.add(new SimpleData("data-2", 2));
        list.add(new SimpleData("data-3", 3));
        list.add(new SimpleData("data-4", 4));
        CollectionHtmlFormatter<SimpleData> collectionFormatter = new CollectionHtmlFormatter<>(list, false/*don't use toString on value*/);
        String html =  collectionFormatter.format("Test SimpleData List");
        log.info("HTML\r\n{}", html);
        Assert.assertNotNull("html not null", html);
        Assert.assertNotNull("html correct start", html.startsWith("<table cellpadding=\"3\" border=\"1\"><tbody>"));
        Assert.assertNotNull("htmlcorrect end", html.startsWith("</tbody></table>"));
    }

    @Test
    public void compactCollectionFormatterTest() {
        log.info("##collectionFormatterTest");
        ArrayList<String> list = new  ArrayList<>();
        for(int i=0; i<50; i++) {
            list.add("Item_"+i);
        }
        CompactCollectionHtmlFormatter<String> compactCollectionFormatter = new CompactCollectionHtmlFormatter<>(list);

        String html1 =  compactCollectionFormatter.format("Test String List", 5/* 5 columns per row*/);
        log.info("HTML\r\n{}", html1);
        Assert.assertNotNull("html1 not null", html1);
        Assert.assertNotNull("html1 correct start", html1.startsWith("<table cellpadding=\"3\" border=\"1\"><tbody>"));
        Assert.assertNotNull("html1correct end", html1.startsWith("</tbody></table>"));

        String html2 =  compactCollectionFormatter.format("Test String List", 10/* 10 columns per row*/);
        log.info("HTML\r\n{}", html2);
        Assert.assertNotNull("html2 not null", html2);
        Assert.assertNotNull("html2 correct start", html2.startsWith("<table cellpadding=\"3\" border=\"1\"><tbody>"));
        Assert.assertNotNull("html2 correct end", html2.startsWith("</tbody></table>"));
    }

    @Test
    public void beanFormatterTest() {
        log.info("##beanFormatterTest");
        SimpleData bean = new SimpleData("Value_Of_Data_1", 2);
        BeanHtmlFormatter<SimpleData> beanFormatter = new BeanHtmlFormatter<>();
        String html =  beanFormatter.format(bean);
        log.info("HTML\r\n{}", html);
        Assert.assertNotNull("html not null", html);
        Assert.assertNotNull("html correct start", html.startsWith("<table cellpadding=\"3\" border=\"1\"><tbody>"));
        Assert.assertNotNull("htmlcorrect end", html.startsWith("</tbody></table>"));
    }
}
