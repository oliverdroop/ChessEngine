package crossword;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class TemplateGenerator {
	
	private static final int MIN_CLUE_LENGTH = 3;
	
	public static Template generateTemplate(int size) {
		Grid grid = new Grid(size);
		Template template = new Template(grid);
		int blackoutCount = (int)Math.round(Math.pow(size, 2) / 8);
		blackOutRandomSquares(template, blackoutCount);
		template.setClues(template.generateClues());
//		if (template.getClues().stream().filter(clue -> clue.getLength() < 2).count() > 0) {
//			template = generateTemplate(size);
//		}
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
			int minClueLength = 0;
			while(x == null || !template.isBlack(x, y)) {
				x = rnd.nextInt(grid.getWidth());
				y = rnd.nextInt(grid.getHeight());
				template.setBlack(x, y);
				int xR = grid.getWidth() - x - 1;
				int yR = grid.getHeight() - y - 1;
				template.setBlack(xR, yR);
				List<Clue> possibleClues = template.generateClues();
				minClueLength = possibleClues.stream().min(new ClueLengthComparator()).get().getLength();
				if (minClueLength < MIN_CLUE_LENGTH || possibleClues.stream()
						.filter(clue -> Template.countIntersections(clue, possibleClues) < 2)
						.collect(Collectors.toList())
						.size() > 0) {
					template.setWhite(x, y);
					template.setWhite(xR, yR);
					x = null;
					y = null;
				}
			}
		}
	}
}
