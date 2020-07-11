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
	
	public Query(List<SQLPhrase> sqlStatement, Database database) {
		this.sqlStatement = sqlStatement;
		this.database = database;
		instruction = extractInstruction();
		table = extractTable();
		conditions = extractConditions();
		assignments = extractAssignments();
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
		int startIndex = 0;
		for (int i = 0; i < sqlStatement.size(); i++) {
			if (sqlStatement.get(i).hasKeywordType(KeywordType.EXPRESSION)) {
				startIndex = i;
				break;
			}
		}
		Map<String, String> output = new LinkedHashMap<>();
		for(int i = startIndex; i < sqlStatement.size(); i++) {
			SQLPhrase phrase = sqlStatement.get(i);
			SQLPhrase linkedPhrase = phrase.getLinkedPhrase();
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
					&& phrase.getLinkedPhrase() != null 
					&& phrase.hasType(PhraseType.COLUMN_NAME) 
					&& phrase.getLinkedPhrase().hasType(PhraseType.VALUE)) {
				output.put(phrase.getString(), phrase.getLinkedPhrase().getString());
			}
		}
		List<SQLPhrase> unlinkedColumns = new ArrayList<>();
		List<SQLPhrase> unlinkedValues = new ArrayList<>();
		for (int i = 0; i < sqlStatement.size(); i++) {
			SQLPhrase phrase = sqlStatement.get(i);
			if (phrase.getLinkedPhrase() == null) {
				if (phrase.hasType(PhraseType.COLUMN_NAME)) {
					unlinkedColumns.add(phrase);
				}
				else if (phrase.hasType(PhraseType.VALUE)) {
					unlinkedValues.add(phrase);
				}
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
					targets = unlinkedColumns.stream().map(p -> p.getString()).collect(Collectors.toList());
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
		if (table != null && instruction != null) {
			if (instruction.getString().equals("INSERT")) {
				int rowsAdded = table.addRow(table.buildRow(assignments));
				output.add(String.format("Inserted %d row into table %s", rowsAdded, table.getName()));
			}
			if (instruction.getString().equals("SELECT") && !conditions.isEmpty()) {
				List<byte[]> rows = table.getStringMatchedRows(conditions);
				if (!targets.isEmpty()) {
					return getColumnsFromRows(targets, rows);
				}
			}
			if (instruction.getString().equals("UPDATE")) {
				if (!assignments.isEmpty() && !conditions.isEmpty()) {
					int rowsUpdated = 0;
					if (!conditions.isEmpty()) {
						rowsUpdated = table.updateStringMatchedRows(conditions, assignments);
					}
					else {
						for(int i = 0; i < table.getAllRows().length; i++) {							
							rowsUpdated += table.updateRowStrings(i, assignments);
						}
					}
					output.add(String.format("Updated %d rows in table %s", rowsUpdated, table.getName()));
				}
			}
			if (instruction.getString().equals("DELETE") && !conditions.isEmpty()) {
				int count = table.deleteStringMatchedRows(conditions);
				String message = String.format("Deleted %d rows from table %s", count, table.getName());
				output.add(message);
			}
		}
		return output;
	}
	
	public List<String> getColumnsFromRows(List<String> columnNames, List<byte[]> rows){
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
