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
		Square pieceSquare = new Square();
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
			Move move = new Move(piece, square1, square2);
			if (piece.team == otherTeam && !move.blocked(square1, square2, otherTeam, allPieces)){
				if (piece.type == PieceType.QUEEN || piece.type == PieceType.ROOK){
					if (move.orthogonal()){
						return true;
					}
				}
				if (piece.type == PieceType.QUEEN || piece.type == PieceType.BISHOP){
					if (move.diagonal()){
						return true;
					}
				}
				if (piece.type == PieceType.KNIGHT){
					if (move.jump()){
						return true;
					}
				}
				if (piece.type == PieceType.KING){
					if (move.adjacent()){
						return true;
					}
				}
				if (piece.type == PieceType.PAWN){
					if (move.adjacent() && move.forward(otherTeam) && move.diagonal()){
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
