package crossword;

import java.util.Comparator;

public class ClueLengthComparator implements Comparator<Clue> {

	@Override
	public int compare(Clue clue1, Clue clue2) {
		Integer length1 = clue1.getLength();
		Integer length2 = clue2.getLength();
		return length1.compareTo(length2);
	}
}
