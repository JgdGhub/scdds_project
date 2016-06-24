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
/**
 * Created by Jeromy Drake on 11/06/2016.
 */
public class Reply {
    private final String text;
    public Reply(String text) {
        this.text = wrapPage(text);
    }
    byte[] getBytes() {
        return text.getBytes();
    }

    private static String wrapPage(String pageFragment) {
        return "<http><body><H2>SC/DDS Cache Viewer</H2><hr><br>\r\n"
                + pageFragment
                +"</body></http>";
    }
}
