package chess;
import java.util.ArrayList;
import java.util.List;

public class Piece extends CoordinateHolder{
	protected Board board;
	protected Team team;
	protected boolean hasMoved;
	protected PieceType type;
	
	public void move(Square square) {
		double movementDistance = findDistance(this, square);
		if (type == PieceType.PAWN){
			if (movementDistance == 2){
				board.enPassantable = this;
			}
			else{
				if (movementDistance == Math.sqrt(2)){
					board.tryEnPassant(square, board.pieces);
				}
				board.enPassantable = null;
			}
			board.halfmoveClock = -1;
		}
		else{
			board.enPassantable = null;
		}
		Piece takeablePiece = board.getPiece(square.x, square.y, board.pieces);
		if (takeablePiece != null) {
			board.pieces.remove(takeablePiece);
			board.halfmoveClock = 0;
		}
		x = square.x;
		y = square.y;
		hasMoved = true;
		if (type == PieceType.KING && movementDistance == 2){
			board.continueCastle(square, board.pieces);
		}
		if (type == PieceType.PAWN && y == 7 * (1 - team.ordinal())) {
			convertPawn();
		}
		if (board.game != null && board.game.wins(team)){
			if (team.ordinal() == 0){
				System.out.println("White wins!");
			}
			else{
				System.out.println("Black wins!");
			}
		}
		if (team == Team.BLACK) {
			board.fullmoveNumber ++;
		}
		board.halfmoveClock ++;
		board.turnTeam = Team.values()[1 - board.turnTeam.ordinal()];
	}
	
	public List<Square> getAvailableMoves(List<Piece> allPieces){
		List<Square> availableMoves = new ArrayList<>();
		for(Square square : board.squares) {
			Move move = new Move(this, getSquare(), square);
			if (move.isLegal(allPieces)) {
				availableMoves.add(square);
			}
		}
		return availableMoves;
	}
	
	public boolean threatens(Square square, List<Piece> allPieces) {
		boolean possible = false;
		Move move = new Move(this, getSquare(), square);
		if (type == PieceType.KING && move.kingMove(team, allPieces)) {
			possible = true;
		}
		if (type == PieceType.QUEEN && (move.orthogonal() || move.diagonal())) {
			possible = true;
		}
		if (type == PieceType.ROOK && move.orthogonal()) {
			possible = true;
		}
		if (type == PieceType.BISHOP && move.diagonal()) {
			possible = true;
		}
		if (type == PieceType.KNIGHT && move.jump()) {
			possible = true;
		}
		if (type == PieceType.PAWN && move.pawnMove(team, allPieces)) {
			possible = true;
		}
		if (possible && !move.blocked(team, allPieces)) {
			return true;
		}
		return false;
	}
	
	public void convertPawn() {
		this.setType(PieceType.QUEEN);
	}
	
	public char getFEN() {
		char c = type.toString().charAt(0);
		if (type == PieceType.KNIGHT) {
			c = 'N';
		}
		if (team == Team.BLACK) {
			c = (char)((byte)c + 32);
		}
		return c;
	}
	
	public Square getSquare() {
		return board.getSquare(x, y);
	}
	
	@Override
	public String toString() {
		return team.toString()
				.concat(" ")
				.concat(type.toString())
				.concat(" ")
				.concat(String.valueOf(hasMoved))
				.concat(" ")
				.concat(String.valueOf(x))
				.concat(" ")
				.concat(String.valueOf(y));
	}
	
	public int getValue() {
		if (type == PieceType.PAWN) {
			return 1;
		}
		if (type == PieceType.KNIGHT || type == PieceType.BISHOP) {
			return 3;
		}
		if (type == PieceType.ROOK) {
			return 5;
		}
		if (type == PieceType.QUEEN) {
			return 9;
		}
		//king
		return 400;
	}
	
	public Team getOpposingTeam() {
		return Team.values()[1 - team.ordinal()];
	}

	public Board getBoard() {
		return board;
	}

	public void setBoard(Board board) {
		this.board = board;
	}

	public Team getTeam() {
		return team;
	}

	public void setTeam(Team team) {
		this.team = team;
	}

	public boolean getHasMoved() {
		return hasMoved;
	}

	public void setHasMoved(boolean hasMoved) {
		this.hasMoved = hasMoved;
	}

	public PieceType getType() {
		return type;
	}

	public void setType(PieceType type) {
		this.type = type;
	}
}
