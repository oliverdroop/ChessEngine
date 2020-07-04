package database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Query {
	private Map<String,String> expressions = new HashMap<>();
	private Map<String,String> clauses = new HashMap<>();
	private static List<String> clauseFlags = Arrays.asList(new String[] {"SELECT", "FROM"});
	private static List<String> expressionFlags = Arrays.asList(new String[] {"WHERE", "AND"});
	private Database database;
	
	public Query(List<String> sqlStatement, Database database) {
		this.database = database;
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
		}
	}
	
	public List<String> execute() {
		Table table = database.getTables().get(clauses.get("FROM"));
		List<Column> selectionColumns = new ArrayList<>();
		if (clauses.get("SELECT").equals("*")) {
			selectionColumns.addAll(table.getColumns().values());
		}
		List<byte[]> byteRows = table.getStringMatchedRows(expressions);
		List<String> output = new ArrayList<>();
		byteRows.forEach(br -> output.add(table.getRowString(br)));
		return output;
	}
}
