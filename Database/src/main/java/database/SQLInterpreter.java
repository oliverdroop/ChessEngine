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
	
	public Query buildQuery(String queryString, Database database) {
		return new Query(getSQLPhrases(queryString), database);
	}
	
	private String getCleanQuery(String query) {
		return query;
	}
	
	private String[] separateSingleQuotedStrings(String query) {
		return query.split("'", 0);
	}
	
	private List<SQLPhrase> getSQLPhrases(String query){
		//identify single-quoted phrases
		String[] parts0 = separateSingleQuotedStrings(query);
		//capitalize non-quoted phrases
		for(int i = 0; i < parts0.length; i += 2) {
			parts0[i] = parts0[i].toUpperCase();
		}
		List<SQLPhrase> phrases = new ArrayList<>();
		
		//categorize values and keywords
		for(int i = 0; i < parts0.length; i++) {
			String part = parts0[i];
			if (i % 2 == 1) {
				phrases.add(new SQLPhrase(part, PhraseType.VALUE));
				continue;
			}
			String firstKeyword = getFirstKeyword(part);
			while(firstKeyword != null) {
				int ik = part.indexOf(firstKeyword);
				if (ik != 0) {
					phrases.add(new SQLPhrase(part.substring(0, ik)));
					part = part.substring(ik);
				}
				else {
					SQLPhrase phrase = new SQLPhrase(part.substring(0, firstKeyword.length()));
					part = part.substring(firstKeyword.length());
					phrase.setType(PhraseType.KEYWORD);
					phrase.setKeywordTypes(getKeywordTypes(phrase.getString()));
					phrases.add(phrase);
					firstKeyword = getFirstKeyword(part);
				}
			}
			if (firstKeyword == null) {
				phrases.add(new SQLPhrase(part));
			}
		}
		
		//categorize phrases which aren't values or keywords and remove spaces
		for(int i = 0; i < phrases.size(); i++) {
			SQLPhrase phrase = phrases.get(i);
			phrase.setString(phrase.getString().replace(" ", ""));
			SQLPhrase previousKeyword = getPreviousKeyword(phrase, phrases);
			if (phrase.getType() == null && i > 0 && previousKeyword.getType() == PhraseType.KEYWORD) {				
				if (previousKeyword.getKeywordTypes().contains(KeywordType.INSTRUCTION)) {
					if (previousKeyword.getKeywordTypes().contains(KeywordType.TABLE_IDENTIFIER)) {
						phrase.setType(PhraseType.TABLE_NAME);
					}
					else {
						phrase.setType(PhraseType.COLUMN_NAME);
					}
				}
				if (previousKeyword.getKeywordTypes().contains(KeywordType.TABLE_IDENTIFIER)) {
					phrase.setType(PhraseType.TABLE_NAME);
				}
				if (previousKeyword.getKeywordTypes().contains(KeywordType.EXPRESSION)) {
					phrase.setType(PhraseType.COLUMN_NAME);
				}
			}
		}
		return phrases;
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
	
	private List<String> separateSubstrings(List<String> strings, String pattern){
		List<String> output = new ArrayList<>();
		for(String s : strings) {
			if (!s.equals(pattern) && s.contains(pattern)) {
				String[] parts = s.split(pattern, 0);
				for(int i = 0; i < parts.length; i++) {
					output.add(parts[i]);
					if (i < parts.length - 1) {
						output.add(pattern);
					}
				}
			}
			else {
				output.add(s);
			}
		}
		return output;
	}
	
	public static SQLPhrase getPreviousKeyword(SQLPhrase currentPhrase, List<SQLPhrase> allPhrases) {
		for(int i = allPhrases.indexOf(currentPhrase) - 1; i >= 0 ; i--) {
			SQLPhrase earlierPhrase = allPhrases.get(i);
			if (earlierPhrase.getType() == PhraseType.KEYWORD) {
				return earlierPhrase;
			}
		}
		return null;
	}
	
	private String getFirstKeyword(String phrase) {
		List<String> containedKeywords = new ArrayList<>();
		for(String keyword : getAllKeywords()) {
			if (phrase.contains(keyword)) {
				containedKeywords.add(keyword);
			}
		}
		if (containedKeywords.isEmpty()) {
			return null;
		}
		else {
			String firstKeyword = containedKeywords.get(0);
			for(String keyword : containedKeywords) {
				if (phrase.indexOf(keyword) < phrase.indexOf(firstKeyword)) {
					firstKeyword = keyword;
				}
			}
			return firstKeyword;
		}
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
