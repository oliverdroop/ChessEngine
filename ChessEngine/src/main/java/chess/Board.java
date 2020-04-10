package chess;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class Board {
	protected Game game;
	protected List<Square> squares = new ArrayList<>();
	protected List<Piece> pieces = new ArrayList<>();
	protected Piece enPassantable = null;
	protected int halfmoveClock = 0;
	protected int fullmoveNumber = 1;
	protected Team turnTeam;
	
	public Board(Game game) {
		this.game = game;
		createSquares();
	}
	
	public Square getSquare(int x, int y){
		for(int i = 0; i < squares.size(); i++){
			Square square = squares.get(i);
			if (positionMatch(square, x, y)){
				return square;
			}
		}
		return null;
	}
	
	public boolean positionMatch(Square square, int x, int y){
		if (square.x == x && square.y == y){
			return true;
		}
		return false;
	}
	
	public Piece getPiece(int x, int y, List<Piece> allPieces){
		for(int i = 0; i < allPieces.size(); i++){
			Piece piece = allPieces.get(i);
			if (piece.x == x && piece.y == y){
				return piece;
			}
		}
		return null;
	}
	
	public void createSquares(){
		for(int x = 0; x < 8; x++){
			for(int y = 0; y < 8; y++){
				Square square = new Square();
					square.x = x;
					square.y = y;
				squares.add(square);
			}
		}
	}
	
	public boolean diagonal(CoordinateHolder square1, CoordinateHolder square2){
		if (square1 != square2){
			int xOffsetSize = Math.abs(square2.x - square1.x);
			int yOffsetSize = Math.abs(square2.y - square1.y);
			if (xOffsetSize == yOffsetSize){
				return true;
			}
		}
		return false;
	}

	public boolean orthogonal(CoordinateHolder square1, CoordinateHolder square2){
		if (square1 != square2){
			if (square2.x == square1.x || square2.y == square1.y){
				return true;
			}
		}
		return false;
	}

	public boolean adjacent(CoordinateHolder square1, CoordinateHolder square2){
		if (square1 != square2){
			int xOffset = square2.x - square1.x;
			int yOffset = square2.y - square1.y;
			if (Math.pow(xOffset, 2) < 2 && Math.pow(yOffset, 2) < 2){
				return true;
			}
		}
		return false;
	}

	public boolean forward(CoordinateHolder square1, CoordinateHolder square2, Team team){
		int yOffset = square2.y - square1.y;
		if (yOffset > 0 && team.ordinal() == 0){
			return true;
		}
		if (yOffset < 0 && team.ordinal() == 1){
			return true;
		}
		return false;
	}

	public boolean jump(Square square1, Square square2){
		if (square1 != square2){
			int xOffsetSize = Math.abs(square2.x - square1.x);
			int yOffsetSize = Math.abs(square2.y - square1.y);
			if (xOffsetSize == 2 && yOffsetSize == 1){
				return true;
			}
			if (xOffsetSize == 1 && yOffsetSize == 2){
				return true;
			}
		}
		return false;
	}
	
	public boolean pawnMove(Square square1, Square square2, Team team, List<Piece> allPieces){
		if (square1 != square2){
			if (forward(square1, square2, team)){
				if (orthogonal(square1, square2) && getPiece(square2.x, square2.y, allPieces) == null){
					if (adjacent(square1, square2)){
						return true;
					}
					int yOffsetSize = Math.abs(square2.y - square1.y);
					if (yOffsetSize == 2 && getPiece(square1.x, square1.y, allPieces).hasMoved == false){
						return true;
					}
				}
				if(adjacent(square1, square2) && diagonal(square1, square2)){
					for(int i = 0; i < allPieces.size(); i++){
						Piece piece2 = allPieces.get(i);
						if (piece2.team != team){
							if (piece2.x == square2.x && piece2.y == square2.y){
								return true;
							}
							if (piece2.y == (3 + piece2.team.ordinal()) && square2.y == (5 - (team.ordinal() * 3)) && piece2.x == square2.x && adjacent(square1, piece2) && enPassantable == piece2){
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}
	
	public boolean kingMove(Square square1, Square square2, Team team, List<Piece> allPieces){
		if (square1 != square2){
			if(adjacent(square1, square2) && !threatened(square2, team, allPieces)){
				return true;
			}
			if(getPiece(square1.x, square1.y, allPieces).hasMoved == false){
				if (square1.y == square2.y && !threatened(square1, team, allPieces) && !threatened(square2, team, allPieces)){
					Piece closeRook = getPiece(0, square1.y, allPieces);
					Square square3 = getSquare(1, square1.y);
					if(closeRook != null && closeRook.hasMoved == false && square2.x == 1 && !blocked(square1, square3, team, allPieces)){
						Square square5 = getSquare(2, square1.y);
						if (!threatened(square5, team, allPieces)){
							return true;
						}
					}
					Piece farRook = getPiece(7, square1.y, allPieces);
					Square square4 = getSquare(6, square1.y);
					if(farRook != null && farRook.hasMoved == false && square2.x == 5 && !blocked(square1, square4, team, allPieces)){
						Square square6 = getSquare(4, square1.y);
						if (!threatened(square6, team, allPieces)){
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	
	public boolean blocked(Square square1, Square square2, Team team, List<Piece> allPieces){
		for(int i = 0; i < allPieces.size(); i++){
			Piece piece = allPieces.get(i);
			if (blocks(piece, square1, square2)){
				return true;
			}
			
		}
		Piece destPiece = getPiece(square2.x, square2.y, allPieces);
		if (destPiece != null && destPiece.team == team){
			return true;
		}
		return false;
	}
	
	public boolean blocks(CoordinateHolder squareTest, Square square1, Square square2){
		if (diagonal(square1, square2) || orthogonal(square1, square2)){
			double angle1 = findAngle(square1, square2);
			double angle2 = findAngle(square1, squareTest);
			double dist1 = findDistance(square1, square2);
			double dist2 = findDistance(square1, squareTest);
			if (angle1 == angle2 && dist1 > dist2){
				return true;
			}
		}
		return false;
	}
	
	public boolean threatened(Square square1, Team team, List<Piece> allPieces){
		for(int i = 0; i < allPieces.size(); i++){
			Piece piece2 = allPieces.get(i);
			if (piece2 != getPiece(square1.x, square1.y, allPieces) && piece2.team != team && threatens(piece2, square1, allPieces)){
				return true;
			}
		}
		return false;
	}
	
	public boolean threatens(Piece piece, Square square, List<Piece> allPieces){
		if (piece.type != PieceType.PAWN && piece.type != PieceType.KING){
			if (piece.getAvailableMoves(allPieces).contains(square)){
				return true;
			}
		}
		if (piece.type == PieceType.KING){
			if (adjacent(piece, square)){
				return true;
			}
		}
		if (piece.type == PieceType.PAWN){
			if (adjacent(piece, square) && diagonal(piece, square) && forward(piece, square, piece.team)){
				return true;
			}
		}
		return false;
	}
	
	public List<Piece> getThreats(Square square, List<Piece> allPieces){
		List<Piece> threats = new ArrayList<>();
		for(int i = 0; i < allPieces.size(); i++){
			Piece piece = allPieces.get(i);
			if (threatens(piece, square, allPieces)){
				threats.add(piece);
			}
		}
		return threats;
	}
	
	public boolean check(Team team, List<Piece> allPieces){
		Piece king = getKing(team, allPieces);
		Team otherTeam = Team.values()[1 - team.ordinal()];
		for(int i = 0; i < allPieces.size(); i++){
			Piece piece = allPieces.get(i);
			Square square1 = getSquare(piece.x, piece.y);
			Square square2 = getSquare(king.x, king.y);
			if (piece.team == otherTeam && !blocked(square1, square2, otherTeam, allPieces)){
				if (piece.type == PieceType.QUEEN || piece.type == PieceType.ROOK){
					if (orthogonal(square1, square2)){
						return true;
					}
				}
				if (piece.type == PieceType.QUEEN || piece.type == PieceType.BISHOP){
					if (diagonal(square1, square2)){
						return true;
					}
				}
				if (piece.type == PieceType.KNIGHT){
					if (jump(square1, square2)){
						return true;
					}
				}
				if (piece.type == PieceType.KING){
					if (adjacent(square1, square2)){
						return true;
					}
				}
				if (piece.type == PieceType.PAWN){
					if (adjacent(square1, square2) && forward(square1, square2, otherTeam) && diagonal(square1, square2)){
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public Piece getKing(Team team, List<Piece> allPieces){
		for(int i = 0; i < allPieces.size(); i++){
			Piece piece = allPieces.get(i);
			if (piece.type == PieceType.KING && piece.team == team){
				return piece;
			}
		}
		return null;
	}
	
	public void continueCastle(Square square, List<Piece> allPieces){
		if (square.x == 1){
			Square square2 = getSquare(2, square.y);
			Piece rook = getPiece(0, square.y, allPieces);
			rook.x = square2.x;
			rook.y = square2.y;
		}
		if (square.x == 5){
			Square square2 = getSquare(4, square.y);
			Piece rook = getPiece(7, square.y, allPieces);
			rook.x = square2.x;
			rook.y = square2.y;
		}
	}
	
	public void tryEnPassant(Square square, List<Piece> allPieces){
		if (getPiece(square.x, square.y, allPieces) == null){
			Piece piece = null;
			if (square.y == 2){
				piece = getPiece(square.x, 3, allPieces);
			}
			if (square.y == 5){
				piece = getPiece(square.x, 4, allPieces);
			}
			if (piece != null){
				allPieces.remove(piece);
				halfmoveClock = 0;
			}
		}
	}
	
	
	
	public double findAngle(CoordinateHolder p1, CoordinateHolder p2) {
		double a = 0;
		if (p1.y - p2.y != 0){
			a = Math.atan((p2.x - p1.x) / (p1.y - p2.y));
		}else{
			a = Math.PI / (2 * Math.signum(p2.x - p1.x));
		}
		if (p2.x - p1.x >= 0 && p2.y - p1.y > 0) {
			a += Math.PI;
		}
		if (p2.x - p1.x < 0 && p2.y - p1.y > 0) {
	        a += Math.PI;
	    }
		if (p2.x - p1.x < 0 && p2.y - p1.y <= 0) {
	        a += Math.PI * 2;
	    }
		return a;
	}

	public double findDistance(CoordinateHolder p0, CoordinateHolder p1){
		return Math.sqrt(Math.pow(p1.x - p0.x, 2) + Math.pow(p1.y - p0.y, 2));
	}
}
