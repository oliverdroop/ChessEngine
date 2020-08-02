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
		Comparable<Object> comp1 = (Comparable<Object>) column.getDataType().getValue(table.getValueBytes(column, row1));
		Comparable<Object> comp2 = (Comparable<Object>) column.getDataType().getValue(table.getValueBytes(column, row2));
		if (ascending) {
			return comp1.compareTo(comp2);
		} else {
			return comp2.compareTo(comp1);
		}
	}
}
