package crossword;

public class Grid {
	
	private int width;
	
	private int height;
	
	public Grid(int width, int height) {
		this.width = width;
		this.height = height;
	}
	
	public Grid(int size) {
		this.width = size;
		this.height = size;
	}

	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
}
