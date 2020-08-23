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
	
	public static String formatResult(List<String> results) {
		Map<Integer, Integer> longestValueMap = getLongestValueMap(results);
		Map<Integer, String> formatMap = getFormatMap(longestValueMap);
		StringBuilder output = new StringBuilder();
		String horizontalLine = getHorizontalLine(longestValueMap);
		output.append(outputLineSeparator);
		output.append(horizontalLine);
		for(int rowIndex = 0; rowIndex < results.size(); rowIndex++) {
			String row = results.get(rowIndex);
			if (rowIndex == 1) {
				output.append(horizontalLine);
			}
			String[] values = row.split(inputColumnSeparator, -1);
			output.append(outputColumnSeparator);
			for(int index = 0; index < countColumns(results); index++) {
				output.append(horizontalPadding);
				output.append(String.format(formatMap.get(index), values[index]));
				output.append(horizontalPadding);
				output.append(outputColumnSeparator);
			}
			output.append(outputLineSeparator);
		}
		output.append(horizontalLine);
		return output.toString();
	}
	
	private static String getHorizontalLine(Map<Integer, Integer> longestValueMap) {
		StringBuilder output = new StringBuilder();
		for(int index : longestValueMap.keySet()) {
			output.append(corner);
			for(int i = longestValueMap.get(index) + (horizontalPadding.length()); i >= 0; i--) {
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
	
	private static Map<Integer, String> getFormatMap(Map<Integer, Integer> longestValueMap){
		Map<Integer, String> formatMap = new HashMap<>();
		for(int index : longestValueMap.keySet()) {
			StringBuilder formatBuilder = new StringBuilder();
			formatBuilder.append("%");
			formatBuilder.append("-");
			formatBuilder.append(String.valueOf(longestValueMap.get(index)));
			formatBuilder.append("s");
			formatMap.put(index, formatBuilder.toString());
		}
		return formatMap;
	}
	
	private static int countColumns(List<String> results) {
		return results.get(0).split(inputColumnSeparator).length;
	}
	
	private static Map<Integer, Integer> getLongestValueMap(List<String> results){
		Map<Integer, Integer> output = new LinkedHashMap<>();
		for(String row : results) {
			String[] values = row.split(inputColumnSeparator, -1);
			for(int index = 0; index < countColumns(results); index++) {
				Integer maxLength = output.get(index);
				if (maxLength == null || maxLength < values[index].length()) {
					output.put(index, values[index].length());
				}
			}
		}
		return output;
	}
}
