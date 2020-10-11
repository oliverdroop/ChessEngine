package database;

public class Column {
	private String name;
	private DataType dataType;
	private int count;
	
	public Column(String[] parameterStrings) {
		if (parameterStrings.length == 3) {
			this.name = parameterStrings[0];
			this.dataType = DataType.valueOf(parameterStrings[1]);
			this.count = Integer.parseInt(parameterStrings[2]);
		}
	}
	
	public Column(String name, String dataTypestring, String countString) {
		this.name = name;
		this.dataType = DataType.valueOf(dataTypestring);
		this.count = Integer.parseInt(countString);
	}
	
	public Column(String name, DataType dataType, int count) {
		this.name = name;
		this.dataType = dataType;
		this.count = count;
	}
	
	public int getLength() {
		return dataType.getLength() * count;
	}
	
	public String getName() {
		return name;
	}
	
	public DataType getDataType() {
		return dataType;
	}
}
