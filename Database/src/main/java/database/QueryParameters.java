package database;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javafx.util.Pair;

public class QueryParameters {
	
	private Database database;
	
	private Table table;
	
	private SQLPhrase instruction;
	
	private Map<String, Pair<Operator, String>> conditions;
	
	private Map<SQLPhrase, SQLPhrase> assignments;
	
	private List<SQLPhrase> targets;
	
	private SQLPhrase joinType;
	
	private List<SQLPhrase> joinCondition;
	
	private LinkedHashMap<String, Boolean> orderBy;

	public Database getDatabase() {
		return database;
	}

	public void setDatabase(Database database) {
		this.database = database;
	}

	public Table getTable() {
		return table;
	}

	public void setTable(Table table) {
		this.table = table;
	}

	public SQLPhrase getInstruction() {
		return instruction;
	}

	public void setInstruction(SQLPhrase instruction) {
		this.instruction = instruction;
	}

	public Map<String, Pair<Operator, String>> getConditions() {
		return conditions;
	}

	public void setConditions(Map<String, Pair<Operator, String>> conditions) {
		this.conditions = conditions;
	}

	public Map<SQLPhrase, SQLPhrase> getAssignments() {
		return assignments;
	}

	public void setAssignments(Map<SQLPhrase, SQLPhrase> assignments) {
		this.assignments = assignments;
	}

	public List<SQLPhrase> getTargets() {
		return targets;
	}

	public void setTargets(List<SQLPhrase> targets) {
		this.targets = targets;
	}

	public SQLPhrase getJoinType() {
		return joinType;
	}

	public void setJoinType(SQLPhrase joinType) {
		this.joinType = joinType;
	}

	public List<SQLPhrase> getJoinCondition() {
		return joinCondition;
	}

	public void setJoinCondition(List<SQLPhrase> joinCondition) {
		this.joinCondition = joinCondition;
	}

	public LinkedHashMap<String, Boolean> getOrderBy() {
		return orderBy;
	}

	public void setOrderBy(LinkedHashMap<String, Boolean> orderBy) {
		this.orderBy = orderBy;
	}
	
	
}
