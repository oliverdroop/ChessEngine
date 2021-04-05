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
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Compressor {
	
	public static final Logger LOGGER = LoggerFactory.getLogger(Compressor.class);
	
	public static final String FILE_EXTENSION = ".dcp";
	
	public static void main(String[] args) {
		long t1 = System.currentTimeMillis();
		LOGGER.info("Starting Compressor");
		if (args != null && args.length > 0) {
			String arg0 = args[0];
			if (arg0.equals("-h") || arg0.equals("--help")) {
				LOGGER.info("Use arguments -c <file path> to compress your file into {} format", FILE_EXTENSION);
				LOGGER.info("Use arguments -u <file path> to uncompress your file from {} format", FILE_EXTENSION);
			} else if (arg0.equals("-c") && args.length == 2) {
				String path = args[1];
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
		
		Map<byte[], byte[]> palette = new HashMap<>();
		boolean compressing = true;
		while(compressing) {
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
					
					//assign a bytes saved value to each pattern: high values are more compressible
					int bytesSavedPerPattern = pattern.length - lowestUnfeaturedSequence.length;
					int patternDefinitionLength = 2 + lowestUnfeaturedSequence.length + pattern.length;
					int totalBytesSavedForPattern = (patternLocations.size() * bytesSavedPerPattern) - patternDefinitionLength;
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
			if (majorityPattern == null) {
				compressing = false;
				LOGGER.info("Finished compressing");
				break;
			}
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
		}
		byte[] paletteDefinition = getPalette(palette);
		byte[] fileBytes = new byte[paletteDefinition.length + bytes.length + fileExtension.length + 1];
		fileBytes[0] = (byte)fileExtension.length;
		int currentPos = 1;
		System.arraycopy(fileExtension, 0, fileBytes, currentPos, fileExtension.length);
		currentPos += fileExtension.length;
		System.arraycopy(paletteDefinition, 0, fileBytes, currentPos, paletteDefinition.length);
		currentPos += paletteDefinition.length;
		System.arraycopy(bytes, 0, fileBytes, currentPos, bytes.length);
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
			//stringBuilder.append((char) b);
			stringBuilder.append(String.format("%02X ", b));
		}
		return stringBuilder.toString();
	}
}
