package database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import database.SQLPhrase.KeywordType;
import database.SQLPhrase.PhraseType;

public class SQLLexer {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SQLLexer.class);
	
	private static final Set<String> INSTRUCTION_KEYWORDS = new HashSet<>(Arrays.asList("INSERT", "SELECT DISTINCT", "SELECT", "UPDATE", "DELETE", "SET", "ORDER BY"));
	
	private static final Set<String> TABLE_IDENTIFIER_KEYWORDS = new HashSet<>(Arrays.asList("FROM", "INTO", "UPDATE", "ON", "LEFT JOIN", "RIGHT JOIN", "FULL JOIN", "INNER JOIN"));
	
	private static final Set<String> EXPRESSION_KEYWORDS = new HashSet<>(Arrays.asList( "WHERE", "AND", "VALUES"));
	
	private static final Set<String> JOIN_KEYWORDS = new HashSet<>(Arrays.asList("LEFT JOIN", "RIGHT JOIN", "FULL JOIN", "INNER JOIN"));
	
	private static final Set<String> ORDER_KEYWORDS = new HashSet<>(Arrays.asList("ASC", "DESC"));
	
	private static List<String> allKeywords;
	
	public static List<SQLPhrase> readQuery(String query){
		List<SQLPhrase> output = new ArrayList<>();
		String currentPhrase = "";
		boolean openQuote = false;
		boolean openBracket = false;
		boolean dot = false;
		boolean numeric = false;
		Operator operator = null;
		for(int i = 0; i < query.length(); i++) {
			String currentCharacter = query.substring(i, i + 1);
			if (!openQuote) {
				currentCharacter = currentCharacter.toUpperCase();
				if (!numeric && currentPhrase.matches("[0-9\\.]")) {
					numeric = true;
				}
			}
			SQLPhrase newPhrase = null;
			if (currentCharacter.equals(" ") && !openQuote) {
				newPhrase = splitOffSQLPhrase(currentPhrase, i);
				if (numeric) {
					newPhrase.setType(PhraseType.VALUE);
				}
			}
			if (currentCharacter.equals("'")) {
				newPhrase = splitOffSQLPhrase(currentPhrase, i);
				if (openQuote) {
					newPhrase.setType(PhraseType.VALUE);
				}
				openQuote = !openQuote;
			}
			if (currentCharacter.equals("(") && !openQuote) {
				newPhrase = splitOffSQLPhrase(currentPhrase, i);
				openBracket = true;
			}
			if (currentCharacter.equals(")") && !openQuote) {
				newPhrase = splitOffSQLPhrase(currentPhrase, i);
				openBracket = false;
			}
			if (currentCharacter.equals(",") && !openQuote) {
				newPhrase = splitOffSQLPhrase(currentPhrase, i);
			}
			if (currentCharacter.equals(".") && !openQuote && !numeric) {
				newPhrase = splitOffSQLPhrase(currentPhrase, i);
				newPhrase.setType(PhraseType.TABLE_NAME);
				dot = true;
			}
			if (getOperator(currentPhrase + currentCharacter) != null && !openQuote) {
				operator = getOperator(currentPhrase + currentCharacter);
			}
			if (currentCharacter.equals(";") && !openQuote) {
				newPhrase = splitOffSQLPhrase(currentPhrase, i);
				if (numeric) {
					newPhrase.setType(PhraseType.VALUE);
				}
			}
			if (newPhrase != null) {
				if (newPhrase.getString().length() > 0) {
					if (newPhrase.getType() == null) {
						categorizePhrase(newPhrase, output);
					}
					if (operator != null) {
						if (newPhrase.hasType(PhraseType.VALUE)) {
							SQLPhrase previousLinkablePhrase = getLastPhrase(output);
							newPhrase.setLinkedColumn(previousLinkablePhrase);
							newPhrase.setLinkedOperator(operator);
							previousLinkablePhrase.setLinkedValue(newPhrase);
							operator = null;
						}
					}
					if (dot) {
						if (!newPhrase.hasType(PhraseType.TABLE_NAME)) {
							newPhrase.setLinkedTable(getLastPhrase(output));
							getLastPhrase(output).setLinkedColumn(newPhrase);
							dot = false;
						}
					}
					if (newPhrase.getType() != null) {
						output.add(newPhrase);
					}
				}
				currentPhrase = "";
			}
			else {				
				currentPhrase += currentCharacter;
			}
		}
		return output;
	}
	
	private static void categorizePhrase(SQLPhrase newPhrase, List<SQLPhrase> previousPhrases) {
		SQLPhrase previousPhrase = getLastPhrase(previousPhrases);
		SQLPhrase previousKeyword = getPreviousKeyword(newPhrase, previousPhrases);
		if (getAllKeywords().contains(newPhrase.getString())) {
			newPhrase.setType(PhraseType.KEYWORD);
			newPhrase.setKeywordTypes(getKeywordTypes(newPhrase.getString()));
		}
		else if (getAllKeywords().contains(previousPhrase.getString() + " " + newPhrase.getString())) {
			String bothPhraseStrings = previousPhrase.getString() + " " + newPhrase.getString();
			previousPhrases.remove(previousPhrase);
			newPhrase.setString(bothPhraseStrings);
			newPhrase.setType(PhraseType.KEYWORD);
			newPhrase.setKeywordTypes(getKeywordTypes(bothPhraseStrings));
		}
		else if(previousKeyword.hasKeywordType(KeywordType.TABLE_IDENTIFIER) && previousPhrase.hasType(PhraseType.KEYWORD)) {
			newPhrase.setType(PhraseType.TABLE_NAME);
		}
		else if(previousKeyword.hasKeywordType(KeywordType.INSTRUCTION)) {
			newPhrase.setType(PhraseType.COLUMN_NAME);
		}
		else if(previousKeyword.hasKeywordType(KeywordType.EXPRESSION)) {
			newPhrase.setType(PhraseType.COLUMN_NAME);
		}
		else if(previousKeyword.hasKeywordType(KeywordType.ORDER)) {
			newPhrase.setType(PhraseType.COLUMN_NAME);
		}
		else if(previousPhrase.hasType(PhraseType.TABLE_NAME)) {
			newPhrase.setType(PhraseType.COLUMN_NAME);
		}
		else if(previousPhrase.hasType(PhraseType.COLUMN_NAME)) {
			newPhrase.setType(PhraseType.COLUMN_NAME);
		}
		if (getOperator(newPhrase.getString()) != null) {
			newPhrase.setType(null);
		}
	}
	
	private static SQLPhrase splitOffSQLPhrase(String currentPhrase, int index) {
		SQLPhrase newPhrase = new SQLPhrase(currentPhrase);
		index ++;
		return newPhrase;
	}
	
	private static Operator getOperator(String input) {
		for(Operator operator : Operator.values()) {
			if (operator.getString().equals(input)) {
				return operator;
			}
		}
		return null;
	}
	
	public static SQLPhrase getLastPhrase(List<SQLPhrase> phrases) {
		return phrases.size() > 0 ? phrases.get(phrases.size() - 1) : null;
	}
	
	public static SQLPhrase getPreviousPhrase(SQLPhrase currentPhrase, List<SQLPhrase> allPhrases) {
		return allPhrases.get(allPhrases.indexOf(currentPhrase) - 1);
	}
	
	public static SQLPhrase getPreviousKeyword(SQLPhrase currentPhrase, List<SQLPhrase> allPhrases) {
		if (allPhrases.size() == 0) {
			return null;
		}
		int upTo = allPhrases.size() - 1;
		if (allPhrases.contains(currentPhrase)) {
			upTo = allPhrases.indexOf(currentPhrase) - 1;
		}
		for(int i = upTo; i >= 0 ; i--) {
			SQLPhrase earlierPhrase = allPhrases.get(i);
			if (earlierPhrase.getType() == PhraseType.KEYWORD) {
				return earlierPhrase;
			}
		}
		return null;
	}
	
	private static final List<String> getAllKeywords(){
		if (allKeywords == null) {
			allKeywords = new ArrayList<>();
			allKeywords.addAll(INSTRUCTION_KEYWORDS);
			allKeywords.addAll(TABLE_IDENTIFIER_KEYWORDS);
			allKeywords.addAll(EXPRESSION_KEYWORDS);
			allKeywords.addAll(JOIN_KEYWORDS);
			allKeywords.addAll(ORDER_KEYWORDS);
		}
		return allKeywords;
	}
	
	private static List<KeywordType> getKeywordTypes(String phrase) {
		Map<KeywordType, Set<String>> keywordGroupMap = new HashMap<>();
		keywordGroupMap.put(KeywordType.INSTRUCTION, INSTRUCTION_KEYWORDS);
		keywordGroupMap.put(KeywordType.TABLE_IDENTIFIER, TABLE_IDENTIFIER_KEYWORDS);
		keywordGroupMap.put(KeywordType.EXPRESSION, EXPRESSION_KEYWORDS);
		keywordGroupMap.put(KeywordType.JOIN, JOIN_KEYWORDS);
		keywordGroupMap.put(KeywordType.ORDER, ORDER_KEYWORDS);
		List<KeywordType> keywordTypes = new ArrayList<>();
		for(KeywordType keywordType : keywordGroupMap.keySet()) {
			if (keywordGroupMap.get(keywordType).contains(phrase)) {
				keywordTypes.add(keywordType);
			}
		}		
		return keywordTypes;
	}
}
