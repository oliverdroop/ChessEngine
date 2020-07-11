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

public class SQLInterpreter {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SQLInterpreter.class);
	
	private static final Set<String> INSTRUCTION_KEYWORDS = new HashSet<>(Arrays.asList("INSERT", "SELECT DISTINCT", "SELECT", "UPDATE", "DELETE", "SET"));
	
	private static final Set<String> TABLE_IDENTIFIER_KEYWORDS = new HashSet<>(Arrays.asList("FROM", "INTO", "UPDATE"));
	
	private static final Set<String> EXPRESSION_KEYWORDS = new HashSet<>(Arrays.asList( "WHERE", "AND", "VALUES"));
	
	public List<SQLPhrase> readQuery(String query){
		List<SQLPhrase> output = new ArrayList<>();
		String currentPhrase = "";
		boolean openQuote = false;
		boolean openBracket = false;
		boolean equality = false;
		for(int i = 0; i < query.length(); i++) {
			String currentCharacter = query.substring(i, i + 1);
			if (!openQuote) {
				currentCharacter = currentCharacter.toUpperCase();
			}
			SQLPhrase newPhrase = null;			
			if (currentCharacter.equals(" ")) {
				newPhrase = splitOffSQLPhrase(currentPhrase, i);				
			}
			if (currentCharacter.equals("'")) {
				newPhrase = splitOffSQLPhrase(currentPhrase, i);
				if (openQuote) {
					newPhrase.setType(PhraseType.VALUE);
				}
				openQuote = !openQuote;
			}
			if (currentCharacter.equals("(")) {
				newPhrase = splitOffSQLPhrase(currentPhrase, i);
				openBracket = true;
			}
			if (currentCharacter.equals(")")) {
				newPhrase = splitOffSQLPhrase(currentPhrase, i);
				openBracket = false;
			}
			if (currentCharacter.equals(",")) {
				newPhrase = splitOffSQLPhrase(currentPhrase, i);
			}
			if (currentCharacter.equals("=")) {
				newPhrase = splitOffSQLPhrase(currentPhrase, i);
				equality = true;
			}
			if (currentCharacter.equals(";")) {
				newPhrase = splitOffSQLPhrase(currentPhrase, i);
			}
			if (newPhrase != null) {
				if (newPhrase.getString().length() > 0) {
					if (newPhrase.getType() == null) {
						categorizePhrase(newPhrase, output);
					}
					if(equality) {
						newPhrase.setLinkedPhrase(getLastPhrase(output));
						getLastPhrase(output).setLinkedPhrase(newPhrase);						
						equality = false;
					}
					output.add(newPhrase);
				}
				currentPhrase = "";
			}
			else {				
				currentPhrase += currentCharacter;
			}
		}
		return output;
	}
	
	private void categorizePhrase(SQLPhrase newPhrase, List<SQLPhrase> previousPhrases) {
		SQLPhrase previousPhrase = getLastPhrase(previousPhrases);
		SQLPhrase previousKeyword = getPreviousKeyword(newPhrase, previousPhrases);
		if (getAllKeywords().contains(newPhrase.getString())) {
			newPhrase.setType(PhraseType.KEYWORD);
			newPhrase.setKeywordTypes(getKeywordTypes(newPhrase.getString()));
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
		else if(previousPhrase.hasType(PhraseType.TABLE_NAME) || previousPhrase.hasType(PhraseType.COLUMN_NAME)) {
			newPhrase.setType(PhraseType.COLUMN_NAME);
		}
	}
	
	private SQLPhrase splitOffSQLPhrase(String currentPhrase, int index) {
		SQLPhrase newPhrase = new SQLPhrase(currentPhrase);
		index ++;
		return newPhrase;
	}
	
	private String[] separateSingleQuotedStrings(String query) {
		return query.split("'", 0);
	}
	
	
	
	private List<String> separateStartingKeyword(String phrase){
		int keywordLength = 0;
		String startingKeyword = getStartingKeyword(phrase);
		if (startingKeyword != null && startingKeyword.length() < phrase.length()) {
			keywordLength = startingKeyword.length();
		}
		List<String> output = new ArrayList<String>();
		if (keywordLength == 0) {
			output.add(phrase);
		}
		else {
			output.add(phrase.substring(0, keywordLength));
			output.add(phrase.substring(keywordLength));
		}
		return output;
	}
	
	public static SQLPhrase getLastPhrase(List<SQLPhrase> phrases) {
		return phrases.size() > 0 ? phrases.get(phrases.size() - 1) : null;
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
	
	private String getStartingKeyword(String phrase) {
		for(String keyword : getAllKeywords()) {
			if (phrase.startsWith(keyword)) {
				return keyword;
			}
		}
		return null;
	}
	
	private List<String> getAllKeywords(){
		List<String> allKeywords = new ArrayList<>();
		allKeywords.addAll(INSTRUCTION_KEYWORDS);
		allKeywords.addAll(TABLE_IDENTIFIER_KEYWORDS);
		allKeywords.addAll(EXPRESSION_KEYWORDS);
		return allKeywords;
	}
	
	private List<KeywordType> getKeywordTypes(String phrase) {
		Map<KeywordType, Set<String>> keywordGroupMap = new HashMap<>();
		keywordGroupMap.put(KeywordType.INSTRUCTION, INSTRUCTION_KEYWORDS);
		keywordGroupMap.put(KeywordType.TABLE_IDENTIFIER, TABLE_IDENTIFIER_KEYWORDS);
		keywordGroupMap.put(KeywordType.EXPRESSION, EXPRESSION_KEYWORDS);
		List<KeywordType> keywordTypes = new ArrayList<>();
		for(KeywordType keywordType : keywordGroupMap.keySet()) {
			if (keywordGroupMap.get(keywordType).contains(phrase)) {
				keywordTypes.add(keywordType);
			}
		}		
		return keywordTypes;
	}
}
