package chess;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.core.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Move implements Comparable {
	private static final Logger LOGGER = LoggerFactory.getLogger(Move.class);
	protected Piece piece;
	protected Square startSquare;
	protected Square endSquare;
	protected Board board;
	private Piece pieceTaken;
	private double evaluation = 1;
	
	public Move(Piece piece, Square startSquare, Square endSquare) {
		this.piece = piece;
		this.startSquare = startSquare;
		this.endSquare = endSquare;
		this.board = piece.board;
		pieceTaken = board.getPiece(endSquare.x, endSquare.y, board.getPieces());
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
		if(adjacent()){
			return true;
		}
		Piece king = board.getKing(team, allPieces);
		if(king.hasMoved == false){
			if (startSquare.y == endSquare.y && !startSquare.threatened(team, allPieces)){
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
	
	public boolean blocked(Team team, List<Piece> allPieces){
		for(Piece piece : allPieces){
			if (piece != this.piece && piece.blocks(this)){
				return true;
			}				
		}
		Piece destPiece = board.getPiece(endSquare.x, endSquare.y, allPieces);
		if (destPiece != null && destPiece.team == team){
			return true;
		}
		return false;
	}
	
	public boolean selfCheck(List<Piece> piecesCurrently) {
		//List<Piece> futurePieceList = new ArrayList<>();
		Board newBoard = new Board(null);
		newBoard.setTurnTeam(board.getTurnTeam());
		Piece pieceToMove = null;
		for(Piece existingPiece : piecesCurrently){
			Piece newPiece = new PieceBuilder()
					.withBoard(newBoard)
					.withTeam(existingPiece.getTeam())
					.withType(existingPiece.getType())
					.withHasMoved(existingPiece.getHasMoved())
					.withX(existingPiece.getX())
					.withY(existingPiece.getY()).build();
			newBoard.getPieces().add(newPiece);
			if (existingPiece.equals(piece)) {
				pieceToMove = newPiece;
			}
			if (board.getEnPassantable() != null && board.getEnPassantable().equals(existingPiece)) {
				newBoard.setEnPassantable(newPiece);
			}
		}
		pieceToMove.move(newBoard.getSquare(endSquare.x, endSquare.y));
		if (newBoard.check(pieceToMove.getTeam(), newBoard.getPieces())) {
			return true;
		}
		return false;
	}
	
	public boolean isLegal(List<Piece> allPieces) {
		if (piece.threatens(endSquare, allPieces) && !selfCheck(allPieces)) {
			return true;
		}
		return false;
	}
	
	public Piece getPieceTaken() {
		return pieceTaken;
	}

	public Piece getPiece() {
		return piece;
	}

	public void setPiece(Piece piece) {
		this.piece = piece;
	}

	public Square getStartSquare() {
		return startSquare;
	}

	public void setStartSquare(Square startSquare) {
		this.startSquare = startSquare;
	}

	public Square getEndSquare() {
		return endSquare;
	}

	public void setEndSquare(Square endSquare) {
		this.endSquare = endSquare;
	}

	public Board getBoard() {
		return board;
	}

	public void setBoard(Board board) {
		this.board = board;
	}
	
	public double getEvaluation() {
		return evaluation;
	}

	public void setEvaluation(double evaluation) {
		this.evaluation = evaluation;
	}
	
	@Override
	public int compareTo(Object move){
		return ((Double) getEvaluation()).compareTo(((Move) move).getEvaluation());
	}

	@Override
	public String toString() {
		return piece.toString() + " " + startSquare.toString() + " " + endSquare.toString();
	}
}
