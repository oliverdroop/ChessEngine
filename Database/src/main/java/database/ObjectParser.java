package database;

import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectParser {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ObjectParser.class);
	
	private Database database;
	
	public ObjectParser(Database database) {
		this.database = database;
	}
	
	public byte[] parse(Object o) {
		try {
			Table table = getApplicableTable(o);
			int index = 0;
			byte[] rowBytes = new byte[table.getRowLength()];
			for(Column column : table.getColumns().values()) {
				byte[] fieldBytes = new byte[column.getLength()];
				String fieldName = getCamelCase(column.getName());
				String getterPrefix = column.getDataType() == DataType.BOOLEAN ? "is" : "get";
				String getterName = getterPrefix + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
				Object fieldValue = o.getClass().getMethod(getterName).invoke(o);
				byte[] objectFieldBytes = DataType.getBytes(fieldValue);
				System.arraycopy(objectFieldBytes, 0, fieldBytes, 0, objectFieldBytes.length);
				System.arraycopy(fieldBytes, 0, rowBytes, index, column.getLength());
				index += column.getLength();
			}	
			return rowBytes;
		}
		catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			LOGGER.warn(e.getMessage());
		}
		return null;
	}
	
	public Object unparse(byte[] row, Table table) {
		try {
			String tableClassName = null;
			if (table.getClassName() != null) {
				tableClassName = table.getClassName();
			}
			else {
				tableClassName = table.getName().substring(0, 1).toUpperCase() + table.getName().substring(1).toLowerCase();
			}
			Class tableClass = Class.forName(tableClassName);
			Object o = tableClass.cast(tableClass.newInstance());
			for(Column column : table.getColumns().values()) {
				String fieldName = getCamelCase(column.getName());
				String setterName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
				byte[] fieldBytes = new byte[column.getLength()];
				System.arraycopy(row, table.getIndexInRow(column), fieldBytes, 0, column.getLength());
				Object value = column.getDataType().getValue(fieldBytes);
				Class parameterClass = column.getDataType().getType();
				o.getClass().getMethod(setterName, parameterClass).invoke(o, value);
			}
			return o;
		}
		catch(ClassNotFoundException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | InstantiationException e) {
			e.printStackTrace();
			LOGGER.warn(e.getMessage());
		}
		return null;
	}
	
	public Table getApplicableTable(Object o) {
		return database.getTables().get(o.getClass().getSimpleName().toUpperCase());
	}
	
	private String getCamelCase(String capitalized) {
		String[] parts = capitalized.split("_");
		String output = "";
		for(String s : parts) {
			output += s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
		}
		return output.substring(0, 1).toLowerCase() + output.substring(1);
	}
	
	private String getCapitalizedString(String camelCase) {
		String output = "";
		for(String c : camelCase.split("(?!^)")) {
			if (c == c.toLowerCase()) {
				output += c.toUpperCase();
			}
			else {
				output += "_" + c;
			}
		}
		return output;
	}
}
