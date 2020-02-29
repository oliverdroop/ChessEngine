package http;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpServer;

public class MyServerContainer {
	private static final Logger LOGGER = LoggerFactory.getLogger(MyServerContainer.class);
	
	private static MyHttpHandler myhttpHandler = new MyHttpHandler();
	
    public static void main(String[] args){
    	try {
    		LOGGER.info("Starting application");
    		HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 8080), 8080);
    		LOGGER.info("Created httpServer");
    		httpServer.createContext("/welcome", myhttpHandler);
    		LOGGER.info("Created context '/welcome'");
    		httpServer.start();
    		LOGGER.info("Started httpServer");
    		
    	}
    	catch(IOException ioe) {
    		LOGGER.info(ioe.getMessage());
    	}
    }
}
