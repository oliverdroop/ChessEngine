package crossword;

import java.util.Optional;

public class TemplateGenerator {
	
	public static Template generateTemplate() {
		Grid grid = new Grid(5);
		Template template = new Template(grid);
		TemplateFiller.fillTemplate(template, new Dictionary());
		return template;
	}
	
	public static void printTemplate(Template template) {
		int w = template.getGrid().getWidth();
		int h = template.getGrid().getHeight();
		for(int y = 0; y < h + 2; y++) {
			int gridY = y - 1;
			for(int x = 0; x < w + 2; x++) {
				int gridX = x - 1;
				String placeholder = "██";
				if (x > 0 && x < w + 1 && y > 0 && y < h + 1) {
					Optional<Integer> clueNumber = template.getClueNumber(gridX, gridY);
					String clueNumberString = clueNumber.isPresent() ? String.valueOf(clueNumber.get()) : " ";
					
					Optional<Character> optChar = template.getCharacter(gridX, gridY);
					char charString = optChar.isPresent() ? optChar.get() : ' ';
					
					String square = clueNumberString + charString;
					placeholder = template.isBlack(gridX, gridY) ? "██" : square.toUpperCase();
				}
				System.out.print(placeholder);
			}			
			System.out.println();
		}
	}
	
	public static void main(String[] args) {
		Template template = generateTemplate();
		printTemplate(template);
	}
}
