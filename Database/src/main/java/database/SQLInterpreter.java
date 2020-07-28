package database;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import database.SQLPhrase.KeywordType;
import database.SQLPhrase.PhraseType;

public class SQLInterpreter {
	
	private List<SQLPhrase> sqlStatement;
	
	private Database database;
	
	private Table table;
	
	private List<String> targets;
	
	public QueryParameters interpret(List<SQLPhrase> sqlStatement, Database database) {
		this.sqlStatement = sqlStatement;
		this.database = database;
		QueryParameters parameters = new QueryParameters();
		
		parameters.setDatabase(database);
		parameters.setInstruction(extractInstruction());
		table = extractTable();
		parameters.setTable(table);
		parameters.setConditions(extractConditions());
		parameters.setAssignments(extractAssignments());
		parameters.setTargets(targets);
		parameters.setJoinType(extractJoinType());
		parameters.setJoinCondition(extractJoinCondition());
		
		this.sqlStatement = null;
		this.database = null;
		
		return parameters;
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
			SQLPhrase previousKeyword = SQLLexer.getPreviousKeyword(phrase, sqlStatement);
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
								.filter(p -> SQLLexer.getPreviousKeyword(p, sqlStatement).hasKeywordType(KeywordType.INSTRUCTION))
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
	
	private SQLPhrase extractJoinType() {
		for(SQLPhrase phrase : sqlStatement) {
			if (phrase.hasKeywordType(KeywordType.JOIN)) {
				return phrase;
			}
		}
		return null;
	}
	
	private List<SQLPhrase> extractJoinCondition(){
		List<SQLPhrase> output = new ArrayList<>();
		int i = 0;
		SQLPhrase joinType = extractJoinType();
		if (joinType != null) {
			i = sqlStatement.indexOf(joinType);
			if (sqlStatement.get(i - 1).hasType(PhraseType.TABLE_NAME)
					&& sqlStatement.get(i + 1).hasType(PhraseType.TABLE_NAME)
					&& sqlStatement.get(i + 2).hasKeywordType(KeywordType.TABLE_IDENTIFIER)
					&& sqlStatement.get(i + 3).hasType(PhraseType.TABLE_NAME)
					&& sqlStatement.get(i + 4).hasType(PhraseType.COLUMN_NAME)
					&& sqlStatement.get(i + 5).hasType(PhraseType.TABLE_NAME)
					&& sqlStatement.get(i + 6).hasType(PhraseType.COLUMN_NAME)) {
				if (sqlStatement.get(i - 1).getString().equals(sqlStatement.get(i + 3).getString())) {
					output.add(sqlStatement.get(i + 4));
					output.add(sqlStatement.get(i + 6));
				} else if (sqlStatement.get(i - 1).getString().equals(sqlStatement.get(i + 5).getString())) {
					output.add(sqlStatement.get(i + 6));
					output.add(sqlStatement.get(i + 4));
				}
			}
		}
		return output;
	}
}
