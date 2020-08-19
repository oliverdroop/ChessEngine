package database;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ResultFormatter {
	
	private static String inputColumnSeparator = "\\t";
	
	private static String outputColumnSeparator = "|";
	
	private static String outputLineSeparator = "\r\n";
	
	private static String horizontalPadding = " ";
	
	private static String corner = "+";
	
	private static String horizontalLine = "-";
	
	public static String formatResult(List<String> results, List<Column> columns, boolean showHeaders) {
		//Map<Column, DataType> dataTypeMap = getDataTypeMap(columns);
		if (showHeaders) {			
			results.add(0, getColumnNamesString(columns));
		}
		Map<Column, Integer> longestValueMap = getLongestValueMap(results, columns);
		Map<Column, String> formatMap = getFormatMap(longestValueMap);
		StringBuilder output = new StringBuilder();
		String horizontalLine = getHorizontalLine(longestValueMap);
		output.append(outputLineSeparator);
		output.append(horizontalLine);
		//for(String row : results) {
		for(int rowIndex = 0; rowIndex < results.size(); rowIndex++) {
			String row = results.get(rowIndex);
			if (showHeaders && rowIndex == 1) {
				output.append(horizontalLine);
			}
			String[] values = row.split(inputColumnSeparator, -1);
			output.append(outputColumnSeparator);
			for(int index = 0; index < columns.size(); index++) {
				output.append(horizontalPadding);
				output.append(String.format(formatMap.get(columns.get(index)), values[index]));
				output.append(horizontalPadding);
				output.append(outputColumnSeparator);
			}
			output.append(outputLineSeparator);
		}
		output.append(horizontalLine);
		return output.toString();
	}
	
	private static String getHorizontalLine(Map<Column, Integer> longestValueMap) {
		StringBuilder output = new StringBuilder();
		for(Column column : longestValueMap.keySet()) {
			output.append(corner);
			for(int i = longestValueMap.get(column) + (horizontalPadding.length()); i >= 0; i--) {
				output.append(horizontalLine);
			}
		}
		output.append(corner);
		output.append(outputLineSeparator);
		return output.toString();
	}
	
	private static String getColumnNamesString(List<Column> columns) {
		StringBuilder columnNames = new StringBuilder();
		for(int i = 0; i < columns.size() - 1; i++) {
			columnNames.append(columns.get(i).getName());
			columnNames.append("\t");
		}
		columnNames.append(columns.get(columns.size() - 1).getName());
		return columnNames.toString();
	}
	
	private static Map<Column, String> getFormatMap(Map<Column, Integer> longestValueMap){
		Map<Column, String> formatMap = new HashMap<>();
		for(Column column : longestValueMap.keySet()) {
			StringBuilder formatBuilder = new StringBuilder();
			boolean numeric = column.getDataType().isNumeric();
			formatBuilder.append("%");
			formatBuilder.append(numeric ? "" : "-");
			formatBuilder.append(String.valueOf(longestValueMap.get(column)));
			formatBuilder.append("s");
			formatMap.put(column, formatBuilder.toString());
		}
		return formatMap;
	}
	
	private static Map<Column, Integer> getLongestValueMap(List<String> results, List<Column> columns){
		Map<Column, Integer> output = new LinkedHashMap<>();
		for(String row : results) {
			String[] values = row.split(inputColumnSeparator, -1);
			for(int index = 0; index < columns.size(); index++) {
				Column column = columns.get(index);
				Integer maxLength = output.get(column);
				if (maxLength == null || maxLength < values[index].length()) {
					output.put(column, values[index].length());
				}
			}
		}
		return output;
	}
	
	private static Map<Column, DataType> getDataTypeMap(List<Column> columns){
		Map<Column, DataType> dataTypeMap = new HashMap<>();
		for(Column column : columns) {
			dataTypeMap.put(column, column.getDataType());
		}
		return dataTypeMap;
	}
}
