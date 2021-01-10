package crossword;

public class Clue {
	
	private int number;
	
	private Direction direction;
	
	private String text = "";
	
	private int startX;
	
	private int startY;
	
	private int length;
	
	private String answer;
	
	public Clue(int x, int y, int number, Direction direction, int length) {
		this.startX = x;
		this.startY = y;
		this.number = number;
		this.direction = direction;
		this.length = length;
	}

	public int getNumber() {
		return number;
	}

	public Direction getDirection() {
		return direction;
	}

	public String getText() {
		return text;
	}

	public void setText(String clueText) {
		this.text = clueText;
	}

	public int getStartX() {
		return startX;
	}

	public int getStartY() {
		return startY;
	}

	public int getLength() {
		return length;
	}

	public String getAnswer() {
		return answer;
	}

	public void setAnswer(String answer) {
		this.answer = answer;
	}
	
}
