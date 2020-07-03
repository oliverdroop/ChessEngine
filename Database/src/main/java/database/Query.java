package database;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Query {
	private Map<String,String> expressions;
	private Map<String,String> clauses;
	private static List<String> clauseFlags = Arrays.asList(new String[] {"SELECT", "FROM"});
	private static List<String> expressionFlags = Arrays.asList(new String[] {"WHERE", "AND"});
	
	public Query buildQuery(List<String> sqlStatement) {
		for(int i = 0; i < sqlStatement.size(); i++) {
			String word = sqlStatement.get(i);
			if (clauseFlags.contains(word)) {
				clauses.put(word, sqlStatement.get(i + 1));
				i++;
			}
			if (expressionFlags.contains(word)) {
				if (sqlStatement.get(i + 2).equals("=")) {
					expressions.put(sqlStatement.get(i + 1), sqlStatement.get(i + 3));
				}
			}
			if (word.equals(";")) {
				return this;
			}
		}
		return null;
	}
	
	public void execute() {
		
	}
}
