package database;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public enum DataType {
	BOOLEAN(1, boolean.class), 
	BYTE(1, byte.class), 
	INT(4, int.class), 
	LONG(8, long.class), 
	DOUBLE(8, double.class), 
	VARCHAR(2, char.class);
	private final int length;
	private final Class type;
	
	DataType(int length, Class clazz){
		this.length = length;
		this.type = clazz;
	}
	
	public int getLength() {
		return length;
	}

	public <T> Class<T> getType() {
		return type;
	}
	
	public static byte[] getBytes(Object o) {
		byte[] out = new byte[0];
		if (o instanceof Boolean) {
			out = new byte[1];
			out[0] = 0;
			if ((boolean) o) {				
				out[0] = 1;
			}
			return out;
		}
		if (o instanceof Byte) {
			out = new byte[1];
			out[0] = (byte)o;
			return out;
		}
		if (o instanceof Integer) {
			out = new byte[4];
			out = ByteBuffer.allocate(4).putInt((int)o).array();
			return out;
		}
		if (o instanceof Long) {
			out = new byte[8];
			out = ByteBuffer.allocate(8).putLong((long)o).array();
			return out;
		}
		if (o instanceof Double) {
			out = new byte[8];
			out = ByteBuffer.allocate(8).putDouble((double)o).array();
			return out;
		}
		if (o instanceof String) {
			out = ((String) o).getBytes();
			return out;
		}
		return null;
	}
	
	public static Object getValue(byte[] fieldBytes, Column column) {
		if (column.getDataType() == DataType.BOOLEAN) {
			return (boolean) (fieldBytes[0] != 0);
		}
		if (column.getDataType() == DataType.BYTE) {
			return fieldBytes[0];
		}
		if (column.getDataType() == DataType.INT) {
		    return ByteBuffer.wrap(fieldBytes).getInt();
		}
		if (column.getDataType() == DataType.LONG) {
		    return ByteBuffer.wrap(fieldBytes).getLong();
		}
		if (column.getDataType() == DataType.DOUBLE) {
		    return ByteBuffer.wrap(fieldBytes).getDouble();
		}
		if (column.getDataType() == DataType.VARCHAR) {
			return new String(fieldBytes, StandardCharsets.UTF_8);
		}
		return null;
	}
}
