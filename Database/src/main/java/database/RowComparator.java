package database;

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
		if (ascending) {
			return table.getValueString(column, row1).compareTo(table.getValueString(column, row2));
		} else {
			return table.getValueString(column, row2).compareTo(table.getValueString(column, row1));
		}
	}
}
