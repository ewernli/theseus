package ch.unibe.iam.scg.test.jibble;

public class WebServerThread implements Runnable {
	private WebServer _webServer;
	
	public WebServerThread( WebServer ws ) {
		_webServer = ws;
	}

	public void run() {
		try {
			_webServer.listen();
		} catch (WebServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
