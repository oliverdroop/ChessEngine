package crossword;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import javafx.util.Pair;

public class TemplateGenerator {
	
	private static final int MIN_CLUE_LENGTH = 3;
	
	public static Template generateTemplate(int size) {
		Grid grid = new Grid(size);
		Template template = new Template(grid);
		blackOutEverything(template);
		
		Random rnd = new Random();
		int count = 0;
		while(count < (grid.getWidth() * grid.getHeight()) / 2) {
			int x = rnd.nextInt(grid.getWidth() - MIN_CLUE_LENGTH);
			int y = rnd.nextInt(grid.getHeight() - MIN_CLUE_LENGTH);
			int xR = grid.getWidth() - x - 1;
			int yR = grid.getHeight() - y - 1;
			int dir = (int) Math.round(rnd.nextDouble());
			Direction direction = dir == 0 ? Direction.ACROSS : Direction.DOWN;
			
			List<Point> newlyWhitened = new ArrayList<>();
			if (direction == Direction.ACROSS) {
				int length = rnd.nextInt(grid.getWidth() - MIN_CLUE_LENGTH - x) + 2;
				while(length > 0) {
					if (template.isBlack(x, y)) {
						newlyWhitened.add(new Point(x, y));
						newlyWhitened.add(new Point(xR, yR));
					}
					template.setColour(x, y, false);
					template.setColour(xR, yR, false);
					x++;
					xR--;
					length--;
				}
			}
			
			if (direction == Direction.DOWN) {
				int length = rnd.nextInt(grid.getHeight() - MIN_CLUE_LENGTH - y) + 2;
				while(length > 0) {
					if (template.isBlack(x, y)) {
						newlyWhitened.add(new Point(x, y));
						newlyWhitened.add(new Point(xR, yR));
					}
					template.setColour(x, y, false);
					template.setColour(xR, yR, false);
					y++;
					yR--;
					length--;
				}
			}
			
			if (validateMinimumClueLength(template) && validateAdjacency(template)) {
				count++;
			} else {
				newlyWhitened.forEach(point -> template.setColour(point.x, point.y, true));
			}
		}
		template.setClues(template.generateClues());
		TemplatePrinter.printTemplate(template);
		TemplateFiller.fillTemplate(template, new Dictionary());
		
		return template;
	}
	
	private static void blackOutEverything(Template template) {
		Grid grid = template.getGrid();
		template.setBlackSquares(new boolean[grid.getWidth()][grid.getHeight()]);
		for(int x = 0; x < grid.getWidth(); x++) {
			for(int y = 0; y < grid.getHeight(); y++) {
				template.setColour(x, y, true);
			}
		}
	}
	
	private static boolean isValidTemplate(Template template) {
		return validateMinimumClueLength(template) 
				&& validateIntersections(template) 
				&& validateAdjacency(template);
	}
	
	private static boolean validateMinimumClueLength(Template template) {
		List<Clue> possibleClues = template.generateClues();
		return !possibleClues.stream()
				.anyMatch(clue -> clue.getLength() < MIN_CLUE_LENGTH);
	}
	
	private static boolean validateIntersections(Template template) {
		List<Clue> possibleClues = template.generateClues();
		return !possibleClues.stream()
				.anyMatch(clue -> Template.countIntersections(clue, possibleClues) < Math.floor(clue.getLength() / 2));
	}
	
	private static boolean validateAdjacency(Template template) {
		List<Clue> possibleClues = template.generateClues();
		return !possibleClues.stream()
				.anyMatch(clue -> clue.isParallelAndAdjacentToAny(possibleClues));
	}
}
