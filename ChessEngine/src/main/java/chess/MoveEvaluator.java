package chess;

import java.util.HashMap;
import java.util.Map;

public class MoveEvaluator {
	private Move move;
	
	private static Map<String, Double> weightMap = new HashMap<>();
	
	static {
		weightMap.put("considerPieceTaken", 1.0);
	}
	
	public double evaluate() {
		double start = 1;
		double result = considerPieceTaken(start);
		return result;
	}
	
	public double considerPieceTaken(double input) {
		if (move.getPieceTaken() == null) {
			return input;
		}
		int pieceValue = move.getPieceTaken().getValue();
		return input + (pieceValue * weightMap.get("considerPieceTaken"));
	}

	public Move getMove() {
		return move;
	}

	public void setMove(Move move) {
		this.move = move;
	}
}
