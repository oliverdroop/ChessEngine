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
	
	List<Square> getAvailableMoves(List<Piece> allPieces){
		List<MoveType> moveTypes = new ArrayList<>();
		if (type==PieceType.PAWN){
			moveTypes.add(MoveType.PAWN);		
		}
		if (type == PieceType.KING){
			moveTypes.add(MoveType.KING);
		}
		if (type==PieceType.QUEEN){
			moveTypes.add(MoveType.DIAGONAL);
			moveTypes.add(MoveType.ORTHOGONAL);
		}
		if (type==PieceType.ROOK){
			moveTypes.add(MoveType.ORTHOGONAL);
		}
		if (type==PieceType.BISHOP){
			moveTypes.add(MoveType.DIAGONAL);
		}
		if (type==PieceType.KNIGHT){
			moveTypes.add(MoveType.JUMP);
		}
		int i = moveTypes.size();
		List<Square> squares = board.squares;
		List<Square> output = new ArrayList<>();
		while(i > 0){
			MoveType moveType = moveTypes.get(i - 1);
			if (type != PieceType.QUEEN){
				squares = board.filterSquares(squares, board.getSquare(x, y), moveType, team, allPieces);
				output = squares;
			}
			else{
				List<Square> squares2 = board.filterSquares(squares, board.getSquare(x, y), moveType, team, allPieces);
				for(int i2 = 0; i2 < squares2.size(); i2++){
					output.add(squares2.get(i2));
				}
			}
			i--;
		}
		squares = new ArrayList<>();
		if (output.size() > 0){
			for(int i1 = 0; i1 < output.size(); i1++){
				Square square2 = output.get(i1);
				List<Piece> pieces2 = new ArrayList<>();
				for(int i2 = 0; i2 < allPieces.size(); i2++){
					Piece piece2 = allPieces.get(i2);
					Square square3 = board.getSquare(piece2.x, piece2.y);
					if (piece2 != this && square3 != square2){
						pieces2.add(piece2);					
					}
					else{
						pieces2.add(new PieceBuilder()
								.withBoard(board)
								.withTeam(team)
								.withType(type)
								.withX(square2.x)
								.withY(square2.y)
								.withHasMoved(true)
								.build());
					}
				}
				if (!board.check(team, pieces2)){
					squares.add(square2);
				}
			}
		}
		output = squares;
		return output;
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
}
