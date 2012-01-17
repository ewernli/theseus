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
$Id: WebServer.java,v 1.2 2004/02/01 13:37:35 pjm2 Exp $

*/


import java.io.*;
import java.net.*;
import java.util.*;

import ch.unibe.iam.scg.ContextAware;
import ch.unibe.iam.scg.ContextClassLoader;


/**
 * The central class to the Jibble Web Server.  This is instantiated
 * by the WebServerMain class and listens for connections on the
 * specified port number before starting a new RequestThread to
 * allow connections to be dealt with concurrently.
 * 
 * @author Copyright Paul Mutton, http://www.jibble.org/
 */
public class WebServer {
    
	public WebServer() throws WebServerException  {
		this( "/Users/ewernli/Downloads/" , 9000);
	}
	
    public WebServer(String rootDir, int port) throws WebServerException {
        try {
            _rootDir = new File(rootDir).getCanonicalFile();
        }
        catch (IOException e) {
            throw new WebServerException("Unable to determine the canonical path of the web root directory.");
        }
        if (!_rootDir.isDirectory()) {
            throw new WebServerException("The specified root directory does not exist or is not a directory.");
        }
        _port = port;
    }
    
    public void activate() throws WebServerException {
        try {
        	_serverSocket = new ServerSocket(_port);
        	listen();
        }
        catch (Exception e) {
            throw new WebServerException("Cannot start the web server on port " + _port + ".", e);
        }
    }
    
    public void listen() throws WebServerException {   
         try {
        	
        	System.out.println("Counter: "+counter);
            // Pass the socket to a new thread so that it can be dealt with
            // while we can go and get ready to accept another connection.
            Socket socket = _serverSocket.accept();
            RequestThread reqRunnable = new RequestThread(socket, _rootDir);
            new Thread( reqRunnable).start();
            
            // Update itself
            counter ++;
            ContextClassLoader newContext = new ContextClassLoader( "$$" + counter );
            Runnable wsRunnable = new WebServerThread(this);
            Runnable newWsRunnable = (Runnable) ((ContextAware)wsRunnable).migrateToNext(newContext);
            new Thread( newWsRunnable).start();
        }
        catch (Exception e) {
            throw new WebServerException("Error processing new connection: " + e, e);
        }
    }
    
    private ServerSocket _serverSocket;
    private File _rootDir;
    private int _port;
    private int counter = 1;
    
}