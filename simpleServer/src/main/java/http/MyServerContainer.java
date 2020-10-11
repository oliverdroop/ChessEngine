package http;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpServer;

public class MyServerContainer {
	private static final Logger LOGGER = LoggerFactory.getLogger(MyServerContainer.class);
	
	private static MyHttpHandler myHttpHandler = new MyHttpHandler();
	
	public MyServerContainer() {
		try {
    		LOGGER.debug("Starting application");
    		HttpServer httpServer = HttpServer.create(new InetSocketAddress("178.62.85.228", 80), 1024);
    		//HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 80), 1024);
    		LOGGER.debug("Created httpServer");
    		List<String> contexts = getAllContexts();
    		contexts.forEach(c -> httpServer.createContext(c, myHttpHandler));
    		contexts.forEach(c -> LOGGER.debug("Created context: {}", c));
    		httpServer.start();    		
    		LOGGER.info("STARTED HTTP SERVER - {}", getDateString());
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
    
    private String getDateString() {
		Calendar calendar = Calendar.getInstance();
    	StringBuilder dateBuilder = new StringBuilder();
		dateBuilder.append(String.format("%tH", calendar));
		dateBuilder.append(":");
		dateBuilder.append(String.format("%tM", calendar));
		dateBuilder.append(":");
		dateBuilder.append(String.format("%tS", calendar));
		dateBuilder.append("\t");
		dateBuilder.append(String.format("%td", calendar));
		dateBuilder.append("/");
		dateBuilder.append(String.format("%tm", calendar));
		dateBuilder.append("/");
		dateBuilder.append(String.format("%tY", calendar));
		return dateBuilder.toString();
    }
    
    public static void main(String[] args) {
    	new MyServerContainer();
    }
}
