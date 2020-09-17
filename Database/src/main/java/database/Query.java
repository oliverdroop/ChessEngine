package database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import database.SQLPhrase.KeywordType;
import database.SQLPhrase.PhraseType;
import javafx.util.Pair;

public class Query {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Query.class);
	
	private Database database;
	
	private Table table;
	
	private SQLPhrase instruction;
	
	private Map<String, Pair<Operator, String>> conditions;
	
	private Map<String, String> assignments;
	
	private List<String> targets;
	
	private SQLPhrase joinType;
	
	private List<SQLPhrase> joinCondition;
	
	private LinkedHashMap<String, Boolean> orderBy;
	
	public Query(QueryParameters parameters) {
		database = parameters.getDatabase();
		instruction = parameters.getInstruction();
		table = parameters.getTable();
		conditions = parameters.getConditions();
		assignments = parameters.getAssignments();
		targets = parameters.getTargets();
		joinType = parameters.getJoinType();
		joinCondition = parameters.getJoinCondition();
		orderBy = parameters.getOrderBy();
	}
	
	private Column getColumn(String columnName) {
		if (table == null) {
			LOGGER.warn("Cannot look for columns in null table");
			return null;
		}
		Column column = table.getColumns().get(columnName);
		if (column == null) {
			LOGGER.warn("Cannot find column {} in table {}", columnName, table.getName());
		}
		return column;
	}
	
	public List<String> execute() {
		List<String> output = new ArrayList<>();
		
		if (joinType != null && joinCondition.size() == 2) {
			table = getJoinTable();
			targets = getTargetColumns(targets, table).stream().map(c -> c.getName()).collect(Collectors.toList());
		}
		
		if (table != null && instruction != null) {
			
			if (instruction.getString().equals("INSERT")) {
				int rowsAdded = table.addRow(table.buildRow(assignments));
				output.add(String.format("Inserted %d row into table %s", rowsAdded, table.getName()));
			}
			
			if (instruction.getString().equals("SELECT")) {
				List<byte[]> rows = null;
				if (conditions.isEmpty()) {
					rows = Arrays.asList(table.getAllRows());
				}
				else if (!conditions.keySet().stream()
						.filter(columnName -> table.getColumns().get(columnName) != null)
						.collect(Collectors.toList())
						.isEmpty()){
					rows = table.getStringMatchedRows(conditions);
				}
				if (orderBy != null && !orderBy.isEmpty()) {
					for (int i = orderBy.size() - 1; i >= 0; i--) {
						String columnName = (String)orderBy.keySet().toArray()[i];
						Column column = table.getColumns().get(columnName);
						if (column == null) {
							LOGGER.warn("Could not sort result set by {} : Column doesn't exist in table {}",columnName , table.getName());
							continue;
						}
						rows = table.sortRows(column, orderBy.get(columnName), rows);
					}
				}
				if (targets != null && !targets.isEmpty() && rows != null) {
					return getResultsWithHeader(rows.stream()
							.map(row -> getValuesString(table, getTargetColumns(targets, table), row))
							.collect(Collectors.toList()));
				} else {
					LOGGER.warn("Invalid SQL");
				}
			}
			
			if (instruction.getString().equals("UPDATE")) {
				if (!assignments.isEmpty()) {
					int rowsUpdated = 0;
					if (!conditions.isEmpty()) {
						rowsUpdated = table.updateStringMatchedRows(conditions, assignments);
					}
					else {
						
						rowsUpdated = table.updateAllRows(assignments);
					}
					output.add(String.format("Updated %d rows in table %s", rowsUpdated, table.getName()));
				}
			}
			
			if (instruction.getString().equals("DELETE")) {
				int count = 0;
				if (conditions.isEmpty()) {
					count = table.deleteAllRows();
				}
				else {
					count = table.deleteStringMatchedRows(conditions);
				}
				String message = String.format("Deleted %d rows from table %s", count, table.getName());
				output.add(message);
			}
			
		}
		return output;
	}
	
	private List<String> getResultsWithHeader(List<String> results){
		if (targets != null && !targets.isEmpty()) {
			StringBuilder header = new StringBuilder();
			for(int index = 0; index < targets.size(); index++) {
				header.append(targets.get(index));
				if (index < targets.size() - 1) {
					header.append("\t");
				}
			}
			results.add(0, header.toString());
		}
		return results;
	}
	
	private Table getJoinTable() {
		int indexPrimary = 0;
		int indexSecondary = 1;
		if (joinType.getString().equals("RIGHT JOIN")) {
			indexPrimary = 1;
			indexSecondary = 0;
		}
		String columnPrimaryString = joinCondition.get(indexPrimary).getString();
		String tablePrimaryString = joinCondition.get(indexPrimary).getLinkedTable().getString();
		String columnSecondaryString = joinCondition.get(indexSecondary).getString();
		String tableSecondaryString = joinCondition.get(indexSecondary).getLinkedTable().getString();
		Table tablePrimary = database.getTables().get(tablePrimaryString);
		Column columnPrimary = tablePrimary.getColumns().get(columnPrimaryString);
		Table tableSecondary = database.getTables().get(tableSecondaryString);
		
		Table tableLeft;
		Table tableRight;
		if (joinType.getString().equals("RIGHT JOIN")) {
			tableLeft = tableSecondary;
			tableRight = tablePrimary;
		} else {
			tableLeft = tablePrimary;
			tableRight = tableSecondary;
		}
		String joinTableName = tableLeft.getName() + "_" + tableRight.getName();
		Map<String,Column> columns = getColumnsFromMultipleTables(tableLeft, tableRight);
		
		Collection<Column> columnSet = columns.values();
		Table joinTable = new Table(joinTableName, columnSet);
		joinTable.setData(getJoinTableData(tablePrimary, tableSecondary, columnPrimary, columnSecondaryString));
		
		return joinTable;
	}
	
	private byte[] getJoinTableData(Table tablePrimary, Table tableSecondary, Column columnPrimary, String columnSecondaryName) {
		List<byte[]> rowsSecondaryJoined = null;
		if (joinType.getString().equals("FULL JOIN")) {
			rowsSecondaryJoined = new ArrayList<>();
		}
		
		byte[] data = new byte[(tablePrimary.getRowLength() + tableSecondary.getRowLength()) * (tablePrimary.countRows() + tableSecondary.countRows())];
		
		Map<String, Pair<Operator, String>> propertyStringMap = new HashMap<>();
		int dataIndex = 0;
		for(int i = 0; i < tablePrimary.getData().length; i += tablePrimary.getRowLength()) {
			byte[] rowPrimary = Arrays.copyOfRange(tablePrimary.getData(), i, i + tablePrimary.getRowLength());
			String joinValueString = tablePrimary.getValueString(columnPrimary, rowPrimary);
			propertyStringMap.put(columnSecondaryName, Operator.EQUAL.pairWith(joinValueString));
			List<byte[]> rowsSecondary = tableSecondary.getStringMatchedRows(propertyStringMap);
			
			if (rowsSecondary.size() == 0) {
				if (joinType.getString().equals("INNER JOIN")) {
					continue;
				} else {
					rowsSecondary.add(new byte[tableSecondary.getRowLength()]);
				}
			}
			
			for(byte[] rowSecondary : rowsSecondary) {
				if (rowsSecondaryJoined != null) {
					rowsSecondaryJoined.add(rowSecondary);
				}

				if (joinType.getString().equals("RIGHT JOIN")) {
					System.arraycopy(rowSecondary, 0, data, dataIndex, tableSecondary.getRowLength());
					System.arraycopy(rowPrimary, 0, data, dataIndex + tableSecondary.getRowLength(), tablePrimary.getRowLength());
				} else {
					System.arraycopy(rowPrimary, 0, data, dataIndex, tablePrimary.getRowLength());
					System.arraycopy(rowSecondary, 0, data, dataIndex + tablePrimary.getRowLength(), tableSecondary.getRowLength());
				}
				dataIndex += tablePrimary.getRowLength() + tableSecondary.getRowLength();
			}
		}
		
		if (rowsSecondaryJoined != null) {
			byte[] rowPrimary = new byte[tablePrimary.getRowLength()];
			for(byte[] rowSecondary : tableSecondary.getAllRows()) {
				boolean rowJoined = false;
				for(byte[] rowSecondaryJoined : rowsSecondaryJoined) {
					if (Arrays.equals(rowSecondary, rowSecondaryJoined)) {
						rowJoined = true;
						break;
					}
				}
				if (!rowJoined) {
					System.arraycopy(rowPrimary, 0, data, dataIndex, tablePrimary.getRowLength());
					System.arraycopy(rowSecondary, 0, data, dataIndex + tablePrimary.getRowLength(), tableSecondary.getRowLength());
					dataIndex += tablePrimary.getRowLength() + tableSecondary.getRowLength();
				}
			}
		}
		byte[] tableData = new byte[dataIndex];
		System.arraycopy(data, 0, tableData, 0, dataIndex);
		return tableData;
	}
	
	private Map<String,Column> getColumnsFromMultipleTables(Table tableLeft, Table tableRight) {
		Map<String, Column> columns = new LinkedHashMap<>();
		Map<String, Column> columnsLeft = getUniqueAndRenamedColumns(tableLeft, tableRight);
		Map<String, Column> columnsRight = getUniqueAndRenamedColumns(tableRight, tableLeft);
		columnsLeft.keySet().forEach(key -> columns.put(key, columnsLeft.get(key)));
		columnsRight.keySet().forEach(key -> columns.put(key, columnsRight.get(key)));
		
		for(String columnName : columns.keySet()) {
			Column originalColumn = columns.get(columnName);
			DataType dataType = originalColumn.getDataType();
			int dataCount = originalColumn.getLength() / dataType.getLength();
			Column newColumn = new Column(columnName, columns.get(columnName).getDataType(), dataCount);
			columns.put(columnName, newColumn);
		}
		return columns;
	}
	
	private Map<String,Column> getUniqueAndRenamedColumns(Table tableSource, Table tableReference) {
		Map<String, Column> output = new LinkedHashMap<>();
		for(String columnName : tableSource.getColumns().keySet()) {
			String finalColumnName = columnName;
			if (tableReference.getColumns().keySet().contains(columnName)) {
				finalColumnName = tableSource.getName() + "." + columnName;
			}
			output.put(finalColumnName, tableSource.getColumns().get(columnName));
		}
		return output;
	}
	
	private List<Column> getTargetColumns(List<String> targets, Table table){
		List<Column> targetColumns = null;
		for(String target : targets) {
			if (table.getColumns().keySet().contains(target)) {
				if (targetColumns == null) {
					targetColumns = new ArrayList<>();
				}
				targetColumns.add(table.getColumns().get(target));
			}
		}
		return targetColumns;
	}
	
	private String getValuesString(Table table, List<Column> columns, byte[] row) {
		String rowString = null;
		if (columns != null && columns.size() > 0) {
			rowString = table.getValuesString(columns, row);
		} else {
			rowString = table.getRowString(row);
		}
		return rowString;
	}
}
