package chess;

import java.util.ArrayList;
import java.util.List;

public class Move {
	protected Piece piece;
	protected Square startSquare;
	protected Square endSquare;
	protected Board board;
	
	public Move(Piece piece, Square startSquare, Square endSquare) {
		this.piece = piece;
		this.startSquare = startSquare;
		this.endSquare = endSquare;
		this.board = piece.board;
	}
	
	public boolean diagonal(){
		if (startSquare == endSquare){
			return false;
		}
		int xOffsetSize = Math.abs(endSquare.x - startSquare.x);
		int yOffsetSize = Math.abs(endSquare.y - startSquare.y);
		if (xOffsetSize == yOffsetSize){
			return true;
		}
		return false;
	}

	public boolean orthogonal(){
		if (startSquare == endSquare){
			return false;
		}
		if (endSquare.x == startSquare.x || endSquare.y == startSquare.y){
			return true;
		}		
		return false;
	}

	public boolean adjacent(){
		if (startSquare == endSquare){
			return false;
		}
		int xOffset = endSquare.x - startSquare.x;
		int yOffset = endSquare.y - startSquare.y;
		if (Math.pow(xOffset, 2) < 2 && Math.pow(yOffset, 2) < 2){
			return true;
		}
		return false;
	}

	public boolean forward(Team team){
		int yOffset = endSquare.y - startSquare.y;
		if (yOffset > 0 && team.ordinal() == 0){
			return true;
		}
		if (yOffset < 0 && team.ordinal() == 1){
			return true;
		}
		return false;
	}

	public boolean jump(){
		if (startSquare == endSquare){
			return false;
		}
		int xOffsetSize = Math.abs(endSquare.x - startSquare.x);
		int yOffsetSize = Math.abs(endSquare.y - startSquare.y);
		if (xOffsetSize == 2 && yOffsetSize == 1){
			return true;
		}
		if (xOffsetSize == 1 && yOffsetSize == 2){
			return true;
		}
		return false;
	}
	
	public boolean pawnMove(Team team, List<Piece> allPieces){
		if (startSquare == endSquare){
			return false;
		}
		if (forward(team)){
			if (orthogonal() && board.getPiece(endSquare.x, endSquare.y, allPieces) == null){
				if (adjacent()){
					return true;
				}
				int yOffsetSize = Math.abs(endSquare.y - startSquare.y);
				if (yOffsetSize == 2 && board.getPiece(startSquare.x, startSquare.y, allPieces).hasMoved == false){
					return true;
				}
			}
			if(adjacent() && diagonal()){
				for(int i = 0; i < allPieces.size(); i++){
					Piece piece2 = allPieces.get(i);
					if (piece2.team != team){
						if (piece2.x == endSquare.x && piece2.y == endSquare.y){
							return true;
						}
						if (piece2.y == (3 + piece2.team.ordinal()) && endSquare.y == (5 - (team.ordinal() * 3)) && piece2.x == endSquare.x && adjacent() && board.enPassantable == piece2){
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	
	public boolean kingMove(Team team, List<Piece> allPieces){
		if (startSquare == endSquare){
			return false;
		}
		if(adjacent() && !endSquare.threatened(team, allPieces)){
			return true;
		}
		Piece king = board.getPiece(startSquare.x, startSquare.y, allPieces);
		if(king.hasMoved == false){
			if (startSquare.y == endSquare.y && !startSquare.threatened(team, allPieces) && !endSquare.threatened(team, allPieces)){
				Piece closeRook = board.getPiece(0, startSquare.y, allPieces);
				Square square3 = board.getSquare(1, startSquare.y);
				if(closeRook != null && closeRook.hasMoved == false && endSquare.x == 1 && !new Move(king, startSquare, square3).blocked(team, allPieces)){
					Square square5 = board.getSquare(2, startSquare.y);
					if (!square5.threatened(team, allPieces)){
						return true;
					}
				}
				Piece farRook = board.getPiece(7, startSquare.y, allPieces);
				Square square4 = board.getSquare(6, startSquare.y);
				if(farRook != null && farRook.hasMoved == false && endSquare.x == 5 && !new Move(king, startSquare, square4).blocked(team, allPieces)){
					Square square6 = board.getSquare(4, startSquare.y);
					if (!square6.threatened(team, allPieces)){
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public boolean blocks(CoordinateHolder squareTest){
		if (diagonal() || orthogonal()){
			double angle1 = board.findAngle(startSquare, endSquare);
			double angle2 = board.findAngle(startSquare, squareTest);
			double dist1 = board.findDistance(startSquare, endSquare);
			double dist2 = board.findDistance(startSquare, squareTest);
			if (angle1 == angle2 && dist1 > dist2){
				return true;
			}
		}
		return false;
	}
	
	public boolean blocked(Team team, List<Piece> allPieces){
		for(int i = 0; i < allPieces.size(); i++){
			Piece piece = allPieces.get(i);
			if (blocks(piece)){
				return true;
			}
			
		}
		Piece destPiece = board.getPiece(endSquare.x, endSquare.y, allPieces);
		if (destPiece != null && destPiece.team == team){
			return true;
		}
		return false;
	}
	
	public boolean resultsInCheck(List<Piece> allPieces) {
		List<Piece> futurePieceList = new ArrayList<>();
		for(Piece piece2 : allPieces){
			Square square3 = board.getSquare(piece2.x, piece2.y);
			if (piece2 != piece && square3 != endSquare){
				futurePieceList.add(piece2);					
			}
			else{
				futurePieceList.add(new PieceBuilder()
						.withBoard(board)
						.withTeam(piece.team)
						.withType(piece.type)
						.withX(endSquare.x)
						.withY(endSquare.y)
						.withHasMoved(true)
						.build());
			}
		}
		if (!board.check(piece.team, futurePieceList)){
			return false;
		}
		return true;
	}
	
	public boolean isLegal(List<Piece> allPieces) {
		boolean possible = false;
		if (piece.type == PieceType.KING && kingMove(piece.team, allPieces)) {
			possible = true;
		}
		if (piece.type == PieceType.QUEEN && (orthogonal() || diagonal())) {
			possible = true;
		}
		if (piece.type == PieceType.ROOK && orthogonal()) {
			possible = true;
		}
		if (piece.type == PieceType.BISHOP && diagonal()) {
			possible = true;
		}
		if (piece.type == PieceType.KNIGHT && diagonal()) {
			possible = true;
		}
		if (piece.type == PieceType.PAWN && pawnMove(piece.team, allPieces)) {
			possible = true;
		}
		if (possible && !blocked(piece.team, allPieces) && !resultsInCheck(allPieces)) {
			return true;
		}
		return false;
	}
}
