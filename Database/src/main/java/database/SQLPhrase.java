package database;

public class SQLPhrase {
	
	public static enum PhraseType{
		KEYWORD, VALUE, TABLE_NAME, COLUMN_NAME
	}
	
	public static enum KeywordType{
		INSTRUCTION, TABLE_IDENTIFIER, EXPRESSION
	}
	
	private PhraseType type;
	
	private KeywordType keywordType;
	
	private String string;
	
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

	public KeywordType getKeywordType() {
		return keywordType;
	}

	public void setKeywordType(KeywordType keywordType) {
		this.keywordType = keywordType;
	}
	
	
}
