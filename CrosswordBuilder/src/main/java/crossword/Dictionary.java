package crossword;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class Dictionary {
	
	List<String> words;
	
	public Dictionary() {
		words = loadWords();
	}
	
	public List<String> loadWords() {
		List<String> words = new ArrayList<>();
		String rootDirectory = System.getProperty("user.dir");
		String path = rootDirectory + "\\src\\main\\resources\\dictionary";
	    try {
	    	BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
	    	String in = "";
			while ((in = reader.readLine()) != null) {
			    words.add(in);
			}
		    reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
		return words;
	}
	
	public String getRandomWord(int length) {
		List<String> correctLengthWords = words.stream()
				.filter(word -> word.length() == length)
				.collect(Collectors.toList());
		return correctLengthWords.get(new Random().nextInt(correctLengthWords.size()));
	}
	
	public String getRandomWordToFit(int length, Map<Integer, Character> checkers) throws NoWordFoundException{
		List<String> fittingWords = words.stream()
				.filter(word -> word.length() == length)
				.collect(Collectors.toList());
		for(Integer cPos : checkers.keySet()) {
			fittingWords = fittingWords.stream()
					.filter(word -> ((Character)word.charAt(cPos)).equals(checkers.get(cPos)))
					.collect(Collectors.toList());
		}
		if (fittingWords.size() == 0) {
			throw new NoWordFoundException("No word found for checkers");
		}
		return fittingWords.get(new Random().nextInt(fittingWords.size()));
	}

	public List<String> getWords() {
		return words;
	}
	
}
