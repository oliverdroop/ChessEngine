import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.BufferOverflowException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Compressor {
	
	public static final Logger LOGGER = LoggerFactory.getLogger(Compressor.class);
	
	public static final String FILE_EXTENSION = ".dcp";
	
	public static void main(String[] args) {
		long t1 = System.currentTimeMillis();
		LOGGER.info("Starting Compressor");
		if (args != null && args.length > 0) {
			String path = args[0];
			byte[] bytes = loadFile(path);
			bytes = compress(bytes);
			writeBytes(getPathWithNewExtension(path), bytes);
			
			bytes = uncompress(bytes);
			writeBytes(path, bytes);
		} else {
			LOGGER.warn("No file to compress");
		}
		long t2 = System.currentTimeMillis();
		LOGGER.info("Compressor finished in {} milliseconds", t2 - t1);
	}
	
	public static byte[] compress(byte[] bytes) {
		
		Map<byte[], byte[]> palette = new HashMap<>();
		int passes = 4;
		while(passes > 0) {
			//find a short key to use for replacing a pattern
			byte[] lowestUnfeaturedSequence = getLowestUnfeaturedSequence(bytes);
			LOGGER.debug("Lowest unfeatured sequence is {}", getByteString(lowestUnfeaturedSequence));
			
			//find patterns
			int patternLengthLimit = 16;
			patternLengthLimit = Math.min(patternLengthLimit, bytes.length);
			Map<byte[], Integer> patternsSearched = new HashMap<>();
			for(int pl = 2; pl <= patternLengthLimit; pl++) {
				for(int i = 0; i <= bytes.length - pl; i++) {
					//define pattern
					byte[] pattern = Arrays.copyOfRange(bytes, i, i + pl);
					//make sure we're not searching for the same pattern more than once
					if (patternsSearched.keySet().stream().anyMatch(ar -> Arrays.equals(ar, pattern))) {
						continue;
					}
					//make sure the pattern we're counting doesn't contain any sequences already in the palette
					if (palette.values().stream().anyMatch(ar -> isPatternContained(ar, pattern))) {
						continue;
					}
					
					List<Integer> patternLocations = getPatternLocations(pattern, bytes);
					//find out what percentage of the file each pattern constitutes
					double percentageOfFile = 0;
					if (!patternLocations.isEmpty()) {
						percentageOfFile = (pattern.length * patternLocations.size()) / (double)bytes.length;
						double roundedPercentage = Math.round(percentageOfFile * 10000) / (double)100;
						LOGGER.debug("Found pattern {} at {} location(s) ({}%)", getByteString(pattern), patternLocations.size(), roundedPercentage);
					}
					
					//assign a value to each pattern: high values are more compressible
					double patternCompressionFactor = pattern.length / (double)lowestUnfeaturedSequence.length;
					int bytesSavedPerPattern = pattern.length - lowestUnfeaturedSequence.length;
					int totalBytesSavedForPattern = patternLocations.size() * bytesSavedPerPattern;
					if (totalBytesSavedForPattern <= 0) {
						continue;
					}
					LOGGER.debug("Replacing pattern {} with {} at {} locations saves {} bytes", getByteString(pattern), 
							getByteString(lowestUnfeaturedSequence), patternLocations.size(), totalBytesSavedForPattern);
					patternsSearched.put(pattern, totalBytesSavedForPattern);
				}
			}
			
			//get the pattern with the highest value and add it to the palette
			byte[] majorityPattern = getMajorityPattern(patternsSearched);
			palette.put(majorityPattern, lowestUnfeaturedSequence);
			LOGGER.info("Added to palette : {} maps to {}", getByteString(majorityPattern), getByteString(lowestUnfeaturedSequence));
			
			//rebuild file data using palette
			List<Byte> outputList = new ArrayList<>();
			List<Integer> patternLocations = getPatternLocations(majorityPattern, bytes);
			int i = 0;
			while(i < bytes.length) {
				if (patternLocations.contains(i)) {
					for(byte b : lowestUnfeaturedSequence) {
						outputList.add(b);
					}
					i += majorityPattern.length;
				} else {
					outputList.add(bytes[i]);
					i++;
				}
			}
			//convert the output list of bytes to a byte array
			byte[] output = new byte[outputList.size()];
			for(int index = 0; index < outputList.size(); index++) {
				output[index] = outputList.get(index);
			}
			bytes = output;
			passes--;
		}
		byte[] paletteDefinition = getPalette(palette);
		byte[] fileBytes = new byte[paletteDefinition.length + bytes.length];
		System.arraycopy(paletteDefinition, 0, fileBytes, 0, paletteDefinition.length);
		System.arraycopy(bytes, 0, fileBytes, paletteDefinition.length, bytes.length);
		return fileBytes;
	}
	
	public static byte[] uncompress(byte[] bytes) {
		LOGGER.info("Rebuilding file from compressed file of length {}", bytes.length);
		int patternCount = (int) bytes[0];
		LOGGER.info("Palette contains {} patterns", patternCount);
		//first build the palette
		Map<byte[], byte[]> palette = new HashMap<>();
		int currentPos = 1;
		for(int pidx = 0; pidx < patternCount; pidx++) {
			int placeholderLength = (int)bytes[currentPos];
			currentPos++;
			byte[] placeholder = new byte[placeholderLength];
			System.arraycopy(bytes, currentPos, placeholder, 0, placeholderLength);
			currentPos += placeholderLength;
			
			int patternLength = (int)bytes[currentPos];
			currentPos++;
			byte[] pattern = new byte[patternLength];
			System.arraycopy(bytes, currentPos, pattern, 0, patternLength);
			currentPos += patternLength;
			
			palette.put(placeholder, pattern);
		}
		LOGGER.info("Constructed palette of {} patterns", palette.size());
		
		//rebuild file
		List<Byte> outputList = new ArrayList<>();
		while(currentPos < bytes.length) {
			boolean replace = false;
			for(byte[] placeholder : palette.keySet()) {
				if (patternAppearsAt(placeholder, bytes, currentPos)) {
					byte[] pattern = palette.get(placeholder);
					LOGGER.debug("Replacing placeholder {} with pattern {} at position {} in output file", 
							getByteString(placeholder), getByteString(pattern), outputList.size());
					for(byte b : pattern) {
						outputList.add(b);
					}
					currentPos += placeholder.length;
					replace = true;
					break;
				}
			}
			if (!replace) {
				outputList.add(bytes[currentPos]);
				currentPos++;
			}
		}
		//convert output list to array
		byte[] output = new byte[outputList.size()];
		for(int i = 0; i < outputList.size(); i++) {
			output[i] = outputList.get(i);
		}
		return output;
	}
	
	private static boolean patternAppearsAt(byte[] pattern, byte[] bytes, int index) {
		if (index > bytes.length - pattern.length) {
			return false;
		}
		return Arrays.equals(pattern, Arrays.copyOfRange(bytes, index, index + pattern.length));
	}
	
	private static List<Integer> getPatternLocations(byte[] pattern, byte[] bytes) {
		int i = 0;
		List<Integer> locations = new ArrayList<>();
		if (pattern.length > bytes.length) {
			return locations;
		}
		while(i <= bytes.length - pattern.length) {
			if (patternAppearsAt(pattern, bytes, i)) {
				locations.add(i);
				i += pattern.length;
			} else {
				i++;
			}
		}
		return locations;
	}
	
	private static boolean isPatternContained(byte[] pattern, byte[] bytes) {
		int i = 0;
		while(i <= bytes.length - pattern.length) {
			if (patternAppearsAt(pattern, bytes, i)) {
				return true;
			} else {
				i++;
			}
		}
		return false;
	}
	
	private static byte[] getLowestUnfeaturedSequence(byte[] bytes) {
		boolean result = false;
		int length = 1;
		byte[] test = new byte[length];
		Arrays.fill(test, Byte.MIN_VALUE);
		while(!result) {
			if(isPatternContained(test, bytes)) {
				try {
					LOGGER.debug("Attempting increment of {}", getByteString(test));
					test = increment(test);
				} catch (BufferOverflowException e) {
					length++;
					test = new byte[length];
					Arrays.fill(test, Byte.MIN_VALUE);
				}
			} else {
				result = true;
				return test;
			}
		}
		return null;
	}
	
	private static byte[] increment(byte[] bytes) throws BufferOverflowException {
		int i = bytes.length - 1;
		boolean overflow = false;
		byte[] output = null;
		while(!overflow && output == null) {
			if (bytes[i] < Byte.MAX_VALUE) {
				bytes[i] ++;
				LOGGER.debug("Incremented byte to {}", getByteString(bytes));
				output = bytes;
			} else {
				bytes[i] = Byte.MIN_VALUE;
				i --;
				if (i < 0) {
					overflow = true;
				}
			}
		}
		if (output != null) {
			return output;
		}
		throw new BufferOverflowException();
	}
	
	private static byte[] getMajorityPattern(Map<byte[], Integer> patterns) {
		double highest = 0;
		byte[] majorityPattern = null;
		for(byte[] pattern : patterns.keySet()) {
			double value = patterns.get(pattern);
			if (value > highest) {
				highest = value;
				majorityPattern = pattern;
			}
		}
		return majorityPattern;
	}
	
	private static byte[] getPalette(Map<byte[], byte[]> palette) {
		List<Byte> outputList = new ArrayList<>();
		outputList.add((byte)palette.keySet().size());
		for(byte[] pattern : palette.keySet()) {
			byte[] placeholder = palette.get(pattern);
			byte patternLength = (byte) pattern.length;
			byte placeholderLength = (byte) placeholder.length;
			outputList.add(placeholderLength);
			for(byte b : placeholder) {
				outputList.add(b);
			}
			outputList.add(patternLength);
			for(byte b : pattern) {
				outputList.add(b);
			}
		}
		byte[] output = new byte[outputList.size()];
		for(int i = 0; i < outputList.size(); i++) {
			output[i] = outputList.get(i);
		}
		return output;
	}
	
	public static void writeBytes(String path, byte[] bytes) {
		try {
			File file = new File(path);
    		Path actualPath = file.toPath();
			Files.write(actualPath, bytes);
    		LOGGER.info("Saved {} bytes to path {}", bytes.length, path);
		} catch (IOException e) {
			LOGGER.warn("Unable to write bytes : {}", e.getMessage());
		}
	}
	
	public static byte[] loadFile(String path) {
		byte[] data = null;
		try {
    		File file = new File(path);
    		Path actualPath = file.toPath();
			data = Files.readAllBytes(actualPath);
    		LOGGER.info("Loaded {} bytes", data.length);
    	}
    	catch(IOException e) {
    		LOGGER.warn("Unable to load file : {}", e.getMessage());
    	}
		return data;
	}
	
	private static String getPathWithNewExtension(String path) {
		if (path == null || path.isEmpty()) {
			LOGGER.warn("No existing path");
			return null;
		}
		int dotPosition = path.lastIndexOf(".");
		if (dotPosition < 0) {
			LOGGER.warn("No existing file extension found in path {}", path);
			return path.concat(FILE_EXTENSION);
		}
		return path.substring(0, dotPosition).concat(FILE_EXTENSION);
	}
	
	public static void logBytes(byte[] bytes) {
		logBytes(bytes, 32);
	}
	
	public static void logBytes(byte[] bytes, int lineLength) {
		int linePos = 0;
		int i = 0;
		StringBuilder stringBuilder = new StringBuilder();
		while(i < bytes.length) {
			stringBuilder.append(String.format("%02X ", bytes[i]));
			linePos++;
			i++;
			if (linePos >= lineLength) {
				linePos = 0;
				LOGGER.info(stringBuilder.toString());
				stringBuilder = new StringBuilder();
			}
		}
		LOGGER.info(stringBuilder.toString());
	}
	
	private static String getByteString(byte[] bytes) {
		StringBuilder stringBuilder = new StringBuilder();
		for(byte b : bytes) {
			//stringBuilder.append((char) b);
			stringBuilder.append(String.format("%02X ", b));
		}
		return stringBuilder.toString();
	}
}
