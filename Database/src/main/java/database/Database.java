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
	
	public Database(String rootDirectory, boolean loadData) {
		LOGGER.info("Started database");
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
		
		if (loadData) {
			for(Table t : getTables().values()) {
				t.load(rootDirectory + File.separator + t.getName() + ".ddbt");
			}
		}
	}
	
	public void listen() {
		CommandListener.listen(this);
	}
	
	public static void main(String[] args) {
		String path = System.getProperty("user.dir");
		boolean loadData = false;
		if (args.length > 0) {
			path = args[args.length - 1];
			if (args.length > 1) {
				for(int index = 0; index < args.length - 1; index++) {
					if (args[index].equals("-l") || args[index].equals("--load")) {
						loadData = true;
					}
				}
			}
		}
		Database database = new Database(path, loadData);	
		database.listen();
	}
	
	public Map<String, Table> getTables(){
		return tables;
	}
}
