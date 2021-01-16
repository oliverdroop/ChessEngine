package crossword;

public class CrosswordBuilder {
	public static void main(String[] args) {
		Template template = TemplateGenerator.generateTemplate(10);
		TemplatePrinter.printTemplate(template);
	}
}
