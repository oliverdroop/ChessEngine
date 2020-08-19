package database;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.util.Pair;

public enum Operator {
	EQUAL("="), 
	LESS("<"), 
	GREATER(">"), 
	LESS_EQUAL("<="), 
	GREATER_EQUAL(">="), 
	NOT_EQUAL("!="),
	CONTAINS("LIKE");
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Operator.class);
	
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
			if (this == CONTAINS) {
				return arrayContains(a, b);
			}
			LOGGER.warn("Cannot perform evaluation {} on data type {}", this.name(), dataType.name());
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
	
	public Pair<Operator, byte[]> pairWith(byte[] valueBytes){
		return new Pair<Operator, byte[]>(this, valueBytes);
	}
	
	public Pair<Operator, String> pairWith(String valueString){
		return new Pair<Operator, String>(this, valueString);
	}
	
	public static boolean isValidOperation(Operator operator, DataType dataType) {
		if (dataType.isNumeric()) {
			if (operator == CONTAINS) {
				return false;
			} else {
				return true;
			}
		}
		if (operator == EQUAL || operator == NOT_EQUAL || operator == CONTAINS) {
			return true;
		}
		return false;
	}
	
	private static boolean arrayContains(byte[] a, byte[] b) {
		if (a.length < b.length) {
			return false;
		}
		byte[] equalLengthArray = new byte[b.length];
		for(int i = 0; i <= a.length - b.length; i++) {
			System.arraycopy(a, i, equalLengthArray, 0, b.length);
			if (Arrays.equals(equalLengthArray, b)) {
				return true;
			}
		}
		return false;
	}
}
