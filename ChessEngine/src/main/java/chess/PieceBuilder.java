package chess;

public class PieceBuilder {
	private Board board;
	private int x;
	private int y;
	private Team team;
	private PieceType type;
	private boolean hasMoved;
	
	public PieceBuilder withBoard(Board board) {
		this.board = board;
		return this;
	}
	
	public PieceBuilder withX(int x) {
		this.x = x;
		return this;
	}
	
	public PieceBuilder withY(int y) {
		this.y = y;
		return this;
	}
	
	public PieceBuilder withTeam(Team team) {
		this.team = team;
		return this;
	}
	
	public PieceBuilder withType(PieceType type) {
		this.type = type;
		return this;
	}
	
	public PieceBuilder withHasMoved(boolean hasMoved) {
		this.hasMoved = hasMoved;
		return this;
	}
	
	public Piece build() {
		Piece piece = new Piece();
		piece.x = x;
		piece.y = y;
		piece.team = team;
		piece.type = type;
		piece.hasMoved = hasMoved;
		return piece;
	}
}
