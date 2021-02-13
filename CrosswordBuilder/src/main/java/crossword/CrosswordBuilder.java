package crossword;

public class CrosswordBuilder {
	public static void main(String[] args) {
		Template template = TemplateGenerator.generateTemplate(11);
		TemplatePrinter.printTemplate(template);
	}
}
