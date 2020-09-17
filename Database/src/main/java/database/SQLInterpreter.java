package database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import database.SQLPhrase.KeywordType;
import database.SQLPhrase.PhraseType;
import javafx.util.Pair;

public final class SQLInterpreter {
	
	public static QueryParameters interpret(List<SQLPhrase> sqlStatement, Database database) {
		QueryParameters parameters = new QueryParameters();
		
		parameters.setDatabase(database);
		parameters.setInstruction(extractInstruction(sqlStatement));
		Table table = extractTable(sqlStatement, database);
		parameters.setTable(table);
		parameters.setConditions(extractConditions(sqlStatement));
		parameters.setOrderBy(extractOrderBy(sqlStatement));
		Map<String, Object> assignmentsAndTargets = extractAssignmentsAndTargets(sqlStatement, database, table);		
		parameters.setAssignments((Map<String,String>) assignmentsAndTargets.get("assignments"));
		parameters.setTargets((List<String>) assignmentsAndTargets.get("targets"));
		SQLPhrase joinType = extractJoinType(sqlStatement);
		parameters.setJoinType(joinType);
		parameters.setJoinCondition(extractJoinCondition(sqlStatement, joinType));
		
		return parameters;
	}
	
	private static SQLPhrase extractInstruction(List<SQLPhrase> sqlStatement) {
		return sqlStatement.get(0).hasKeywordType(KeywordType.INSTRUCTION) ? sqlStatement.get(0) : null;
	}
	
	private static Table extractTable(List<SQLPhrase> sqlStatement, Database database) {
		for(int i = 0; i < sqlStatement.size(); i++) {
			SQLPhrase phrase = sqlStatement.get(i);
			if (phrase.getType() == PhraseType.TABLE_NAME) {
				return database.getTables().get(phrase.getString());
			}
		}
		return null;
	}
	
	private static Map<String, Pair<Operator, String>> extractConditions(List<SQLPhrase> sqlStatement){
		int startIndex = sqlStatement.size();
		for (int i = 0; i < sqlStatement.size(); i++) {
			if (sqlStatement.get(i).hasKeywordType(KeywordType.EXPRESSION)) {
				startIndex = i;
				break;
			}
		}
		Map<String, Pair<Operator, String>> output = new LinkedHashMap<>();
		for(int i = startIndex; i < sqlStatement.size(); i++) {
			SQLPhrase phrase = sqlStatement.get(i);
			SQLPhrase linkedPhrase = phrase.getLinkedColumn();
			if (linkedPhrase != null && linkedPhrase.hasType(PhraseType.COLUMN_NAME) && phrase.hasType(PhraseType.VALUE)) {
				output.put(linkedPhrase.getString(), phrase.getLinkedOperator().pairWith(phrase.getString()));
			}
		}
		return output;
	}
	
	private static Map<String, Object> extractAssignmentsAndTargets(List<SQLPhrase> sqlStatement, Database database, Table table){
		Map<String, String> assignments = new LinkedHashMap<>();
		List<String> targets = null;
		for (int i = 0; i < sqlStatement.size(); i++) {
			SQLPhrase phrase = sqlStatement.get(i);
			SQLPhrase previousKeyword = SQLLexer.getPreviousKeyword(phrase, sqlStatement);
			if (previousKeyword != null 
					&& previousKeyword.hasKeywordType(KeywordType.INSTRUCTION) 
					&& phrase.getLinkedValue() != null 
					&& phrase.hasType(PhraseType.COLUMN_NAME) 
					&& phrase.getLinkedValue().hasType(PhraseType.VALUE)) {
				assignments.put(phrase.getString(), phrase.getLinkedValue().getString());
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
					assignments.put(unlinkedColumns.get(i).getString(), unlinkedValues.get(i).getString());
				}
			}
			else {
				if (unlinkedValues.size() == 0) {
					if (unlinkedColumns.get(0).getString().equals("*") && table != null) {
						targets = table.getColumns().keySet().stream().collect(Collectors.toList());
					}
					else {
						targets = unlinkedColumns
								.stream()
								.filter(p -> p.getLinkedTable() == null || database.getTables().get(p.getLinkedTable().getString()).equals(table))
								.filter(p -> SQLLexer.getPreviousKeyword(p, sqlStatement).hasKeywordType(KeywordType.INSTRUCTION))
								.filter(p -> !SQLLexer.getPreviousKeyword(p, sqlStatement).hasKeywordType(KeywordType.ORDER))
								.filter(p -> !SQLLexer.getPreviousKeyword(p, sqlStatement).getString().equals("ORDER BY"))
								.map(p -> p.getString())
								.collect(Collectors.toList());
					}
				}
			}
		}
		else if(unlinkedValues.size() == table.getColumns().size()){
			List<String> keyList = table.getColumns().keySet().stream().collect(Collectors.toList());
			for(int i = 0; i < unlinkedValues.size(); i++) {
				assignments.put(keyList.get(i), unlinkedValues.get(i).getString());
			}
		}
		Map<String, Object> output = new HashMap<>();
		output.put("assignments", assignments);
		output.put("targets", targets);
		return output;
	}
	
	private static SQLPhrase extractJoinType(List<SQLPhrase> sqlStatement) {
		for(SQLPhrase phrase : sqlStatement) {
			if (phrase.hasKeywordType(KeywordType.JOIN)) {
				return phrase;
			}
		}
		return null;
	}
	
	private static List<SQLPhrase> extractJoinCondition(List<SQLPhrase> sqlStatement, SQLPhrase joinType){
		List<SQLPhrase> output = new ArrayList<>();
		int i = 0;
		if (joinType != null) {
			i = sqlStatement.indexOf(joinType);
			if (sqlStatement.get(i - 1).hasType(PhraseType.TABLE_NAME)
					&& sqlStatement.get(i + 1).hasType(PhraseType.TABLE_NAME)
					&& sqlStatement.get(i + 2).hasKeywordType(KeywordType.TABLE_POINTER)
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
	
	private static LinkedHashMap<String, Boolean> extractOrderBy(List<SQLPhrase> sqlStatement){
		int startIndex = 0;
		for(int i = 0; i < sqlStatement.size(); i++) {
			SQLPhrase phrase = sqlStatement.get(i);
			if (phrase.hasKeywordType(KeywordType.INSTRUCTION) && phrase.getString().equals("ORDER BY") && i < sqlStatement.size() - 1) {
				startIndex = i + 1;
			}
		}
		if (startIndex == 0) {
			return null;
		}
		LinkedHashMap<String, Boolean> output = new LinkedHashMap<>();
		for(int i = startIndex; i < sqlStatement.size(); i++) {
			SQLPhrase phrase = sqlStatement.get(i);
			if (phrase.hasType(PhraseType.COLUMN_NAME)) {
				output.put(phrase.getString(), true);
			} else if (phrase.hasKeywordType(KeywordType.ORDER)){
				SQLPhrase previousPhrase = SQLLexer.getPreviousPhrase(phrase, sqlStatement);
				if (previousPhrase.hasType(PhraseType.COLUMN_NAME)) {
					output.put(previousPhrase.getString(), phrase.getString().equals("ASC"));
				}
			}
		}
		return output;
	}
}
