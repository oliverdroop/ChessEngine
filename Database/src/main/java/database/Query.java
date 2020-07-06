package database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
	
	private List<String> targets;
	
	private List<String> values;
	
	public Query(List<SQLPhrase> sqlStatement, Database database) {
		this.sqlStatement = sqlStatement;
		this.database = database;
		instruction = extractInstruction();
		table = extractTable();
		conditions = extractConditions();
		targets = extractTargets();
		values = extractValues();
	}
	
	private SQLPhrase extractInstruction() {
		for(int i = 0; i < sqlStatement.size(); i++) {
			SQLPhrase phrase = sqlStatement.get(i);
			if (phrase.getKeywordType() != null && phrase.getKeywordType() == KeywordType.INSTRUCTION) {
				return phrase;
			}
		}
		return null;
	}
	
	private Table extractTable() {
		for(int i = 0; i < sqlStatement.size(); i++) {
			SQLPhrase phrase0 = sqlStatement.get(i);
			if (phrase0.getKeywordType() != null && phrase0.getKeywordType() == KeywordType.TABLE_IDENTIFIER && i + 1 < sqlStatement.size()) {
				SQLPhrase phrase1 = sqlStatement.get(i + 1);
				if (phrase1.getType() == PhraseType.TABLE_NAME) {
					if (phrase1.getString().contains("(")) {
						String[] parts = phrase1.getString().split("\\(");
						sqlStatement.add(i + 2, new SQLPhrase(parts[1].replace(")", ""), PhraseType.COLUMN_NAME));
						phrase1 = new SQLPhrase(parts[0], PhraseType.TABLE_NAME);
						sqlStatement.set(i + 1, phrase1);
					}
					return database.getTables().get(phrase1.getString());									
				}
			}
		}		
		return null;
	}
	
	private Map<String,String> extractConditions(){
		Map<String, String> equalityConditions = new HashMap<>();
		for(int i = 0; i < sqlStatement.size(); i++) {
			SQLPhrase phrase0 = sqlStatement.get(i);
			if (phrase0.getKeywordType() != null && phrase0.getKeywordType() == KeywordType.EXPRESSION && i + 2 < sqlStatement.size()) {
				SQLPhrase phrase1 = sqlStatement.get(i + 1);
				SQLPhrase phrase2 = sqlStatement.get(i + 2);
				if (phrase1.getString().endsWith("=")) {
					phrase1.setString(phrase1.getString().substring(0, phrase1.getString().length() - 1));
					Column column = getColumn(phrase1.getString());
					if (phrase1.getType() == PhraseType.COLUMN_NAME && phrase2.getType() == PhraseType.VALUE && column != null) {
						equalityConditions.put(column.getName(), phrase2.getString());
					}
				}				
			}
		}
		return equalityConditions;
	}
	
	private List<String> extractTargets(){
		List<String> targets = new ArrayList<>();
		for(int i = 0; i < sqlStatement.size(); i++) {
			SQLPhrase phrase0 = sqlStatement.get(i);
			if (phrase0.getType() == PhraseType.COLUMN_NAME && i > 0) {
				SQLPhrase phrase1 = sqlStatement.get(i - 1);
				if ((phrase1.getKeywordType() != null && phrase1.getKeywordType() == KeywordType.INSTRUCTION) || phrase1.getType() == PhraseType.TABLE_NAME) {
					for(String columnName : phrase0.getString().split(",")) {
						if (columnName.equals("*")) {
							table.getColumns().values().forEach(c -> targets.add(c.getName()));
						}
						else {
							Column column = getColumn(columnName);
							if (column != null) {
								targets.add(column.getName());
							}
						}
					}
				}
			}
		}
		return targets;
	}
	
	private List<String> extractValues(){
		List<String> values = new ArrayList<>();
		for(int i = 0; i < sqlStatement.size(); i++) {
			SQLPhrase phrase0 = sqlStatement.get(i);
			if (phrase0.getType() == PhraseType.VALUE) {
				values.add(phrase0.getString());
			}
		}
		return values;
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
				if (!targets.isEmpty() && !values.isEmpty() && targets.size() == values.size()) {
					Map<String,String> insertMap = new HashMap<>();
					for(int i = 0; i < targets.size(); i++) {
						insertMap.put(targets.get(i), values.get(i));
					}
					int rowsAdded = table.addRow(table.buildRow(insertMap));
					output.add(String.format("Inserted %d row into table %s", rowsAdded, table.getName()));
				}
			}
			if (instruction.getString().equals("SELECT") && !conditions.isEmpty()) {
				List<byte[]> rows = table.getStringMatchedRows(conditions);
				if (!targets.isEmpty()) {
					return getTargetColumnsFromRows(rows);
				}
			}
			if (instruction.getString().equals("UPDATE")) {
				
			}
			if (instruction.getString().equals("DELETE") && !conditions.isEmpty()) {
				int count = table.deleteStringMatchedRows(conditions);
				String message = String.format("Deleted %d rows from table %s", count, table.getName());
				output.add(message);
			}
		}
		return output;
	}
	
	public List<String> getTargetColumnsFromRows(List<byte[]> rows){
		List<String> output = new ArrayList<>();
		List<Column> columns = targets.stream().map(t -> getColumn(t)).collect(Collectors.toList());
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
