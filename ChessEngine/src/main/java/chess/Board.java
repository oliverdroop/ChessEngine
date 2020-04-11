package chess;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Board {
	private static final Logger LOGGER = LoggerFactory.getLogger(Board.class);
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
		for(Square square : squares){
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
				Square square = new Square(this);
					square.x = x;
					square.y = y;
				squares.add(square);
			}
		}
	}
	
	public boolean check2(Team team, List<Piece> allPieces){
		Piece king = getKing(team, allPieces);
		Team otherTeam = Team.values()[1 - team.ordinal()];
		for(Piece piece : getTeamPieces(otherTeam, allPieces)){
			Square square1 = getSquare(piece.x, piece.y);
			Square square2 = getSquare(king.x, king.y);
			Move move = new Move(piece, square1, square2);
			if (!move.blocked(otherTeam, allPieces)){
				if ((piece.type == PieceType.QUEEN || piece.type == PieceType.ROOK) && move.orthogonal()){
					return true;
				}
				if ((piece.type == PieceType.QUEEN || piece.type == PieceType.BISHOP) && move.diagonal()){
					return true;
				}
				if (piece.type == PieceType.KNIGHT && move.jump()){
					return true;
				}
				if (piece.type == PieceType.KING && move.adjacent()){
					return true;
				}
				if (piece.type == PieceType.PAWN && move.adjacent() && move.forward(otherTeam) && move.diagonal()){
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean check(Team team, List<Piece> allPieces) {
		Piece king = getKing(team, allPieces);
		Team otherTeam = Team.values()[1 - team.ordinal()];
		for(Piece piece : getTeamPieces(otherTeam, allPieces)) {
			if (piece.threatens(king.getSquare(), allPieces)) {
				return true;
			}
		}
		return false;
	}
	
	public Piece getKing(Team team, List<Piece> allPieces){
		for(Piece piece : getTeamPieces(team, allPieces)){
			if (piece.type == PieceType.KING){
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
			if (p2.x == p1.x && p2.y > p1.y) {
				return Math.PI;
			}
			if (p2.x == p1.x && p2.y < p1.y) {
				return -Math.PI;
			}
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
	
	public List<Move> getAvailableMoves(){
		List<Move> moves = new ArrayList<>();
		for(Piece piece : getTeamPieces(turnTeam, pieces)) {
			for(Square square : piece.getAvailableMoves(pieces)) {
				moves.add(new Move(piece, piece.getSquare(), square));
			}
		}
		return moves;
	}
	
	public List<Piece> getTeamPieces(Team team, List<Piece> allPieces){
		return allPieces.stream().filter(p -> p.team == team).collect(Collectors.toList());
	}

	public double findDistance(CoordinateHolder p0, CoordinateHolder p1){
		return Math.sqrt(Math.pow(p1.x - p0.x, 2) + Math.pow(p1.y - p0.y, 2));
	}

	public Game getGame() {
		return game;
	}

	public void setGame(Game game) {
		this.game = game;
	}

	public List<Square> getSquares() {
		return squares;
	}

	public void setSquares(List<Square> squares) {
		this.squares = squares;
	}

	public List<Piece> getPieces() {
		return pieces;
	}

	public void setPieces(List<Piece> pieces) {
		this.pieces = pieces;
	}

	public Piece getEnPassantable() {
		return enPassantable;
	}

	public void setEnPassantable(Piece enPassantable) {
		this.enPassantable = enPassantable;
	}

	public int getHalfmoveClock() {
		return halfmoveClock;
	}

	public void setHalfmoveClock(int halfmoveClock) {
		this.halfmoveClock = halfmoveClock;
	}

	public int getFullmoveNumber() {
		return fullmoveNumber;
	}

	public void setFullmoveNumber(int fullmoveNumber) {
		this.fullmoveNumber = fullmoveNumber;
	}

	public Team getTurnTeam() {
		return turnTeam;
	}

	public void setTurnTeam(Team turnTeam) {
		this.turnTeam = turnTeam;
	}
	
}
