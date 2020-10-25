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
	private GameState gameState = GameState.IN_PROGRESS;
	private int turnsAheadToLook = 3;
	public static enum GameState {
		WON_BY_WHITE, WON_BY_BLACK, DRAWN, IN_PROGRESS;
	}
	private List<MoveEvaluator> moveEvaluators = new ArrayList<>();
	
	public Game() {
		board = new Board(this);
		board.turnTeam = Team.WHITE;
		createPieces();
		MoveEvaluator evaluator = new MoveEvaluator();
		moveEvaluators.add(evaluator);
		moveEvaluators.add(evaluator);
	}
	
	public boolean wins(Team team){
		int availableMoves = 0;
		availableMoves = board.getAvailableMoves().size();
		if (availableMoves == 0){
			if (board.check(Team.values()[1 - team.ordinal()], board.pieces)){
				gameState = GameState.values()[team.ordinal()];
				return true;
			}
			else{
				LOGGER.info("Stalemate");
				gameState = GameState.DRAWN;
			}
		}
		if (board.getTeamPieces(team, board.pieces).size() == 1 && board.getTeamPieces(Team.values()[1 - team.ordinal()], board.pieces).size() == 1) {
			LOGGER.info("Draw due to neither team having enough pieces to win");
			gameState = GameState.DRAWN;
		}
		if (board.getHalfmoveClock() >= 50) {
			LOGGER.info("Draw due to halfmove clock reaching 50");
			gameState = GameState.DRAWN;
		}
		return false;
	}
	
	public void playAIMove(){
		Move chosenMove = getAIMove();
		chosenMove.getPiece().move(chosenMove.getEndSquare());
	}
	
	public void playAIGame() {
		Move currentMove = getAIMove();
		while(currentMove != null && gameState == GameState.IN_PROGRESS) {
			currentMove.getPiece().move(currentMove.getEndSquare());
			LOGGER.info(currentMove.toString());
			LOGGER.info(getBoardState());
			currentMove = getAIMove();
		}
	}
	
	public Move getAIMove() {
		List<Move> availableMoves = board.getAvailableMoves();
		MoveEvaluator moveEvaluator = null;
		if (moveEvaluators.size() == 2) {
			moveEvaluator = moveEvaluators.get(board.getTurnTeam().ordinal());
		}
		if (moveEvaluator != null) {
			if (availableMoves.size() > 0) {
				Collections.shuffle(availableMoves);
				for(Move move : availableMoves) {
					moveEvaluator.setMove(move);
					moveEvaluator.evaluate(turnsAheadToLook);
				}
				availableMoves.sort(null);
				return availableMoves.get(availableMoves.size() - 1);
			}
		}
		else {
			LOGGER.info("No move evaluator present to select move");
		}
		return null;
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
	
	public Board getBoard() {
		return board;
	}

	public void setBoard(Board board) {
		this.board = board;
	}

	public static void main(String[] args) {
		Game game = new Game();
		game.playAIGame();
	}
	
	public String getBoardState() {
		return new FENWriter().write(board);
	}
	
	public void setBoardState(String fen) {
		board = new FENReader().read(fen, this);
	}

	public GameState getGameState() {
		return gameState;
	}

	public void setGameState(GameState gameState) {
		this.gameState = gameState;
	}

	public List<MoveEvaluator> getMoveEvaluators() {
		return moveEvaluators;
	}

	public void setMoveEvaluators(List<MoveEvaluator> moveEvaluators) {
		this.moveEvaluators = moveEvaluators;
	}
	
	
	
}
