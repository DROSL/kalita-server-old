package kalita;

import org.webbitserver.WebServer;
import org.webbitserver.WebServers;
import org.webbitserver.handler.StaticFileHandler;

public class kalitaServer {

	public static void main(String[] args) throws Exception {
		WebServer webServer = WebServers.createWebServer(8080);
		webServer.add(new StaticFileHandler("/static-files"));
		webServer.add("/kalitatts", new WebSocketHandler());
		webServer.start();
		System.out.println("Server started on port " + webServer.getPort());
	}
}
