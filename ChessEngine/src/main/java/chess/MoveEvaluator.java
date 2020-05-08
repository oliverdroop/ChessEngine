package chess;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoveEvaluator {
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
		result = resultantBoard.getEvaluation();
		if (halfmovesAhead > 0) {
			halfmovesAhead --;
			result = considerFuture(result, halfmovesAhead);
		}
		move.setEvaluation(result);
	}

	public double considerFuture(double input, int halfmovesAhead) {
		List<Move> futureMoves = new ArrayList<>();
		move.getResultantBoard().getAvailableMoves().forEach(m -> futureMoves.add(m));
		if (futureMoves.size() > 0) {
			for(Move futureMove : futureMoves) {
				setMove(futureMove);
				evaluate(halfmovesAhead);
			}
			futureMoves.sort(null);
			
			return input - futureMoves.get(futureMoves.size() - 1).getEvaluation();
		}
		else {
			return input;
		}
	}
	
	public Move getMove() {
		return move;
	}

	public void setMove(Move move) {
		this.move = move;
	}
}
