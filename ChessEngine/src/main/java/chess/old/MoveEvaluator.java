package chess.old;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoveEvaluator {
	private static final Logger LOGGER = LoggerFactory.getLogger(MoveEvaluator.class);
	private Move move;
	private BoardEvaluator boardEvaluator = new BoardEvaluator();
	private Board resultantBoard;
	
	public MoveEvaluator() {
		// TODO Auto-generated constructor stub
	}

	public void evaluate(int halfmovesAhead) {
		double result = 0;
		resultantBoard = move.getResultantBoard();
		boardEvaluator.setBoard(resultantBoard);
		boardEvaluator.evaluate();
		result = -resultantBoard.getEvaluation();
		if (halfmovesAhead > 0) {
			halfmovesAhead --;
			result += considerFuture(halfmovesAhead);
		}
		move.setEvaluation(result);
		LOGGER.debug(move.toString() + " " + result);
	}

	public double considerFuture(int halfmovesAhead) {
		List<Move> futureMoves = new ArrayList<>();
		resultantBoard.getAvailableMoves().forEach(m -> futureMoves.add(m));
		if (futureMoves.size() > 0) {
			for(Move futureMove : futureMoves) {
				MoveEvaluator secondaryMoveEvaluator = new MoveEvaluator();
				secondaryMoveEvaluator.setMove(futureMove);
				secondaryMoveEvaluator.evaluate(halfmovesAhead);
			}
			futureMoves.sort(null);
			
			return -futureMoves.get(futureMoves.size() - 1).getEvaluation();
		}
		else {
			if (resultantBoard.check(move.getPiece().getOpposingTeam(), resultantBoard.getPieces())) {
				return 1000000 + halfmovesAhead;
			}
			else {
				return -1000000;
			}
		}
	}
	
	public Move getMove() {
		return move;
	}

	public void setMove(Move move) {
		this.move = move;
	}
}
