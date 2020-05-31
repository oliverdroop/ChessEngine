package http;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpServer;

public class MyServerContainer {
	private static final Logger LOGGER = LoggerFactory.getLogger(MyServerContainer.class);
	
	private static MyHttpHandler myHttpHandler = new MyHttpHandler();
	
	public MyServerContainer() {
		try {
    		LOGGER.info("Starting application");
    		HttpServer httpServer = HttpServer.create(new InetSocketAddress("178.62.85.228", 80), 1024);
    		//HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 80), 1024);
    		LOGGER.info("Created httpServer");
    		List<String> contexts = getAllContexts();
    		contexts.forEach(c -> httpServer.createContext(c, myHttpHandler));
    		contexts.forEach(c -> LOGGER.info("Created context: {}", c));
    		httpServer.start();
    		LOGGER.info("Started httpServer");
    	}
    	catch(IOException ioe) {
    		LOGGER.info(ioe.getMessage());
    	}
	}
    
    private List<String> findFileContexts(){
    	File directory = new File(System.getProperty("user.dir"));
    	File[] files = directory.listFiles();
    	List<String> contexts = new ArrayList<>();
    	for(String extension : myHttpHandler.getValidFileExtensions()) {
    		for(File file : files) {
	    		if (file.getName().endsWith(extension)) {
	    			contexts.add("/" + file.getName());
	    		}
    		}
    	}
    	return contexts;
    }
    
    private List<String> getAllContexts(){
    	List<String> contexts = findFileContexts();
    	contexts.add("/");
    	contexts.add("/chess");
    	return contexts;
    }
    
    public static void main(String[] args) {
    	new MyServerContainer();
    }
}
