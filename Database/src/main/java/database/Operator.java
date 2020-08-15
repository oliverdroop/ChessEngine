package database;

import java.util.Arrays;

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
	
	public boolean evaluate(byte[] a, byte[] b, DataType dataType) {
		if (!dataType.isNumeric()) {
			if (this == EQUAL) {
				return Arrays.equals(a, b);
			}
			if (this == NOT_EQUAL) {
				return !Arrays.equals(a, b);
			}
		}
		else {
			Double valueA = Double.parseDouble(dataType.getValueString(a));
			Double valueB = Double.parseDouble(dataType.getValueString(b));		
			if (this == EQUAL) {
				return Arrays.equals(a, b);
			}
			if (this == NOT_EQUAL) {
				return !Arrays.equals(a, b);
			}
			if (this == LESS) {
				return valueA.compareTo(valueB) < 0;
			}
			if (this == GREATER) {
				return valueA.compareTo(valueB) > 0;
			}
			if (this == LESS_EQUAL) {
				return valueA.compareTo(valueB) <= 0;
			}
			if (this == GREATER_EQUAL) {
				return valueA.compareTo(valueB) >= 0;
			}
		}
		return false;
	}
}
