package database;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Database {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Database.class);
	
	private Map<String, Table> tables;
	
	public Database() {
		String rootDirectory = System.getProperty("user.dir");
		File directory = new File(rootDirectory);
    	File[] files = directory.listFiles();
    	tables = new HashMap<>();
		for(File file : files) {
    		if (file.getName().endsWith(".dbtd")) {
    			Table table = new Table(file);
    			tables.put(table.getName(), table);
    		}
		}
		addData1();
		addData2();
	}
	
	public void addData1() {
		byte[] data = new byte[98];
		for(int i = 0; i < data.length; i++) {
			byte b = data[i];
			if (b == 0) {
				data[i] = 32;
			}
		}
		System.arraycopy("P348XHT".getBytes(), 0, data, 0, "P348XHT".getBytes().length);
		System.arraycopy("Fiat".getBytes(), 0, data, 14, "Fiat".getBytes().length);
		System.arraycopy("Punto".getBytes(), 0, data, 46, "Punto".getBytes().length);
		System.arraycopy("Red".getBytes(), 0, data, 78, "Red".getBytes().length);
		tables.get("CAR").addRow(data);
	}
	
	public void addData2() {
		Car car = new Car();
		car.setRegistration("P237TFW");
		car.setManufacturer("Ford");
		car.setModel("Fiesta");
		car.setColour("Purple");
		tables.get("CAR").addRow(new ObjectParser(this).parse(car));
	}
	
	public static void main(String[] args) {
		new Database();
	}
	
	public Map<String, Table> getTables(){
		return tables;
	}
}
