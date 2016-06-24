package uk.co.octatec.scdds.net.registry.http;
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
import uk.co.octatec.scdds.net.registry.RegistryServerSession;
import uk.co.octatec.scdds.net.socket.Session;

import java.io.IOException;
import java.util.StringTokenizer;

/**
 * Created by Jeromy Drake on 17/06/2016.
 *
 * (Just used in the Registry server to provide xml-listing of registry entries over http  - /scdds/registry/)
 *
 * A very lightweight http implementation, just enough for the needs of the Registry, which are very minor - this is used so
 * that the registry can use the same socket for its own protocol and also for HTTP requests - the registry doesn't expect to
 * handle very many HTTP requests
 */
public class LightweightHttp {

    private final static Logger log = LoggerFactory.getLogger(RegistryServerSession.class);

    private final Session sc;

    private String httpProto;
    private String httpMethod;

    public LightweightHttp(Session sc) {
        this.sc = sc;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getHttpProtocolVersion() {
        return httpProto;
    }

    public void replyNotFound() throws IOException {
        String text =
            "<html><head><title>404 Not Found</title></head>\r\n"+
            "<body><h1>Not Found</h1><p>The requested URL was not found on this server.</p></body>\r\n"+
            "</html>";
        sendHttpReply(text, 404);
    }

    public void sendHttpReply(String text) throws IOException {
        sendHttpReply(text, 200);
    }

    public void sendHttpReply(String text, int code) throws IOException {
        String textCode = code==404 ? "Not Found" : "OK";
        sendReply(text, code, textCode, true/* is html;*/);
    }

    public void sendXmlReply(String text) throws IOException {
        sendXmlReply(text, 200);
    }

    public void sendXmlReply(String text, int code) throws IOException {
        String textCode = code==404 ? "Not Found" : "OK";
        sendReply(text, code, textCode, false/* not html;*/);
    }

    void sendReply(String text, int code, String codeText, boolean isHtml) throws IOException {
        String contentType = isHtml ? "text/html" : "application/xml";
        String reply = httpProto+" "+code+" "+codeText+"\r\n" +
            "Content-Length: "+text.length()+"\r\n"+
            "Content-Type: "+contentType+"\r\n"+
                "\r\n"+
            text;

        byte[] bytes = reply.getBytes();
        log.info("send HTPP reply [{}]", reply);
        sc.write(bytes, 0, bytes.length);
    }

    public String readRequest(byte[] buff) throws IOException {
        StringBuilder request = new StringBuilder();
        request.append((char)buff[0]);// the first byte is expected to have already been read
        while(true) {
            // not very efficient, but we don't expect many HTTP commands
            sc.read(buff, 0, 1);
            if( buff[0] == '\r') {
                continue;
            }
            if( buff[0] == '\n') {
                break;
            }
            request.append((char)buff[0]);
        }
        log.info("got HTTP request [{}]", request);  // e.g. GET  /scdds/registry  HTTP/1.1
        StringTokenizer t = new StringTokenizer(request.toString());
        try {
            httpMethod = t.nextToken();
            String target = t.nextToken();
            httpProto = t.nextToken();
            return target;
        }
        catch( Exception e) {
            log.error("failed to decode http request ["+request+"]", e);
            return null;
        }
    }
}
