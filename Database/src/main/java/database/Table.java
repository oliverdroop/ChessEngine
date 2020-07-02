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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Table {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Table.class);
	private File schemaFile;
	private String name;
	private String className;
	private String databaseName;
	private Map<String, Column> columns;
	private String primaryKey;
	private boolean autoGenerateKey = false;
	private byte[] lastGeneratedKey;
	private int rowLength = 0;
	private byte[] data = new byte[0];
	
	public Table(File schemaFile) {
		try {
			FileReader fileReader = new FileReader(schemaFile.getPath());
			BufferedReader br = new BufferedReader(fileReader);
			List<String> lines = new ArrayList<>();
			while(br.ready() || lines.size() == 0) {
				lines.add(br.readLine());
			}
			br.close();
			Map<String, Column> columns = new LinkedHashMap<>();
			for(String line : lines) {
				if (line.startsWith("DATABASE=")) {
					databaseName = line.replace("DATABASE=", "");
				}
				if (line.startsWith("TABLE=")) {
					name = line.replace("TABLE=", "");
				}
				if (line.startsWith("PRIMARY_KEY=")) {
					primaryKey = line.replace("PRIMARY_KEY=", "");
				}
				if (line.startsWith("AUTO_GENERATE_KEY=")) {
					autoGenerateKey = Boolean.parseBoolean(line.replace("AUTO_GENERATE_KEY=", ""));
				}
				if (line.startsWith("COLUMN=")) {
					line = line.replace("COLUMN=", "");
					String[] columnParamaters = line.split(",");
					Column column = new Column(columnParamaters);
					columns.put(column.getName(), column);
				}
			}
			this.columns = columns;
			if (primaryKey != null && columns.get(primaryKey) == null) {
				LOGGER.warn("No column {} exists in table {} : Unable to set idColumn", primaryKey, name);
				primaryKey = null;
			}
			
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
	
	public List<byte[]> getByteMatchedRows(Map<String, byte[]> propertyValueMap) {
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
	
	public List<byte[]> getStringMatchedRows(Map<String, String> propertyStringMap) {
		return getByteMatchedRows(getPropertyValueMap(propertyStringMap));
	}
	
	public int getRowIndexById(Object id) {
		if (primaryKey == null) {
			LOGGER.warn("Unable to get index by id : No idColumn specified for table {}", name);
			return -1;
		}
		Column column = columns.get(primaryKey);
		int rows = countRows();
		int columnLength = column.getLength();
		int columnPosition = getIndexInRow(column);
		byte[] idBytes = DataType.getBytes(id);
		
		for(int i = 0; i < rows; i++) {
			byte[] fieldValue = new byte[columnLength];
			System.arraycopy(data, (i * getRowLength()) + columnPosition, fieldValue, 0, columnLength);
			if (Arrays.equals(fieldValue, idBytes)) {
				return i;
			}
		}
		DataType dataType = columns.get(primaryKey).getDataType();
		LOGGER.info("Unable to get index by id {} in table {} : No match found", dataType.getValue(DataType.getBytes(id)), name);
		return -1;
	}
	
	public byte[] getRowById(Object id) {
		return getRowByIndex(getRowIndexById(id));
	}
	
	public byte[] getRowByIndex(int index) {
		byte[] row = new byte[getRowLength()];
		System.arraycopy(data, getRowLength() * index, row, 0, getRowLength());
		return row;
	}
	
	private byte[] getColumnBytes(Column column){
		int rows = countRows();
		int columnLength = column.getLength();
		int outputLength = columnLength * rows;
		byte[] output = new byte[outputLength];
		int columnPosition = getIndexInRow(column);
		for(int i = 0; i < rows; i++) {
			System.arraycopy(data, (i * getRowLength()) + columnPosition, output, i * columnLength, columnLength);
		}
		return output;
	}
	
	private byte[] getValueBytes(Column column, byte[] row) {
		byte[] value = new byte[column.getLength()];
		System.arraycopy(row, getIndexInRow(column), value, 0, column.getLength());
		return value;
	}
	
	private byte[] getValueBytes(String columnName, byte[] row) {
		return getValueBytes(columns.get(columnName), row);
	}
	
	public byte[][] getAllRows(){
		int rowCount = data.length / getRowLength();
		byte[][] output = new byte[rowCount][getRowLength()];
		for(int i = 0; i < rowCount; i++) {
			System.arraycopy(data, i * getRowLength(), output[i], 0, getRowLength());
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
	
	private boolean isAddableRow(byte[] row) {
		if (row.length != getRowLength()) {
			return false;
		}
		byte[] id = null;
		if (primaryKey != null) {
			id = getValueBytes(primaryKey, row);
		}
		if (id != null) {
			if (getRowIndexById(id) != -1) {
				if (autoGenerateKey) {
					byte[] key = generateKey();
					Column primaryKeyColumn = columns.get(primaryKey);
					System.arraycopy(key, 0, row, getIndexInRow(primaryKeyColumn), primaryKeyColumn.getLength());
				}
				else {
					LOGGER.warn("Unable to add row to table {} : id {} exists", name, columns.get(primaryKey).getDataType().getValue(id));
					return false;
				}
			}
		}
		return true;
	}
	
	public void addRow(byte[] row) {
		if (data == null) {
			data = new byte[0];
		}
		if (isAddableRow(row)) {
			byte[] newData = new byte[data.length + row.length];
			System.arraycopy(data, 0, newData, 0, data.length);
			System.arraycopy(row, 0, newData, data.length, row.length);
			data = newData;
			LOGGER.info("Added data row. New length: {}", data.length);
			LOGGER.info(getRowString(row));
		}
	}
	
	public void deleteRow(byte[] id) {
		if (primaryKey == null) {
			LOGGER.warn("Unable to delete row without primary key for table {}", name);
			return;
		}
		int start = getRowIndexById(id) * getRowLength();
		int end = start + getRowLength();
		int newLength = data.length - getRowLength();
		byte[] newData = new byte[newLength];
		if (start > 0) {
			System.arraycopy(data, 0, newData, 0, start);
		}
		if (end <= newLength) {
			System.arraycopy(data, end, newData, start, newLength - start);
		}
		data = newData;
		LOGGER.info("Deleted row with id {} from table {}", columns.get(primaryKey).getDataType().getValueString(id), name);
	}
	
	public void deleteByteMatchedRows(Map<String,byte[]> propertyValueMap) {
		if (primaryKey == null) {
			LOGGER.warn("Unable to delete byte matched rows without primary key for table {}", name);
			return;
		}
		for(byte[] row : getByteMatchedRows(propertyValueMap)) {
			byte[] id = getValueBytes(primaryKey, row);
			deleteRow(id);
		}
	}
	
	public void deleteStringMatchedRows(Map<String,String> propertyStringMap) {
		deleteByteMatchedRows(getPropertyValueMap(propertyStringMap));
	}
	
	public void updateRowValues(byte[] id, Map<String,byte[]> propertyValueMap) {
		if (primaryKey == null) {
			LOGGER.warn("Unable to update rows without primary key for table {}", name);
			return;
		}
		int rowIndex = getRowIndexById(id) * getRowLength();
		for(String key : propertyValueMap.keySet()) {
			if (!columns.containsKey(key)) {
				LOGGER.warn("Unable to set property {} of row {} in table {} : No matching column found", key, columns.get("id").getDataType().getValue(id), name);
			}
			int propertyIndex = getIndexInRow(key);
			int propertyLength = columns.get(key).getLength();
			byte[] newProperty = new byte[propertyLength];
			System.arraycopy(propertyValueMap.get(key), 0, newProperty, 0, propertyValueMap.get(key).length);
			System.arraycopy(newProperty, 0, data, rowIndex + propertyIndex, propertyLength);
			LOGGER.info("Successfully updated property {} of row {} in table {}", key, columns.get("id").getDataType().getValue(id), name);
		}
	}
	
	public void updateRowStrings(byte[] id, Map<String,String> propertyStringMap) {
		updateRowValues(id, getPropertyValueMap(propertyStringMap));
	}
	
	public void updateByteMatchedRows(Map<String,byte[]> searchMap, Map<String,byte[]> replacementMap){
		if (primaryKey == null) {
			LOGGER.warn("Unable to update rows without primary key for table {}", name);
			return;
		}
		for(byte[] row : getByteMatchedRows(searchMap)) {
			byte[] id = getValueBytes(primaryKey, row);
			updateRowValues(id, replacementMap);
		}
	}
	
	public void updateStringMatchedRows(Map<String,String> searchMap, Map<String,String> replacementMap){
		updateByteMatchedRows(getPropertyValueMap(searchMap), getPropertyValueMap(replacementMap));
	}
	
	private byte[] generateKey() {
		if (primaryKey == null) {
			return null;
		}
		byte[] output = null;
		if (lastGeneratedKey != null) {
			byte[] lastGeneratedKeyBytes = DataType.getBytes(lastGeneratedKey);
			output = DataType.increment(lastGeneratedKeyBytes);
		}
		else {
			output = new byte[columns.get(primaryKey).getLength()];
		}
		lastGeneratedKey = output;
		if(getRowIndexById(output) != -1) {
			output = generateKey();
		}
		return output;
	}
	
	public int countRows() {
		return data.length / getRowLength();
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
	
	public String getRowString(byte[] row) {
		StringBuilder rowString = new StringBuilder();
		int count = 0;
		for(Column column : columns.values()) {
			rowString.append(column.getDataType().getValueString(getValueBytes(column, row)));
			count += 1;
			if (count < columns.keySet().size()) {
				rowString.append("\t");
			}
		}
		return rowString.toString();
	}
	
	private Map<String,byte[]> getPropertyValueMap(Map<String, String> propertyStringMap){
		Map<String, byte[]> propertyValueMap = new HashMap<>();
		for(String fieldName : propertyStringMap.keySet()) {
			Column column = columns.get(fieldName);
			byte[] bytes = column.getDataType().getBytes(propertyStringMap.get(fieldName));
			propertyValueMap.put(fieldName, bytes);
		}
		return propertyValueMap;
	}
	
	public void setLastGeneratedKey(byte[] lastGeneratedKey) {
		this.lastGeneratedKey = lastGeneratedKey;
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

	public boolean isAutoGenerateKey() {
		return autoGenerateKey;
	}

	public void setAutoGenerateKey(boolean autoGenerateKey) {
		this.autoGenerateKey = autoGenerateKey;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}
}
