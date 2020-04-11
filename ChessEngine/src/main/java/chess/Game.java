package chess;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Game {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Game.class);
	private Board board;
	
	public Game() {
		board = new Board(this);
		board.turnTeam = Team.WHITE;
		createPieces();
	}
	
	public boolean wins(Team team){
		int availableMoves = 0;
		for(int i = 0; i < board.pieces.size(); i++){
			Piece piece = board.pieces.get(i);
			if (piece.team != team){
				availableMoves += piece.getAvailableMoves(board.pieces).size();
			}
		}
		if (availableMoves == 0){
			if (board.check(Team.values()[1 - team.ordinal()], board.pieces)){
				return true;
			}
			else{
				//stalemate
				System.out.println("Stalemate");
			}
		}
		return false;
	}
	
	public void createPieces(){
		for(int c = 0; c < 2; c++){
			//pawns
			for(int i = 0; i < 8; i++){
				board.pieces.add(new PieceBuilder()
						.withBoard(board)
						.withTeam(Team.values()[c])
						.withType(PieceType.PAWN)
						.withX(i)
						.withY(1 + (c * 5))
						.withHasMoved(false)
						.build());
			}
			//kings
			board.pieces.add(new PieceBuilder()
					.withBoard(board)
					.withTeam(Team.values()[c])
					.withType(PieceType.KING)
					.withX(3)
					.withY(c * 7)
					.withHasMoved(false)
					.build());
			//queens
			board.pieces.add(new PieceBuilder()
					.withBoard(board)
					.withTeam(Team.values()[c])
					.withType(PieceType.QUEEN)
					.withX(4)
					.withY(c * 7)
					.withHasMoved(false)
					.build());
			//rooks
			board.pieces.add(new PieceBuilder()
					.withBoard(board)
					.withTeam(Team.values()[c])
					.withType(PieceType.ROOK)
					.withX(0)
					.withY(c * 7)
					.withHasMoved(false)
					.build());
			board.pieces.add(new PieceBuilder()
					.withBoard(board)
					.withTeam(Team.values()[c])
					.withType(PieceType.ROOK)
					.withX(7)
					.withY(c * 7)
					.withHasMoved(false)
					.build());
			//bishops
			board.pieces.add(new PieceBuilder()
					.withBoard(board)
					.withTeam(Team.values()[c])
					.withType(PieceType.BISHOP)
					.withX(2)
					.withY(c * 7)
					.withHasMoved(false)
					.build());
			board.pieces.add(new PieceBuilder()
					.withBoard(board)
					.withTeam(Team.values()[c])
					.withType(PieceType.BISHOP)
					.withX(5)
					.withY(c * 7)
					.withHasMoved(false)
					.build());
			//knights
			board.pieces.add(new PieceBuilder()
					.withBoard(board)
					.withTeam(Team.values()[c])
					.withType(PieceType.KNIGHT)
					.withX(1)
					.withY(c * 7)
					.withHasMoved(false)
					.build());
			board.pieces.add(new PieceBuilder()
					.withBoard(board)
					.withTeam(Team.values()[c])
					.withType(PieceType.KNIGHT)
					.withX(6)
					.withY(c * 7)
					.withHasMoved(false)
					.build());
		}
	}
	
	public void playAIMove(){
		Random rnd = new Random();
		List<Move> availableMoves = board.getAvailableMoves();
		availableMoves.forEach(m -> LOGGER.info(m.toString()));
		Move chosenMove = availableMoves.get(rnd.nextInt(availableMoves.size()));
		chosenMove.getPiece().move(chosenMove.getEndSquare());
	}
	
	public Board getBoard() {
		return board;
	}

	public void setBoard(Board board) {
		this.board = board;
	}

	public static void main(String[] args) {
		new Game();		
	}
	
	public String getBoardState() {
		return new FENWriter().write(board);
	}
	
	public void setBoardState(String fen) {
		board = new FENReader().read(fen, this);
	}
}
