package database;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Table {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Table.class);
	private File schemaFile;
	private String name;
	private String databaseName;
	private Map<String, Column> columns;
	private Column id;
	private int rowLength = 0;
	private byte[] data;
	
	public Table(File schemaFile) {
		try {
			FileReader fileReader = new FileReader(schemaFile.getPath());
			BufferedReader br = new BufferedReader(fileReader);
			List<String> lines = new ArrayList<>();
			while(br.ready() || lines.size() == 0) {
				lines.add(br.readLine());
			}
			Map<String, Column> columns = new LinkedHashMap<>();
			for(String line : lines) {
				if (line.startsWith("DATABASE=")) {
					databaseName = line.replace("DATABASE=", "");
				}
				if (line.startsWith("TABLE=")) {
					name = line.replace("TABLE=", "");
				}
				if (line.startsWith("COLUMN=")) {
					line = line.replace("COLUMN=", "");
					String[] columnParamaters = line.split(",");
					Column column = new Column(columnParamaters);
					columns.put(column.getName(), column);
				}
			}
			this.columns = columns;
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Table(String databaseName, String name, Column... columns) {
		this.databaseName = databaseName;
		this.name = name;
		this.columns = new LinkedHashMap<>();
		for(Column c : columns) {
			this.columns.put(c.getName(), c);
		}
	}
	
	public int getRowLength() {
		if (rowLength == 0) {
			columns.values().forEach(c -> rowLength += c.getLength());
		}
		return rowLength;
	}
	
	public List<byte[]> getRows(Map<String, byte[]> propertyValueMap) {
		for(String fieldName : propertyValueMap.keySet()) {
			Column column = columns.get(fieldName);
			byte[] value = propertyValueMap.get(fieldName);
			if (value.length < column.getLength() && column.getDataType() == DataType.VARCHAR) {
				byte[] valueWithWhitespace = new byte[column.getLength()];
				System.arraycopy(value, 0, valueWithWhitespace, 0, value.length);
				propertyValueMap.put(fieldName, valueWithWhitespace);
			}
		}
		
		List<byte[]> matches = new ArrayList<>();
		for(byte[] row : getAllRows()) {
			boolean match = true;
			for(String columnName : propertyValueMap.keySet()) {
				if (!match) {
					continue;
				}
				if (!Arrays.equals(getValueBytes(columnName, row), propertyValueMap.get(columnName))) {
					match = false;
				}
			}
			if (match) {
				matches.add(row);
			}
		}
		return matches;
	}
	
	private byte[] getValueBytes(Column column, byte[] row) {
		byte[] value = new byte[column.getLength()];
		System.arraycopy(row, getIndexInRow(column), value, 0, column.getLength());
		return value;
	}
	
	private byte[] getValueBytes(String columnName, byte[] row) {
		return getValueBytes(columns.get(columnName), row);
	}
	
	private List<byte[]> getAllRows(){
		int rowCount = data.length / getRowLength();
		List<byte[]> output = new ArrayList<>(rowCount);
		for(int i = 0; i < rowCount; i++) {
			byte[] row = new byte[getRowLength()];
			System.arraycopy(data, i * getRowLength(), row, 0, getRowLength());
			output.add(row); 
		}
		return output;
	}
	
	public int getIndexInRow(Column column) {
		return getIndexInRow(column.getName());
	}
	
	public int getIndexInRow(String columnName) {
		int index = 0;
		boolean indexFound = false;
		for(String key : columns.keySet()) {
			if(indexFound) {
				continue;
			}
			if(key.equals(columnName)) {
				indexFound = true;				
			}
			else {
				index += columns.get(key).getLength();
			}
		}
		return index;
	}
	
	public void addRow(byte[] row) {
		if (data == null) {
			data = new byte[0];
		}
		if (row.length == getRowLength()) {
			byte[] newData = new byte[data.length + row.length];
			System.arraycopy(data, 0, newData, 0, data.length);
			System.arraycopy(row, 0, newData, data.length, row.length);
			data = newData;
			LOGGER.info("Added data row. New length: {}", data.length);
			StringBuilder dataString = new StringBuilder();
			for(Column column : columns.values()) {
				dataString.append(column.getDataType().getValue(getValueBytes(column, row), column).toString());
				//dataString.append(getValue(column, row).toString());
				dataString.append("\t");
			}
			LOGGER.info(dataString.toString());
		}
	}
	
	public void save(String path) {
		try {
			File file = new File(path);
    		Path actualPath = file.toPath();
			Files.write(actualPath, data);
		}
		catch(IOException e) {
    		e.printStackTrace();
    		LOGGER.warn(e.getMessage());
    	}
	}
	
	public void load(String path) {
		try {
    		File file = new File(path);
    		Path actualPath = file.toPath();
    		data = Files.readAllBytes(actualPath);
    	}
    	catch(IOException e) {
    		e.printStackTrace();
    		LOGGER.warn(e.getMessage());
    	}
	}
	
	public String getName() {
		return this.name;
	}
	
	public Map<String, Column> getColumns(){
		return this.columns;
	}

	public byte[] getData() {
		return data;
	}
	
	public void setData(byte[] data) {
		this.data = data;
	}
	
}
