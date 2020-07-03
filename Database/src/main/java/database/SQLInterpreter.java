package database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQLInterpreter {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SQLInterpreter.class);
	
	private enum WordType{
		CLAUSE, IDENTIFIER, EXPRESSION
	};
	
	public String executeQuery(String query) {
		List<String> words = getWords(getCleanQuery(query));
		StringBuilder outputBuilder = new StringBuilder();
		words.forEach(w -> outputBuilder.append(w + System.lineSeparator()));
		return outputBuilder.toString();
	}
	
	private String getCleanQuery(String query) {
		return query;
	}
	
	private String[] separateSingleQuotedStrings(String query) {
		return query.split("'", 0);
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
}
