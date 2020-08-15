package database;

public enum Operator {
	EQUAL("="), 
	LESS("<"), 
	GREATER(">"), 
	LESS_EQUAL("<="), 
	GREATER_EQUAL(">="), 
	NOT_EQUAL("!=");
	
	private String string;
	
	Operator(String string){
		this.string = string;
	}
	
	public String getString() {
		return string;
	}
}
