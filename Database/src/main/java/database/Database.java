package database;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Database {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Database.class);
	
	private Map<String, Table> tables;
	
	public Database(String rootDirectory) {
		File directory = new File(rootDirectory);
    	File[] files = directory.listFiles();
    	tables = new HashMap<>();
		for(File file : files) {
    		if (file.getName().endsWith(".dbtd")) {
    			Table table = new Table(file);
    			tables.put(table.getName(), table);
    			LOGGER.info("Created new table: {}", table.getName());
    		}
		}
	}
	
	public static void main(String[] args) {
		new Database(System.getProperty("user.dir"));
	}
	
	public Map<String, Table> getTables(){
		return tables;
	}
}
