package chess.old;
import java.util.ArrayList;
import java.util.Arrays;
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
	private double evaluation = 0;
	
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
	
	public boolean check(Team team, List<Piece> allPieces) {
		Piece king = getKing(team, allPieces);
		if (king == null) {
			return false;
		}
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
	
	public List<Move> getAvailableMoves(){
		List<Move> moves = new ArrayList<>();
		for(Piece piece : getTeamPieces(turnTeam, pieces)) {
			for(Square square : piece.getAvailableMoves(pieces)) {
				if (piece.getType() == PieceType.PAWN && square.getY() == 7 * (1 - piece.getTeam().ordinal())) {
					getPawnUpgrades(piece).forEach(p -> moves.add(new Move(p, piece.getSquare(), square)));
				} else {
					moves.add(new Move(piece, piece.getSquare(), square));
				}				
			}
		}
		return moves;
	}
	
	private List<Piece> getPawnUpgrades(Piece piece){
		int x = piece.getX();
		int y = piece.getY();
		Team team = piece.getTeam();
		List<PieceType> pawnUpgradeTypes = Arrays.asList(PieceType.BISHOP, PieceType.ROOK, PieceType.KNIGHT, PieceType.QUEEN);
		List<Piece> upgradePieces = new ArrayList<>();
		for(PieceType type : pawnUpgradeTypes) {
			upgradePieces.add(new PieceBuilder()
					.withX(x)
					.withY(y)
					.withTeam(team)
					.withBoard(this)
					.withHasMoved(true)
					.withType(type)
					.build());
		}
		return upgradePieces;
	}
	
	public Piece copyPiece(Piece piece) {
		return new PieceBuilder()
				.withBoard(this)
				.withTeam(piece.getTeam())
				.withType(piece.getType())
				.withHasMoved(piece.getHasMoved())
				.withX(piece.getX())
				.withY(piece.getY())
				.build();
	}
	
	public int compareTo(Object board) {
		return ((Double)this.getEvaluation()).compareTo(((Board)board).getEvaluation());
	}
	
	public List<Piece> getTeamPieces(Team team, List<Piece> allPieces){
		return allPieces.stream().filter(p -> p.team == team).collect(Collectors.toList());
	}
	
	public Team getOpposingTeam() {
		return Team.values()[1 - turnTeam.ordinal()];
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
	
	public void addPiece(Piece piece) {
		pieces.add(piece);
	}
	
	public void removePiece(Piece piece) {
		pieces.remove(piece);
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

	public double getEvaluation() {
		return evaluation;
	}

	public void setEvaluation(double evaluation) {
		this.evaluation = evaluation;
	}
}
