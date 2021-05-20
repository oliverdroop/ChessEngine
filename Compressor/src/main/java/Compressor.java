import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.logging.log4j.core.util.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Compressor {
	
	public static final Logger LOGGER = LoggerFactory.getLogger(Compressor.class);
	
	public static final String FILE_EXTENSION = ".dcp";
	
	public static final Charset CHARSET = Charset.forName("ISO-8859-1");
	
	public static void main(String[] args) {
		long t1 = System.currentTimeMillis();
		LOGGER.info("Starting Compressor");
		System.setProperty("file.encoding", "ISO-8859-1");
		if (args != null && args.length > 0) {
			String arg0 = args[0];
			if (arg0.equals("-h") || arg0.equals("--help")) {
				LOGGER.info("Use arguments -c <file path> to compress your file into {} format", FILE_EXTENSION);
				LOGGER.info("Use arguments -u <file path> to uncompress your file from {} format", FILE_EXTENSION);
			} else if (arg0.equals("-c") && args.length == 2) {
				String path = args[1];
				//Map<Long, Byte> longBytes = loadFile2(path);
				byte[] bytes = loadFile(path);
				byte[] fileExtension = getFileExtension(path);
				bytes = compress(bytes, fileExtension);
				writeBytes(getPathWithoutExtension(path).concat(FILE_EXTENSION), bytes);
			} else if (arg0.equals("-u") && args.length == 2) {
				String path = args[1];
				byte[] bytes = loadFile(path);
				Map<byte[], String> uncompressedFile = uncompress(bytes);
				byte[] uncompressedBytes = uncompressedFile.keySet().stream().findFirst().get();
				String extension = uncompressedFile.values().stream().findFirst().get();
				writeBytes(getPathWithoutExtension(path).concat(extension), uncompressedBytes);
			}
		} else {
			LOGGER.warn("No file to compress or uncompress");
		}
		long t2 = System.currentTimeMillis();
		LOGGER.info("Compressor finished in {} milliseconds", t2 - t1);
	}
	
	public static byte[] compress(byte[] bytes, byte[] fileExtension) {
		Map<String, byte[]> palette = new HashMap<>();
		int maxPatternLength = 8;
		Map<String, int[]> patternLocations = new HashMap<>();
		Map<String, Integer> patternValues = new HashMap<>();
		List<String> consideredPatterns = new ArrayList<>();
		int[][] patternStarts = new int[65536][];
		//Search for patterns
		int searchPercentage = 0;
		int blockSize = (int)Math.round((bytes.length) / (double) 100);
		int count = 0;
		System.out.print("Searching : 0%\r");

		for(int i = 0; i <= bytes.length - 2; i++) {
			
			//Report how far through the file we've searched
			count++;
			if (count > blockSize) {
				count = 0;
				searchPercentage++;
				System.out.print(String.format("Searching : %d%%\r", searchPercentage));
			}
			
			int pl = 2;
			int[] locations = new int[0];
			while (pl < maxPatternLength) {
				byte[] pattern = Arrays.copyOfRange(bytes, i, i + pl);
				String patternString = getString(pattern);
				if (pl == 2 && patternStarts[getUnsignedShort(pattern)] != null) {
					pl++;
					continue;
				}
				if (patternValues.containsKey(patternString)) {
					pl++;
					continue;
				}
				if (locations.length <= 0) {
					//Find locations for shortest pattern at i
					locations = getPatternLocations(pattern, bytes, i);
					if (pl == 2) {
						patternStarts[getUnsignedShort(pattern)] = locations;
					}
				} else if (locations.length > 1) {
					//Filter locations for longer versions of the short pattern
					int[] filteredLocations = Arrays.copyOf(locations, locations.length);
					int previousPatternLength = pl - 1;
					locations = Arrays.stream(locations)
							.filter(loc -> testDifferenceFromPrevious(filteredLocations, loc, pattern.length))
							.filter(loc -> patternContinuesAt(pattern, bytes, loc, previousPatternLength))
							.toArray();
				}
				if (locations.length < 2) {
					break;
				}
				//Assign a value to each pattern (high values mean more compression)
				int value = pl * (locations.length - 1);
				//Add pattern, value and locations to the map(s)
				int maxPatternValues = 4096;
				if (patternValues.size() < maxPatternValues) {
					patternValues.put(patternString, value);
					patternLocations.put(patternString, locations);
				} else  {
					//Trim out patterns with low values to keep maps and memory usage small
					String trimmablePattern = getMinorityPattern(patternValues);
					if (value > patternValues.get(trimmablePattern)) {
						patternValues.remove(trimmablePattern);
						patternLocations.remove(trimmablePattern);
						patternValues.put(patternString, value);
						patternLocations.put(patternString, locations);
					}
				}
				pl++;
			}
		}
		System.out.println();
		

		//Build a palette of patterns and placeholders
		int passes = 256;
		consideredPatterns = new ArrayList<>();
		byte[] placeholder = getLowestUnfeaturedSequence(bytes, palette.values());
		while(passes > 0) {
			String pattern = getMajorityPattern(patternValues, consideredPatterns);
			passes--;
			if (pattern == null) {
				continue;
			}
			byte[] patternBytes = getBytes(pattern);
			consideredPatterns.add(pattern);
			LOGGER.debug("Found pattern {} at {} locations", getByteString(patternBytes), patternLocations.get(pattern).length);
			
			//Define a placeholder for the pattern and calculate the number of bytes saved
			int bytesSavedPerPattern = patternBytes.length - placeholder.length;
			int patternDefinitionLength = 2 + placeholder.length + patternBytes.length;
			int totalBytesSavedForPattern = (patternLocations.get(pattern).length * bytesSavedPerPattern) - patternDefinitionLength;
			if (totalBytesSavedForPattern <= 0) {
				continue;
			}
			LOGGER.info("Replacing pattern {} with {} at {} locations saves {} bytes", getByteString(patternBytes), 
					getByteString(placeholder), patternLocations.get(pattern).length, totalBytesSavedForPattern);
			
			//Remove overlapping patterns
			for(String unusablePattern : getUnusablePatterns(patternLocations, patternBytes, patternLocations.get(pattern))) {
				byte[] unusablePatternBytes = getBytes(unusablePattern);
				if (!palette.containsKey(unusablePattern)) {
					patternLocations.remove(unusablePattern);
					patternValues.remove(unusablePattern);
					LOGGER.debug("Removed overlapping pattern {}", getByteString(unusablePatternBytes));
				} else {
					LOGGER.warn("Tried to remove palette contained in pattern");
				}
			}
			palette.put(pattern, placeholder);
			placeholder = getLowestUnfeaturedSequence(bytes, palette.values());
		}
		
		//Validate by checking for inadvertent placeholder usage
		List<byte[]> consideredPlaceholders = new ArrayList<>();
		consideredPlaceholders.addAll(palette.values());
		byte[] output = null;
		boolean validating = true;
		loop:
		while (validating) {
			List<Byte> outputBytes = new ArrayList<>();
			
			//Build an output list of bytes
			int i = 0;
			while(i < bytes.length) {
				boolean isPatternLocation = false;
				//Replace patterns with their placeholders
				for(String pattern : palette.keySet()) {
					int[] locations = patternLocations.get(pattern);
					if (Arrays.binarySearch(locations, i) >= 0) {
						isPatternLocation = true;
						for(byte b : palette.get(pattern)) {
							outputBytes.add(b);
						}
						i += pattern.length();
						break;
					}
				}
				//Copy non-pattern bytes like-for-like
				if (!isPatternLocation) {
					outputBytes.add(bytes[i]);
					i++;
				}
			}
			output = new byte[outputBytes.size()];
			for(int j = 0; j < output.length; j++) {
				output[j] = outputBytes.get(j);
			}

			//Check for inadvertent placeholder usage
			for(String pattern : palette.keySet()) {
				placeholder = palette.get(pattern);
				int placeholderCount = getPatternLocations(placeholder, output, 0).length;
				int patternCount = patternLocations.get(pattern).length;
				if (placeholderCount != patternCount) {
					LOGGER.info("Couldn't use placeholder {} because use count {} didn't match pattern count {}", 
							getByteString(placeholder), placeholderCount, patternCount);
					placeholder = getLowestUnfeaturedSequence(bytes, consideredPlaceholders);
					consideredPlaceholders.add(placeholder);
					palette.put(pattern, placeholder);
					continue loop;
				}
			}
			validating = false;
		}
		
		//Define the palette
		byte[] paletteDefinition = getPaletteDefinition(palette);
		byte[] fileBytes = new byte[paletteDefinition.length + output.length + fileExtension.length + 1];
		//Define the file extension
		fileBytes[0] = (byte)fileExtension.length;
		int currentPos = 1;
		System.arraycopy(fileExtension, 0, fileBytes, currentPos, fileExtension.length);
		currentPos += fileExtension.length;
		//Add the palette definition
		System.arraycopy(paletteDefinition, 0, fileBytes, currentPos, paletteDefinition.length);
		currentPos += paletteDefinition.length;
		//Add the compressed data
		System.arraycopy(output, 0, fileBytes, currentPos, output.length);
		//Report the compression factor
		double roundedCompressionFactor = Math.round((bytes.length / (double)fileBytes.length) * 100) / (double) 100;
		LOGGER.info("File compressed by a factor of {}", roundedCompressionFactor);
		return fileBytes;
	}
	
	public static Map<byte[], String> uncompress(byte[] bytes) {
		LOGGER.info("Rebuilding file from compressed file of length {}", bytes.length);
		int fileExtensionLength = (int) bytes[0];
		int currentPos = 1;
		byte[] fileExtensionBytes = Arrays.copyOfRange(bytes, currentPos, currentPos + fileExtensionLength);
		currentPos += fileExtensionLength;
		String fileExtension = getFileExtension(fileExtensionBytes);
		
		int patternCount = (int) bytes[currentPos];
		if (patternCount < 0) {
			patternCount += 256;
		}
		currentPos++;
		LOGGER.info("Palette contains {} patterns", patternCount);
		//first build the palette
		Map<byte[], byte[]> palette = new HashMap<>();
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
		byte[] outputBytes = new byte[outputList.size()];
		for(int i = 0; i < outputList.size(); i++) {
			outputBytes[i] = outputList.get(i);
		}
		Map<byte[], String> output = new HashMap<>();
		output.put(outputBytes, fileExtension);
		return output;
	}
	
	private static List<String> getUnusablePatterns(Map<String, int[]> patternLocations, byte[] pattern, int[] locations) {
		List<String> unusablePatterns = new ArrayList<>();
		for(String otherPattern : patternLocations.keySet()) {
			if (otherPattern.equals(getString(pattern))) {
				continue;
			}
			loop:
			for(int location : patternLocations.get(otherPattern)) {
				for(int location2 : locations) {
					if ((location >= location2 && location < location2 + pattern.length)
							|| (location2 >= location && location2 < location + otherPattern.length())) {
						unusablePatterns.add(otherPattern);
						break loop;
					}
				}
			}
		}
		return unusablePatterns;
	}
	
	private static boolean patternAppearsAt(byte[] pattern, byte[] bytes, int index) {
		if (index > bytes.length - pattern.length) {
			return false;
		}
		for(int i = 0; i < pattern.length; i++) {
			if (pattern[i] != bytes[index + i]) {
				return false;
			}
		}
		return true;
	}
	
	private static boolean patternContinuesAt(byte[] pattern, byte[] bytes, int index, int prefixLength) {
		if (index > bytes.length - pattern.length || prefixLength >= pattern.length) {
			return false;
		}
		for(int i = prefixLength; i < pattern.length; i++) {
			if (pattern[i] != bytes[index + i]) {
				return false;
			}
		}
		return true;
	}
	
	private static int[] getPatternLocations(byte[] pattern, byte[] bytes, int startIndex) {
		int i = startIndex;
		List<Integer> locations = new ArrayList<>();
		if (pattern.length > bytes.length) {
			return new int[0];
		}
		while(i <= bytes.length - pattern.length) {
			if (patternAppearsAt(pattern, bytes, i)) {
				locations.add(i);
				i += pattern.length;
			} else {
				i++;
			}
		}
		return locations.stream().mapToInt(n->n).toArray();
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
	
	private static byte[] getLowestUnfeaturedSequence(byte[] bytes, Collection<byte[]> placeholders) {
		//Find the shortest and lowest unused sequence of bytes for use as a placeholder
		boolean result = false;
		int length = 1;
		byte[] test = new byte[length];
		Arrays.fill(test, Byte.MIN_VALUE);
		while(!result) {
			if(isPatternContained(test, bytes) || isContained(test, placeholders)) {
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
	
	private static boolean testDifferenceFromPrevious(int[] integers, int testInteger, int minimumDifference) {
		int index = Arrays.binarySearch(integers, testInteger);
		if (index <= 0) {
			return index == 0;
		}
		return testInteger - integers[index - 1] >= minimumDifference;
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
	
	private static String getMajorityPattern(Map<String, Integer> patterns, Collection<String> existingPatterns) {
		double highest = 0;
		String majorityPattern = null;
		for(String pattern : patterns.keySet()) {
			int value = patterns.get(pattern);
			if (value > highest && !existingPatterns.contains(pattern)) {
				highest = value;
				majorityPattern = pattern;
			}
		}
		if (majorityPattern != null) {
			LOGGER.debug("Found majority pattern {} with score {}", getByteString(getBytes(majorityPattern)), highest);
		}
		return majorityPattern;
	}
	
	private static String getMinorityPattern(Map<String, Integer> patterns) {
		double lowest = Double.MAX_VALUE;
		String minorityPattern = null;
		for(String pattern : patterns.keySet()) {
			int value = patterns.get(pattern);
			if (value < lowest) {
				lowest = value;
				minorityPattern = pattern;
			}
		}
		if (minorityPattern != null) {
			LOGGER.debug("Found minority pattern {} with score {}", getByteString(getBytes(minorityPattern)), lowest);
		}
		return minorityPattern;
	}
	
	private static byte[] getPaletteDefinition(Map<String, byte[]> palette) {
		List<Byte> outputList = new ArrayList<>();
		outputList.add((byte)palette.keySet().size());
		for(String pattern : palette.keySet()) {
			byte[] placeholder = palette.get(pattern);
			byte patternLength = (byte) pattern.length();
			byte placeholderLength = (byte) placeholder.length;
			outputList.add(placeholderLength);
			for(byte b : placeholder) {
				outputList.add(b);
			}
			outputList.add(patternLength);
			for(byte b : getBytes(pattern)) {
				outputList.add(b);
			}
		}
		byte[] output = new byte[outputList.size()];
		for(int i = 0; i < outputList.size(); i++) {
			output[i] = outputList.get(i);
		}
		return output;
	}
	
	private static boolean isContained(byte[] byteArray, Collection<byte[]> byteArrays) {
		return byteArrays.stream().anyMatch(ba -> Arrays.equals(ba, byteArray));
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
	
	public static Map<Long, Byte> loadFile2(String path) {
		Map<Long, Byte> data = new HashMap<>();
		try (InputStream stream = new FileInputStream(path)){
			long index = 0;
			int in = 0;
			while((in = stream.read()) >= 0) {
				byte b = (byte)in;
				data.put(index, b);
				index++;
			}
			LOGGER.info("Long loader loaded {} bytes", index);
		} catch(IOException e) {
    		LOGGER.warn("Unable to load file : {}", e.getMessage());
    	}
		return data;
	}
	
	private static String getPathWithoutExtension(String path) {
		if (path == null || path.isEmpty()) {
			LOGGER.warn("No existing path");
			return null;
		}
		int dotPosition = path.lastIndexOf(".");
		if (dotPosition < 0) {
			LOGGER.warn("No existing file extension found in path {}", path);
			return path;
		}
		return path.substring(0, dotPosition);
	}
	
	private static byte[] getFileExtension(String path) {
		int dotPosition = path.lastIndexOf('.');
		String extension = path.substring(dotPosition + 1);
		return extension.getBytes();
	}
	
	private static String getFileExtension(byte[] extensionBytes) {
		StringBuilder extensionBuilder = new StringBuilder();
		extensionBuilder.append('.');
		for(byte b : extensionBytes) {
			extensionBuilder.append((char) b);
		}
		return extensionBuilder.toString();
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
			stringBuilder.append(String.format("%02X ", b));
		}
		return stringBuilder.toString();
	}
	
	private static String getString(byte[] bytes) {
		return CHARSET.decode(ByteBuffer.wrap(bytes)).toString();
	}
	
	private static byte[] getBytes(String string) {
		return CHARSET.encode(string).array();
	}
	
	private static int getUnsignedShort(byte[] bytes) {
		if (bytes.length != 2) {
			return -1;
		}
		int ubyte0 = bytes[0] < 0 ? bytes[0] + 256 : bytes[0];
		int ubyte1 = bytes[1] < 0 ? bytes[1] + 256 : bytes[1];
		return (ubyte0 * 256) + ubyte1;
	}
	
	private static long getLong(byte[] bytes) {
		byte[] newBytes = new byte[8];
		System.arraycopy(bytes, 0, newBytes, 8 - bytes.length, bytes.length);
		return ByteBuffer.wrap(newBytes).getLong();
	}
	
	private static byte[] getBytes(long num) {
		int lz = Long.numberOfLeadingZeros(num) / 8;
		byte[] ar8 = ByteBuffer.allocate(8).putLong(num).array();
		return Arrays.copyOfRange(ar8, Math.min(6, lz), 8);
	}
}
