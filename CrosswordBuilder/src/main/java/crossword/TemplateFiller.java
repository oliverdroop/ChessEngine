package crossword;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TemplateFiller {
	
	public static void fillTemplate(Template template, Dictionary dictionary) {
		List<Clue> sortedClues = template.getClues();
		Collections.shuffle(sortedClues);
		sortedClues.sort(new ClueLengthComparator());
		
		boolean filled = true;
		List<String> usedWords = new ArrayList<>();
		for(int i = sortedClues.size() - 1; i >= 0 ; i--) {
			Clue clue = sortedClues.get(i);
			String answer = null;
			while(answer == null || usedWords.contains(answer) == false) {
				try {
					answer = dictionary.getRandomWordToFit(clue.getLength(), template.getCheckers(clue));
				}
				catch(NoWordFoundException e) {
					//System.out.println(e.getMessage());
					filled = false;
					break;
				}
				usedWords.add(answer);
			}
			clue.setAnswer(answer);
		}
		if (!filled) {
			template.clearClueAnswers();
			fillTemplate(template, dictionary);
		}
	}	
}
