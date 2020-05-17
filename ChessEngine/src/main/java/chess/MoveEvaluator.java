package chess;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoveEvaluator {
	private static final Logger LOGGER = LoggerFactory.getLogger(MoveEvaluator.class);
	private Move move;
	private BoardEvaluator boardEvaluator = new BoardEvaluator();
	
	public MoveEvaluator() {
		// TODO Auto-generated constructor stub
	}

	public void evaluate(int halfmovesAhead) {
		double result = 0;
		Board resultantBoard = move.getResultantBoard();
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
		move.getResultantBoard().getAvailableMoves().forEach(m -> futureMoves.add(m));
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
			return 0;
		}
	}
	
	public Move getMove() {
		return move;
	}

	public void setMove(Move move) {
		this.move = move;
	}
}
