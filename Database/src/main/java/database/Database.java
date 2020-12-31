package database;

import java.io.File;
import java.sql.SQLException;
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
	
	public Database(String rootDirectory, boolean loadData, PersistenceType persistenceType) {
		LOGGER.info("Started database");
		File directory = new File(rootDirectory);
    	File[] files = directory.listFiles();
    	tables = new HashMap<>();
		for(File file : files) {
    		if (file.getName().endsWith(".dbtd")) {
    			Table table = new Table(file);
    			table.setPersistenceType(persistenceType);
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
		PersistenceType persistenceType = PersistenceType.IMMEDIATE;
		for(String arg : args) {
			for(PersistenceType pType : PersistenceType.values()) {
				if (arg.equals(pType.name())) {
					persistenceType = pType;
				}
			}
		}
		Database database = new Database(path, true, persistenceType);	
		database.listen();
	}
	
	public Map<String, Table> getTables(){
		return tables;
	}
	
	public void addTable(Table table) {
		String tableName = table.getName();
		if (tableName != null && tableName.length() > 0 && tables.get(tableName) == null) {
			tables.put(table.getName(), table);
		}
	}
	
	public void removeTable(String tableName) {
		if (tableName != null && tableName.length() > 0 && tables.get(tableName) != null) {
			tables.remove(tableName);
		}
	}
}
