package chess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Game {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Game.class);
	private Board board;
	
	public Game(boolean aiOnly) {
		board = new Board(this, true);
		board.turnTeam = Team.WHITE;
		createPieces();
		if (aiOnly) {
			playAIGame();
		}
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
		if (board.getTeamPieces(team, board.pieces).size() == 1 && board.getTeamPieces(Team.values()[1 - team.ordinal()], board.pieces).size() == 1) {
			System.out.println("Draw");
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
		Move chosenMove = getAIMove();
		chosenMove.getPiece().move(chosenMove.getEndSquare());
	}
	
	public void playAIGame() {
		Move currentMove = getAIMove();
		while(currentMove != null) {
			currentMove.getPiece().move(currentMove.getEndSquare());
			LOGGER.info(currentMove.toString());
			LOGGER.info(getBoardState());
			currentMove = getAIMove();
		}
	}
	
	public Move getAIMove() {
		Random rnd = new Random();
		Map<Move,Double> evaluationMap = new HashMap<>();
		List<Move> availableMoves = board.getAvailableMoves();
		MoveEvaluator moveEvaluator = new MoveEvaluator();
		if (availableMoves.size() > 0) {
			Collections.shuffle(availableMoves);
			for(Move move : availableMoves) {
				moveEvaluator.setMove(move);
				moveEvaluator.evaluate();
			}
			availableMoves.sort(null);
			//return availableMoves.get(rnd.nextInt(availableMoves.size()));
			return availableMoves.get(availableMoves.size() - 1);
		}
		return null;
	}
	
	public Board getBoard() {
		return board;
	}

	public void setBoard(Board board) {
		this.board = board;
	}

	public static void main(String[] args) {
		new Game(true);
	}
	
	public String getBoardState() {
		return new FENWriter().write(board);
	}
	
	public void setBoardState(String fen) {
		board = new FENReader().read(fen, this);
	}
}
