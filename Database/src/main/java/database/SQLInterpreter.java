package database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
	
	private static final Set<String> INSTRUCTION_KEYWORDS = (Set) Arrays.asList("INSERT", "SELECT DISTINCT", "SELECT", "UPDATE", "DELETE");
	
	private static final Set<String> TABLE_IDENTIFIER_KEYWORDS = (Set) Arrays.asList("FROM", "INTO");
	
	private static final Set<String> EXPRESSION_KEYWORDS = (Set) Arrays.asList( "WHERE", "AND" );
	
	public String executeQuery(String queryString, Database database) {
		List<String> words = getWords(getCleanQuery(queryString));
		Query query = new Query(words, database);
		List<String> outputLines = query.execute();
		StringBuilder outputBuilder = new StringBuilder();
		outputLines.forEach(line -> outputBuilder.append(System.lineSeparator() + line));
		return outputBuilder.toString();
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
					firstKeyword = getFirstKeyword(part.substring(ik));
				}
				else {
					SQLPhrase phrase = new SQLPhrase(part.substring(0, firstKeyword.length()));
					phrase.setType(PhraseType.KEYWORD);
					phrase.setKeywordType(getKeywordType(phrase.getString()));
					phrases.add(phrase);
					firstKeyword = getFirstKeyword(part.substring(firstKeyword.length()));
				}
				if (firstKeyword == null) {
					phrases.add(new SQLPhrase(firstKeyword));
				}
			}
		}
		
		//categorize phrases which aren't values or keywords
		for(int i = 0; i < phrases.size(); i++) {
			SQLPhrase phrase = phrases.get(i);
			if (phrase.getType() == null && i > 0 && phrases.get(i - 1).getType() == PhraseType.KEYWORD) {
				if (phrases.get(i - 1).getKeywordType() == KeywordType.INSTRUCTION) {
					phrase.setType(PhraseType.COLUMN_NAME);
				}
				if (phrases.get(i - 1).getKeywordType() == KeywordType.TABLE_IDENTIFIER) {
					phrase.setType(PhraseType.TABLE_NAME);
				}
				if (phrases.get(i - 1).getKeywordType() == KeywordType.EXPRESSION) {
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
	
	private List<String> getWords(String query) {
		List<String> output = new ArrayList<>();
		String[] parts0 = separateSingleQuotedStrings(query);
		for(int i = 0; i < parts0.length; i++) {
			if (i % 2 == 0) {
				String[] parts1 = parts0[i].split(" ");
				for(String s : parts1) {
					output.add(s.toUpperCase());
				}
			}
			else {
				output.add(parts0[i]);
			}
		}
		output = separateSubstrings(output, "=");
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
	
	private Set<String> getKeywordGroup(String phrase) {
		List<Set<String>> keywordGroups = Arrays.asList(INSTRUCTION_KEYWORDS, TABLE_IDENTIFIER_KEYWORDS, EXPRESSION_KEYWORDS);
		List<Set<String>> output = keywordGroups.stream().filter(g -> g.contains(phrase)).collect(Collectors.toList());
		return output.size() == 1 ? output.get(0) : null;
	}
	
	private KeywordType getKeywordType(String phrase) {
		Map<KeywordType, Set<String>> keywordGroupMap = new HashMap<>();
		keywordGroupMap.put(KeywordType.INSTRUCTION, INSTRUCTION_KEYWORDS);
		keywordGroupMap.put(KeywordType.TABLE_IDENTIFIER, TABLE_IDENTIFIER_KEYWORDS);
		keywordGroupMap.put(KeywordType.EXPRESSION, EXPRESSION_KEYWORDS);
		for(KeywordType keywordType : keywordGroupMap.keySet()) {
			if (keywordGroupMap.get(keywordType).contains(phrase)) {
				return keywordType;
			}
		}
		return null;
	}
}
