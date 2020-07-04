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
	
	private Database database;
	
	private Table table;
	
	private SQLPhrase instruction;
	
	private Map<String, String> conditions;
	
	private List<String> targets;
	
	public Query(List<SQLPhrase> sqlStatement, Database database) {
		this.database = database;
		instruction = extractInstruction(sqlStatement);
		table = extractTable(sqlStatement);
		conditions = extractConditions(sqlStatement);
		targets = extractTargets(sqlStatement);
	}
	
	private SQLPhrase extractInstruction(List<SQLPhrase> sqlStatement) {
		for(int i = 0; i < sqlStatement.size(); i++) {
			SQLPhrase phrase = sqlStatement.get(i);
			if (phrase.getKeywordType() != null && phrase.getKeywordType() == KeywordType.INSTRUCTION) {
				return phrase;
			}
		}
		return null;
	}
	
	private Table extractTable(List<SQLPhrase> sqlStatement) {
		for(int i = 0; i < sqlStatement.size(); i++) {
			SQLPhrase phrase0 = sqlStatement.get(i);
			if (phrase0.getKeywordType() != null && phrase0.getKeywordType() == KeywordType.TABLE_IDENTIFIER && i + 1 < sqlStatement.size()) {
				SQLPhrase phrase1 = sqlStatement.get(i + 1);
				if (phrase1.getType() == PhraseType.TABLE_NAME) {
					return database.getTables().get(phrase1.getString());
				}
			}
		}
		return null;
	}
	
	private Map<String,String> extractConditions(List<SQLPhrase> sqlStatement){
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
	
	private List<String> extractTargets(List<SQLPhrase> sqlStatement){
		List<String> targets = new ArrayList<>();
		for(int i = 0; i < sqlStatement.size(); i++) {
			SQLPhrase phrase0 = sqlStatement.get(i);
			if (phrase0.getType() == PhraseType.COLUMN_NAME && i > 0) {
				SQLPhrase phrase1 = sqlStatement.get(i - 1);
				if (phrase1.getKeywordType() != null && phrase1.getKeywordType() == KeywordType.INSTRUCTION) {
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
				
			}
			if (instruction.getString().equals("SELECT")) {
				if (!conditions.isEmpty()) {
					List<byte[]> rows = table.getStringMatchedRows(conditions);
					if (!targets.isEmpty()) {
						List<Column> columns = targets.stream().map(t -> getColumn(t)).collect(Collectors.toList());
						for(byte[] row : rows) {
							StringBuilder rowStringBuilder = new StringBuilder();
							for(Column column : columns) {
								rowStringBuilder.append(table.getValueString(column, row));
								rowStringBuilder.append("\t");
							}
							output.add(rowStringBuilder.toString());
						}
					}
				}
			}
			if (instruction.getString().equals("UPDATE")) {
				
			}
			if (instruction.getString().equals("DELETE")) {
				
			}
		}
		return output;
	}
}
