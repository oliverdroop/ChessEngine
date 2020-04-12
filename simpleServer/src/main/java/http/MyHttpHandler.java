package http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import chess.Game;

public class MyHttpHandler implements HttpHandler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MyHttpHandler.class);


	public void handle(HttpExchange httpExchange) throws IOException {
		LOGGER.info("Handling httpExchange {}", httpExchange.getHttpContext());
		//LOGGER.info("RequestURI : {}", httpExchange.getRequestURI());
		respond(httpExchange);
	}
	
	public void respond(HttpExchange httpExchange) {
		try {
			String responseString = "Hello";
			
			Game game = new Game(false);
			httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
			if (httpExchange.getRequestMethod().equals("GET")) {
				responseString = game.getBoardState();
			}
			if (httpExchange.getRequestMethod().equals("POST")) {
				String in = read(httpExchange.getRequestBody());
				LOGGER.info("Received : " + in);
				game.setBoardState(in);
				//LOGGER.info("Board state set successfully");
				game.playAIMove();
				//LOGGER.info("AI moved successfully");
				responseString = game.getBoardState();
				LOGGER.info("Transmit : " + responseString);
			}
			
			httpExchange.sendResponseHeaders(200, responseString.length());
			httpExchange.getResponseBody().write(responseString.getBytes());
			httpExchange.getResponseBody().close();
		}
		catch (IOException ioe) {
			LOGGER.warn(ioe.getMessage());
		}
	}
	
	public String read(InputStream inputStream) throws IOException{
		String in = "";
		BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
		while(br.ready() || in.length() == 0) {
			in += br.readLine();
		}
		inputStream.close();
		return in;
	}
}
