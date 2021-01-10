package crossword;

public class CrosswordBuilder {
	public static void main(String[] args) {
		Template template = TemplateGenerator.generateTemplate(9);
		TemplatePrinter.printTemplate(template);
	}
}
