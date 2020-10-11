package database;

import java.util.ArrayList;
import java.util.List;

public class SQLPhrase {
	
	public static enum PhraseType{
		KEYWORD, VALUE, TABLE_NAME, COLUMN_NAME
	}
	
	public static enum KeywordType{
		INSTRUCTION, TABLE_POINTER, EXPRESSION, JOIN, ORDER, DATA_TYPE
	}
	
	private PhraseType type;
	
	private List<KeywordType> keywordTypes = new ArrayList<>();
	
	private String string;
	
	private SQLPhrase linkedValue;
	
	private SQLPhrase linkedColumn;
	
	private SQLPhrase linkedTable;
	
	private Operator linkedOperator;
	
	public SQLPhrase(String string) {
		this.string = string;
	}
	
	public SQLPhrase(String string, PhraseType type) {
		this.string = string;
		this.type = type;
	}
	
	public String getString() {
		return string;
	}
	public void setString(String string) {
		this.string = string;
	}
	public PhraseType getType() {
		return type;
	}
	public void setType(PhraseType type) {
		this.type = type;
	}

	public List<KeywordType> getKeywordTypes() {
		return keywordTypes;
	}

	public void setKeywordTypes(List<KeywordType> keywordTypes) {
		this.keywordTypes = keywordTypes;
	}
	
	public boolean hasType(PhraseType type) {
		return this.type == type;
	}
	
	public boolean hasKeywordType(KeywordType keywordType) {
		return this.keywordTypes.contains(keywordType);
	}

	public SQLPhrase getLinkedValue() {
		return linkedValue;
	}

	public void setLinkedValue(SQLPhrase linkedValue) {
		this.linkedValue = linkedValue;
	}

	public SQLPhrase getLinkedColumn() {
		return linkedColumn;
	}

	public void setLinkedColumn(SQLPhrase linkedColumn) {
		this.linkedColumn = linkedColumn;
	}

	public SQLPhrase getLinkedTable() {
		return linkedTable;
	}

	public void setLinkedTable(SQLPhrase linkedTable) {
		this.linkedTable = linkedTable;
	}	
	
	public Operator getLinkedOperator() {
		return linkedOperator;
	}

	public void setLinkedOperator(Operator operator) {
		this.linkedOperator = operator;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("[");
		stringBuilder.append("\"");
		stringBuilder.append(string);
		stringBuilder.append("\"");
		stringBuilder.append(", ");
		stringBuilder.append(type);
		for(KeywordType keywordType : keywordTypes) {
			stringBuilder.append(", ");
			stringBuilder.append(keywordType);
		}
		stringBuilder.append("]");
		return stringBuilder.toString();
	}
}
