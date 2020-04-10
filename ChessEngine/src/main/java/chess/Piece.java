package chess;
import java.util.ArrayList;
import java.util.List;

public class Piece extends CoordinateHolder{
	protected Board board;
	protected Team team;
	protected boolean hasMoved;
	protected PieceType type;
	
	public void move(Square square) {
		double movementDistance = board.findDistance(this, square);
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
		if (type == PieceType.KING && movementDistance > 1){
			board.continueCastle(square, board.pieces);
		}
		if (board.game.wins(team)){
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
	
	public boolean threatens(Piece piece, Square square, List<Piece> allPieces){
		if (piece.type != PieceType.PAWN && piece.type != PieceType.KING){
			if (piece.getAvailableMoves(allPieces).contains(square)){
				return true;
			}
		}
		Square pieceSquare = new Square(board);
		pieceSquare.x = piece.x;
		pieceSquare.y = piece.y;
		Move move = new Move(piece, pieceSquare, square);
		if (piece.type == PieceType.KING){
			if (move.adjacent()){
				return true;
			}
		}
		if (piece.type == PieceType.PAWN){
			if (move.adjacent() && move.diagonal() && move.forward(piece.team)){
				return true;
			}
		}
		return false;
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
}
