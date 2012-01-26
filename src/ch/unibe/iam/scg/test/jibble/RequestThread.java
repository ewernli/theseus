package ch.unibe.iam.scg.test.jibble;
/* 
Copyright Paul James Mutton, 2001-2004, http://www.jibble.org/

This file is part of Jibble Web Server / WebServerLite.

This software is dual-licensed, allowing you to choose between the GNU
General Public License (GPL) and the www.jibble.org Commercial License.
Since the GPL may be too restrictive for use in a proprietary application,
a commercial license is also provided. Full license information can be
found at http://www.jibble.org/licenses/

$Author: pjm2 $
$Id: RequestThread.java,v 1.2 2004/02/01 13:37:35 pjm2 Exp $

*/


import java.io.*;
import java.net.*;
import java.util.*;


/**
 * A thread which deals with an individual request to the web server.
 * This is passed a socket from the WebServer when a connection is
 * accepted.
 * 
 * @author Copyright Paul Mutton, http://www.jibble.org/
 */
public class RequestThread implements Runnable {

    public RequestThread(Socket socket, File rootDir, WebServer webServer ) {
        _socket = socket;
        _rootDir = rootDir;
        _webServer = webServer;
    }
    
    // handles a connction from a client.
    public void run() {
        String ip = "unknown";
        String request = "unknown";
        int bytesSent = 0;
        try {
            ip = _socket.getInetAddress().getHostAddress();
            BufferedReader in = new BufferedReader(new InputStreamReader(_socket.getInputStream()));
            BufferedOutputStream out = new BufferedOutputStream(_socket.getOutputStream());
            
            String path = "";
            // Read the first line from the client.
            request = in.readLine();
            if (request != null && request.startsWith("GET ") && (request.endsWith(" HTTP/1.0") || request.endsWith("HTTP/1.1"))) {
                path = request.substring(4, request.length() - 9);
            }
            else {
                // Invalid request type (no "GET")
                Logger.log(ip, request, 405);
                _socket.close();
                return;
            }
            
            //Read in and store all the headers.
            HashMap headers = new HashMap();
            String line = null;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.equals("")) {
                    break;
                }
                int colonPos = line.indexOf(":");
                if (colonPos > 0) {
                    String key = line.substring(0, colonPos);
                    String value = line.substring(colonPos + 1);
                    headers.put(key, value.trim());
                }
            }
            
            String data = URLDecoder.decode(path);
           
            Logger.log(ip, request, 200);           
            out.write(("HTTP/1.0 200 OK\r\n" +
                       "Content-Type: text/html\r\n" +
                       "Expires: Thu, 01 Dec 1994 16:00:00 GMT\r\n" +
                       "\r\n" +
                       "<h1>Hello ABB</h1>" +
                       "<h3>URL: " + path + "</h3>" +
                       "<h3>Server State #: " + _webServer.getCounter() + "</h3>" +
                       "<h3>Server Class: "+ _webServer.getClass().toString() + "</h3>").getBytes());
            
            out.flush();
            _socket.close();
            return;         
        }
        catch (IOException e) {
            Logger.log(ip, "ERROR " + e.toString() + " " + request, 0);   
        }
    }
    
    private Socket _socket;
    private File _rootDir;
    private WebServer _webServer;
}