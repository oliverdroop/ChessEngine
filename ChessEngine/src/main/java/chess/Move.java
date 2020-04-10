package chess;

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
		if (startSquare != endSquare){
			int xOffsetSize = Math.abs(endSquare.x - startSquare.x);
			int yOffsetSize = Math.abs(endSquare.y - startSquare.y);
			if (xOffsetSize == yOffsetSize){
				return true;
			}
		}
		return false;
	}

	public boolean orthogonal(){
		if (startSquare != endSquare){
			if (endSquare.x == startSquare.x || endSquare.y == startSquare.y){
				return true;
			}
		}
		return false;
	}

	public boolean adjacent(){
		if (startSquare != endSquare){
			int xOffset = endSquare.x - startSquare.x;
			int yOffset = endSquare.y - startSquare.y;
			if (Math.pow(xOffset, 2) < 2 && Math.pow(yOffset, 2) < 2){
				return true;
			}
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
		if (startSquare != endSquare){
			int xOffsetSize = Math.abs(endSquare.x - startSquare.x);
			int yOffsetSize = Math.abs(endSquare.y - startSquare.y);
			if (xOffsetSize == 2 && yOffsetSize == 1){
				return true;
			}
			if (xOffsetSize == 1 && yOffsetSize == 2){
				return true;
			}
		}
		return false;
	}
	
	public boolean pawnMove(Team team, List<Piece> allPieces){
		if (startSquare != endSquare){
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
		}
		return false;
	}
	
	public boolean kingMove(Team team, List<Piece> allPieces){
		if (startSquare != endSquare){
			if(adjacent() && !board.threatened(endSquare, team, allPieces)){
				return true;
			}
			if(board.getPiece(startSquare.x, startSquare.y, allPieces).hasMoved == false){
				if (startSquare.y == endSquare.y && !board.threatened(startSquare, team, allPieces) && !board.threatened(endSquare, team, allPieces)){
					Piece closeRook = board.getPiece(0, startSquare.y, allPieces);
					Square square3 = board.getSquare(1, startSquare.y);
					if(closeRook != null && closeRook.hasMoved == false && endSquare.x == 1 && !blocked(startSquare, square3, team, allPieces)){
						Square square5 = board.getSquare(2, startSquare.y);
						if (!board.threatened(square5, team, allPieces)){
							return true;
						}
					}
					Piece farRook = board.getPiece(7, startSquare.y, allPieces);
					Square square4 = board.getSquare(6, startSquare.y);
					if(farRook != null && farRook.hasMoved == false && endSquare.x == 5 && !blocked(startSquare, square4, team, allPieces)){
						Square square6 = board.getSquare(4, startSquare.y);
						if (!board.threatened(square6, team, allPieces)){
							return true;
						}
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
	
	public boolean blocked(Square square1, Square square2, Team team, List<Piece> allPieces){
		for(int i = 0; i < allPieces.size(); i++){
			Piece piece = allPieces.get(i);
			if (blocks(piece)){
				return true;
			}
			
		}
		Piece destPiece = board.getPiece(square2.x, square2.y, allPieces);
		if (destPiece != null && destPiece.team == team){
			return true;
		}
		return false;
	}
}
