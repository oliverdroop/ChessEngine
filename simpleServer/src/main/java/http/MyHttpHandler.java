package http;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class MyHttpHandler implements HttpHandler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MyHttpHandler.class);


	public void handle(HttpExchange httpExchange) throws IOException {
		LOGGER.info("Handling httpExchange {}", httpExchange.getHttpContext());
		LOGGER.info("RequestURI : {}", httpExchange.getRequestURI());
		respond(httpExchange);
	}
	
	public void respond(HttpExchange httpExchange) {
		try {
			String responseString = "Hello";
			httpExchange.sendResponseHeaders(200, responseString.length());
			httpExchange.getResponseBody().write(responseString.getBytes());
		}
		catch (IOException ioe) {
			LOGGER.warn(ioe.getMessage());
		}
	}

}
