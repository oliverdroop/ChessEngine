package database;

import java.lang.reflect.Type;
import java.util.Comparator;

public class RowComparator implements Comparator<byte[]>{
	
	private Table table;
	
	private Column column;
	
	private boolean ascending;
	
	public RowComparator(Table table, Column column, boolean ascending) {
		this.table = table;
		this.column = column;
		this.ascending = ascending;
	}

	@Override
	public int compare(byte[] row1, byte[] row2) {
		
		Object value1 = column.getDataType().getValue(table.getValueBytes(column, row1));
		Object value2 = column.getDataType().getValue(table.getValueBytes(column, row2));
		Comparable comp1 = (Comparable) value1;
		Comparable comp2 = (Comparable) value2;
		if (ascending) {
			return comp1.compareTo(comp2);
		} else {
			return comp2.compareTo(comp1);
		}
	}
}
