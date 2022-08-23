package chess.old;

import java.util.ArrayList;
import java.util.List;

public class Square extends CoordinateHolder{
	private Board board;
	
	public Square(Board board) {
		this.board = board;
	}
	
	public boolean threatened(Team team, List<Piece> allPieces){
		for(int i = 0; i < allPieces.size(); i++){
			Piece piece2 = allPieces.get(i);
			if (piece2 != board.getPiece(x, y, allPieces) && piece2.team != team && piece2.threatens(this, allPieces)){
				return true;
			}
		}
		return false;
	}
	
	public List<Piece> getThreats(List<Piece> allPieces){
		List<Piece> threats = new ArrayList<>();
		for(int i = 0; i < allPieces.size(); i++){
			Piece piece = allPieces.get(i);
			if (piece.threatens(this, allPieces)){
				threats.add(piece);
			}
		}
		return threats;
	}
	
	@Override
	public String toString() {
		return "[" + String.valueOf(x) + " " + String.valueOf(y) + "]";
	}
}
