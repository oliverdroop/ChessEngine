package database;

import java.util.ArrayList;
import java.util.List;

public class SQLPhrase {
	
	public static enum PhraseType{
		KEYWORD, VALUE, TABLE_NAME, COLUMN_NAME
	}
	
	public static enum KeywordType{
		INSTRUCTION, TABLE_IDENTIFIER, EXPRESSION
	}
	
	private PhraseType type;
	
	private List<KeywordType> keywordTypes = new ArrayList<>();
	
	private String string;
	
	private SQLPhrase linkedPhrase;
	
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
	
	public SQLPhrase getLinkedPhrase() {
		return linkedPhrase;
	}

	public void setLinkedPhrase(SQLPhrase linkedPhrase) {
		this.linkedPhrase = linkedPhrase;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(string);
		stringBuilder.append(", ");
		stringBuilder.append(type);
		stringBuilder.append(", [");
		keywordTypes.forEach(t -> stringBuilder.append(t + ", "));
		stringBuilder.append("]");
		return stringBuilder.toString();
	}
}
