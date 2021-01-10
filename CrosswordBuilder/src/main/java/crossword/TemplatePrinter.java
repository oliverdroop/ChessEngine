package crossword;

import java.util.Optional;

public class TemplatePrinter {
	public static void printTemplate(Template template) {
		int w = template.getGrid().getWidth();
		int h = template.getGrid().getHeight();

		String placeholder = "█";
		while(placeholder.length() <= String.valueOf(template.getHighestClueNumber()).length()) {
			placeholder = placeholder + placeholder.charAt(0);
		}
		
		for(int y = 0; y < h + 2; y++) {
			int gridY = y - 1;
			for(int x = 0; x < w + 2; x++) {
				int gridX = x - 1;
				String square = placeholder;
				if (x > 0 && x < w + 1 && y > 0 && y < h + 1) {
					Optional<Integer> clueNumber = template.getClueNumber(gridX, gridY);
					String clueNumberString = clueNumber.isPresent() ? String.valueOf(clueNumber.get()) : " ";
					while(clueNumberString.length() < placeholder.length() - 1) {
						clueNumberString = clueNumberString + " ";
					}
					
					Optional<Character> optChar = template.getCharacter(gridX, gridY);
					char charString = optChar.isPresent() ? optChar.get() : ' ';
					
					square = clueNumberString + charString;
					square = template.isBlack(gridX, gridY) ? placeholder : square.toUpperCase();
				}
				System.out.print(square);
			}			
			System.out.println();
		}
	}
}
