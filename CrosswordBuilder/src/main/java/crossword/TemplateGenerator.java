package crossword;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class TemplateGenerator {
	
	private static final int MIN_CLUE_LENGTH = 3;
	
	public static Template generateTemplate(int size) {
		Grid grid = new Grid(size);
		Template template = new Template(grid);
		int blackoutCount = (int)Math.round(Math.pow(size, 2) / 10);
		blackOutRandomSquares(template, blackoutCount);
		template.setClues(template.generateClues());
		TemplatePrinter.printTemplate(template);
		TemplateFiller.fillTemplate(template, new Dictionary());
		return template;
	}
	
	private static void blackOutRandomSquares(Template template, int blackouts) {
		Grid grid = template.getGrid();
		Random rnd = new Random();
		for(int i = 0; i < blackouts; i++) {
			Integer x = null;
			Integer y = null;
			while(x == null) {
				x = rnd.nextInt(grid.getWidth());
				y = rnd.nextInt(grid.getHeight());
				int xR = grid.getWidth() - x - 1;
				int yR = grid.getHeight() - y - 1;
				boolean isBlack = template.isBlack(x, y);
				template.setColour(x, y, !isBlack);
				template.setColour(xR, yR, !isBlack);
				if (!isValidTemplate(template)) {
					template.setColour(x, y, isBlack);
					template.setColour(xR, yR, isBlack);
					x = null;
					y = null;
				}
			}
		}
	}
	
	private static boolean isValidTemplate(Template template) {
		List<Clue> possibleClues = template.generateClues();
		int minClueLength = possibleClues.stream().min(new ClueLengthComparator()).get().getLength();
		return minClueLength >= MIN_CLUE_LENGTH && possibleClues.stream()
				.filter(clue -> Template.countIntersections(clue, possibleClues) < 2)
				.collect(Collectors.toList())
				.size() == 0;
	}
}
