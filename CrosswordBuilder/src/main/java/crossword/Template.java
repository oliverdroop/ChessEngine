package crossword;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Template {
	
	private Grid grid;

	private boolean[][] blackSquares;
	
	private List<Clue> clues;
	
	public Template(Grid grid) {
		this.grid = grid;
		blackSquares = new boolean[grid.getWidth()][grid.getHeight()];
		for(int x = 0; x < grid.getWidth(); x++) {
			for(int y = 0; y < grid.getHeight(); y++) {
				blackSquares[x][y] = true;
				if (x % 2 == 0) {
					blackSquares[x][y] = false;
				}
				if (y % 2 == 0) {
					blackSquares[x][y] = false;
				}
			}
		}
	}
	
	public List<Clue> generateClues(){
		List<Clue> clues = new ArrayList<>();
		int nextClueNumber = 0;
		for(int y = 0; y < grid.getHeight(); y++) {			
			for(int x = 0; x < grid.getWidth(); x++) {				
				Map<Direction, Integer> directionLengths = getClueDirectionLengths(x, y);
				if (directionLengths.isEmpty()) {
					continue;
				}
				
				for(Direction direction : directionLengths.keySet()) {
					nextClueNumber++;
					if (directionLengths.size() == 2 && direction == Direction.DOWN) {
						nextClueNumber--;
					}
					clues.add(new Clue(x, y, nextClueNumber, direction, directionLengths.get(direction)));
				}
			}
		}
		return clues;
	}
	
	public Optional<Integer> getClueNumber(int x, int y) {
		for(Clue clue : clues) {
			if (clue.getStartX() != x || clue.getStartY() != y) {
				continue;
			}
			return Optional.of(clue.getNumber());
		}
		return Optional.empty();
	}
	
	public Map<Integer, Character> getCheckers(Clue clue){
		Map<Integer, Character> checkers = new HashMap<>();
		int x = clue.getStartX();
		int y = clue.getStartY();
		for(int pos = 0; pos < clue.getLength(); pos++) {
			Optional<Character> c = Optional.empty();
			if (clue.getDirection() == Direction.ACROSS) {
				c = getCharacter(x + pos, y);
			} else {
				c = getCharacter(x, y + pos);
			}
			if (c.isPresent()) {
				checkers.put(pos, c.get());
			}
		}
		return checkers;
	}
	
	public Optional<Character> getCharacter(int x, int y) {
		for(Clue clue : clues) {
			int clueX = clue.getStartX();
			int clueY = clue.getStartY();
			if (clue.getAnswer() == null || (clueX != x && clueY != y)) {
				continue;
			}
			int posX = x - clueX;
			if (clue.getDirection() == Direction.ACROSS && y == clueY && posX >= 0 && posX < clue.getLength()) {
				return Optional.of(clue.getAnswer().charAt(posX));
			}
			int posY = y - clueY;
			if (clue.getDirection() == Direction.DOWN && x == clueX && posY >= 0 && posY < clue.getLength()) {
				return Optional.of(clue.getAnswer().charAt(posY));
			}
		}
		return Optional.empty();
	}
	
	public static int countIntersections(Clue clue, List<Clue> clues) {
		int count = 0;
		for(int i = 0; i < clue.getLength(); i++) {
			Map<Direction, Integer> coordinates = clue.getCoordinatesAtPosition(i);
			for(Clue clue2 : clues) {
				if (clue2 == clue) {
					continue;
				}
				for(int i2 = 0; i2 < clue.getLength(); i2++) {
					Map<Direction, Integer> coordinates2 = clue2.getCoordinatesAtPosition(i2);
					if (coordinates.get(Direction.ACROSS).equals(coordinates2.get(Direction.ACROSS))
							&& coordinates.get(Direction.DOWN).equals(coordinates2.get(Direction.DOWN))) {
						count++;
					}
				}
			}
		}
		return count;
		
	}
	
	private Map<Direction, Integer> getClueDirectionLengths(int x, int y){
		Map<Direction, Integer> directionLengths = new LinkedHashMap<>();
		if (blackSquares[x][y]) {
			return directionLengths;
		}
		
		int posX = x;
		while(posX < grid.getWidth() && !isBlack(posX, y) && (x == 0 || isBlack(x - 1, y))) {			
			posX++;
		}
		if (posX - x > 1) {
			directionLengths.put(Direction.ACROSS, posX - x);
		}
		
		int posY = y;
		while(posY < grid.getHeight() && !isBlack(x, posY) && (y == 0 || isBlack(x, y - 1))) {			
			posY++;
		}
		if (posY - y > 1) {
			directionLengths.put(Direction.DOWN, posY - y);
		}
		return directionLengths;
	}
	
	public int getHighestClueNumber() {
		return clues.stream().max(new ClueNumberComparator()).get().getNumber();
	}
	
	private class ClueNumberComparator implements Comparator<Clue> {
		@Override
		public int compare(Clue clue1, Clue clue2) {
			return Integer.compare(clue1.getNumber(), clue2.getNumber());
		}
	}
	
	public void clearClueAnswers() {
		clues.forEach(clue -> clue.setAnswer(null));
	}
	
	public Grid getGrid() {
		return grid;
	}

	public void setGrid(Grid grid) {
		this.grid = grid;
	}
	
	public boolean isBlack(int x, int y) {
		return blackSquares[x][y];
	}
	
	public void setBlack(int x, int y) {
		blackSquares[x][y] = true;
	}
	
	public void setWhite(int x, int y) {
		blackSquares[x][y] = false;
	}

	public void setBlackSquares(boolean[][] blackSquares) {
		this.blackSquares = blackSquares;
	}

	public List<Clue> getClues() {
		return clues;
	}

	public void setClues(List<Clue> clues) {
		this.clues = clues;
	}
	
}
