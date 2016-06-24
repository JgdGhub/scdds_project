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
import junit.framework.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.net.html.links.CacheDisplayLinkGenerator;
import uk.co.octatec.scdds.net.html.links.CacheEntryDisplayLinkGenerator;
import uk.co.octatec.scdds.net.html.links.NoOpLinkGenerator;

/**
 * Created by Jeromy Drake on 14/06/2016.
 */
public class LinkGeneratorTest {
    private static final Logger log = LoggerFactory.getLogger(LinkGeneratorTest.class);

    @Test
    public void cacheDisplayLinkGeneratorTest() {
        CacheDisplayLinkGenerator cacheDisplayLinkGenerator = new CacheDisplayLinkGenerator();
        String link = cacheDisplayLinkGenerator.getLink("CACHE-NAME");
        log.info("link [{}]", link);
        Assert.assertEquals("link", link, "<a href=\"/scdds/cache/CACHE-NAME\">CACHE-NAME</a>");
    }

    @Test
    public void cacheEntryDisplayLinkGeneratorTest() {
        CacheEntryDisplayLinkGenerator cacheEntryDisplayLinkGenerator = new CacheEntryDisplayLinkGenerator("CACHE-NAME");
        String link = cacheEntryDisplayLinkGenerator.getLink("KEY");
        log.info("link [{}]", link);
        Assert.assertEquals("link", link, "<a href=\"/scdds/cache/CACHE-NAME/KEY\">KEY</a>");
    }

    @Test
    public void noOpLinkGeneratorTest() {
        NoOpLinkGenerator noOpLinkGenerator = new NoOpLinkGenerator();
        String link = noOpLinkGenerator.getLink("SOME-TEXT");
        log.info("link [{}]", link);
        Assert.assertEquals("link", link, "SOME-TEXT");
    }

}
