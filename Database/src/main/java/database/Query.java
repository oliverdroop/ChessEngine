package database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import database.SQLPhrase.KeywordType;
import database.SQLPhrase.PhraseType;

public class Query {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Query.class);
	
	private List<SQLPhrase> sqlStatement;
	
	private Database database;
	
	private Table table;
	
	private SQLPhrase instruction;
	
	private Map<String, String> conditions;
	
	private Map<String, String> assignments;
	
	private List<String> targets;
	
	private String joinType;
	
	private List<SQLPhrase> joinCondition;
	
	public Query(List<SQLPhrase> sqlStatement, Database database) {
		this.sqlStatement = sqlStatement;
		this.database = database;
		instruction = extractInstruction();
		table = extractTable();
		conditions = extractConditions();
		assignments = extractAssignments();
		joinType = extractJoinType();
		joinCondition = extractJoinCondition();
	}
	
	private SQLPhrase extractInstruction() {
		return sqlStatement.get(0).hasKeywordType(KeywordType.INSTRUCTION) ? sqlStatement.get(0) : null;
	}
	
	private Table extractTable() {
		for(int i = 0; i < sqlStatement.size(); i++) {
			SQLPhrase phrase = sqlStatement.get(i);
			if (phrase.getType() == PhraseType.TABLE_NAME) {
				return database.getTables().get(phrase.getString());
			}
		}
		return null;
	}
	
	private Map<String,String> extractConditions(){
		int startIndex = sqlStatement.size();
		for (int i = 0; i < sqlStatement.size(); i++) {
			if (sqlStatement.get(i).hasKeywordType(KeywordType.EXPRESSION)) {
				startIndex = i;
				break;
			}
		}
		Map<String, String> output = new LinkedHashMap<>();
		for(int i = startIndex; i < sqlStatement.size(); i++) {
			SQLPhrase phrase = sqlStatement.get(i);
			SQLPhrase linkedPhrase = phrase.getLinkedColumn();
			if (linkedPhrase != null && linkedPhrase.hasType(PhraseType.COLUMN_NAME) && phrase.hasType(PhraseType.VALUE)) {
				output.put(linkedPhrase.getString(), phrase.getString());
			}
		}
		return output;
	}
	
	private Map<String, String> extractAssignments(){
		Map<String, String> output = new LinkedHashMap<>();
		for (int i = 0; i < sqlStatement.size(); i++) {
			SQLPhrase phrase = sqlStatement.get(i);
			SQLPhrase previousKeyword = SQLInterpreter.getPreviousKeyword(phrase, sqlStatement);
			if (previousKeyword != null 
					&& previousKeyword.hasKeywordType(KeywordType.INSTRUCTION) 
					&& phrase.getLinkedValue() != null 
					&& phrase.hasType(PhraseType.COLUMN_NAME) 
					&& phrase.getLinkedValue().hasType(PhraseType.VALUE)) {
				output.put(phrase.getString(), phrase.getLinkedValue().getString());
			}
		}
		List<SQLPhrase> unlinkedColumns = new ArrayList<>();
		List<SQLPhrase> unlinkedValues = new ArrayList<>();
		for (int i = 0; i < sqlStatement.size(); i++) {
			SQLPhrase phrase = sqlStatement.get(i);
			if (phrase.getLinkedValue() == null && phrase.hasType(PhraseType.COLUMN_NAME)) {
				unlinkedColumns.add(phrase);
			} else if (phrase.getLinkedColumn() == null && phrase.hasType(PhraseType.VALUE)) {
				unlinkedValues.add(phrase);
			}
		}
		if (unlinkedColumns.size() > 0) {
			if (unlinkedColumns.size() == unlinkedValues.size()) {
				for(int i = 0; i < unlinkedColumns.size(); i++) {
					output.put(unlinkedColumns.get(i).getString(), unlinkedValues.get(i).getString());
				}
			}
			else {
				if (unlinkedValues.size() == 0) {
					if (unlinkedColumns.size() == 1 && unlinkedColumns.get(0).getString().equals("*") && table != null) {
						targets = table.getColumns().keySet().stream().collect(Collectors.toList());
					}
					else {
						targets = unlinkedColumns
								.stream()
								.filter(p -> p.getLinkedTable() == null || database.getTables().get(p.getLinkedTable().getString()).equals(table))
								.filter(p -> SQLInterpreter.getPreviousKeyword(p, sqlStatement).hasKeywordType(KeywordType.INSTRUCTION))
								.map(p -> p.getString())
								.collect(Collectors.toList());
					}
				}
			}
		}
		else if(unlinkedValues.size() == table.getColumns().size()){
			List<String> keyList = table.getColumns().keySet().stream().collect(Collectors.toList());
			for(int i = 0; i < unlinkedValues.size(); i++) {
				output.put(keyList.get(i), unlinkedValues.get(i).getString());
			}
		}
		return output;
	}
	
	private String extractJoinType() {
		for(SQLPhrase phrase : sqlStatement) {
			if (phrase.hasKeywordType(KeywordType.JOIN)) {
				return phrase.getString();
			}
		}
		return null;
	}
	
	private List<SQLPhrase> extractJoinCondition(){
		List<SQLPhrase> output = new ArrayList<>();
		for(int i = 0; i < sqlStatement.size(); i++) {
			SQLPhrase phrase = sqlStatement.get(i);
			if (phrase.hasKeywordType(KeywordType.JOIN)) {
				if (sqlStatement.get(i + 1).hasKeywordType(KeywordType.TABLE_IDENTIFIER)
						&& sqlStatement.get(i + 2).hasType(PhraseType.TABLE_NAME)
						&& sqlStatement.get(i + 3).hasType(PhraseType.COLUMN_NAME)
						&& sqlStatement.get(i + 4).hasType(PhraseType.TABLE_NAME)
						&& sqlStatement.get(i + 5).hasType(PhraseType.COLUMN_NAME)) {
					output.add(sqlStatement.get(i + 3));
					output.add(sqlStatement.get(i + 5));
				}
			}
		}
		return output;
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
			return executeJoin();
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
				else {
					rows = table.getStringMatchedRows(conditions);
				}
				if (!targets.isEmpty()) {
					return rows.stream().map(row -> getValuesString(table, getTargetColumns(targets, table), row)).collect(Collectors.toList());
					//return getColumnsFromRows(targets, rows);
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
	
	private List<String> executeJoin() {
		String columnLeftString = joinCondition.get(0).getString();
		String tableLeftString = joinCondition.get(0).getLinkedTable().getString();
		String columnRightString = joinCondition.get(1).getString();
		String tableRightString = joinCondition.get(1).getLinkedTable().getString();
		Table tableLeft = database.getTables().get(tableLeftString);
		Column columnLeft = tableLeft.getColumns().get(columnLeftString);
		Table tableRight = database.getTables().get(tableRightString);
		Column columnRight = tableRight.getColumns().get(columnRightString);
		
		List<Column> targetColumnsLeft = null;
		List<Column> targetColumnsRight = null;
		if (targets != null) {
			targetColumnsLeft = this.getTargetColumns(targets, tableLeft);
			targetColumnsRight = this.getTargetColumns(targets, tableRight);
		}
		
		List<String> output = new ArrayList<>();
		
		if (joinType.equals("LEFT JOIN")) {
			Map<String, String> propertyStringMap = new HashMap<>();
			for(byte[] rowLeft : tableLeft.getAllRows()) {
				String joinValueString = tableLeft.getValueString(columnLeft, rowLeft);
				propertyStringMap.put(columnRightString, joinValueString);
				List<byte[]> rowsRight = tableRight.getStringMatchedRows(propertyStringMap);
				
				if (rowsRight.size() == 0) {
					rowsRight.add(new byte[tableRight.getRowLength()]);
				}
				
				String rowStringLeft = getValuesString(tableLeft, targetColumnsLeft, rowLeft);
				
				for(byte[] rowRight : rowsRight) {
					String rowStringRight = getValuesString(tableRight, targetColumnsRight, rowRight);
					output.add(rowStringLeft + "\t" + rowStringRight);
				}
			}
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
	
	public List<String> getaColumnsFromRows(List<String> columnNames, List<byte[]> rows){
		List<String> output = new ArrayList<>();
		List<Column> columns = columnNames.stream().map(t -> getColumn(t)).collect(Collectors.toList());
		for(byte[] row : rows) {
			StringBuilder rowStringBuilder = new StringBuilder();
			for(Column column : columns) {
				rowStringBuilder.append(table.getValueString(column, row));
				rowStringBuilder.append("\t");
			}
			output.add(rowStringBuilder.toString());
		}
		return output;
	}
}
