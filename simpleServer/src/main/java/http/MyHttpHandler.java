package http;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpContext;

import chess.Game;

public class MyHttpHandler implements HttpHandler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MyHttpHandler.class);


	public void handle(HttpExchange httpExchange) throws IOException {
		LOGGER.info("Handling httpExchange {} {} {}", httpExchange.getRemoteAddress().toString() , httpExchange.getRequestMethod() ,httpExchange.getHttpContext().getPath());
		//LOGGER.info("RequestURI : {}", httpExchange.getRequestURI());
		respond(httpExchange);
	}
	
	private void respond(HttpExchange httpExchange) throws IOException{
		byte[] responseBytes = "Hello".getBytes();
		httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

		if (httpExchange.getRequestMethod().equals("GET")) {
			if (httpExchange.getHttpContext().getPath().equals("/")) {
				responseBytes = readFile("home.html");	
			}
			if (isContextWithValidExtension(httpExchange.getHttpContext())) {				
				responseBytes = readFile(getFilePathFromContext(httpExchange.getHttpContext()));
			}
		}
		
		if (httpExchange.getRequestMethod().equals("POST")) {
			String in = read(httpExchange.getRequestBody());
			responseBytes = getPostResponse(httpExchange.getHttpContext(), in);
		}
		
		completeExchange(httpExchange, responseBytes);
	}
	
	private byte[] getPostResponse(HttpContext httpContext, String postedString) {
		LOGGER.info("Received : " + postedString);
		byte[] responseBytes = new byte[0];
		if (httpContext.getPath().equals("/chess")) {
			Game game = new Game();
			game.setBoardState(postedString);
			game.playAIMove();
			responseBytes = game.getBoardState().getBytes();
		}
		LOGGER.info("Transmit : " + new String(responseBytes, Charset.forName("UTF-8")));
		return responseBytes;
	}
	
	private String read(InputStream inputStream) throws IOException{
		int timeout = 3000;
		long requestStart = System.currentTimeMillis();
		StringBuilder in = new StringBuilder();
		BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
		while((br.ready() || in.length() == 0) && System.currentTimeMillis() < requestStart + timeout) {
			in.append(br.readLine());
		}
		inputStream.close();
		return in.toString();
	}
    
    private byte[] readFile(String path) {
    	try {
    		File file = new File(path);
    		Path actualPath = file.toPath();
    		return Files.readAllBytes(actualPath);
    	}
    	catch(IOException e) {
    		e.printStackTrace();
    		LOGGER.warn("Problem reading file : {}", e.getMessage());
    		return null;
    	}
    }
    
    public List<String> getValidFileExtensions(){
    	return Arrays.asList(".html",".jpg",".png",".bmp", ".ico", ".js");
    }
    
    private boolean isContextWithValidExtension(HttpContext context) {
    	for(String extension : getValidFileExtensions()) {
    		if (context.getPath().endsWith(extension)) {
    			return true;
    		}
    	}
    	return false;
    }
    
    private String getFilePathFromContext(HttpContext context) {
    	return context.getPath().substring(1);
    }
    
    private void completeExchange(HttpExchange httpExchange, byte[] responseBytes){
    	try {
    		LOGGER.debug("Sending {} bytes", responseBytes.length);
	    	httpExchange.sendResponseHeaders(200, responseBytes.length);
	    	OutputStream outputStream = httpExchange.getResponseBody();
	    	outputStream.write(responseBytes);
	    	outputStream.close();
			LOGGER.debug("Sent {} bytes", responseBytes.length);
    	}
    	catch(IOException e) {
    		LOGGER.warn(e.getMessage());
    	}
    }
}
