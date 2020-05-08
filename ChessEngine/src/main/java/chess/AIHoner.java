package chess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chess.Game.GameState;

public class AIHoner {
	private static final Logger LOGGER = LoggerFactory.getLogger(AIHoner.class);
	public void hone() {
		int iterations = 10;
		while(iterations > 0) {
			List<Game.GameState> gameStates = new ArrayList<>();
			int count = 100;
			double[] doubles0 = getRandomDoubles(6, 2);
			MoveEvaluator whiteEvaluator = new MoveEvaluator(doubles0[0], doubles0[1], doubles0[2], doubles0[3], doubles0[4], doubles0[5]);
			double[] doubles = getRandomDoubles(6, 2);
			MoveEvaluator blackEvaluator = new MoveEvaluator(doubles[0], doubles[1], doubles[2], doubles[3], doubles[4], doubles[5]);
			List<MoveEvaluator> moveEvaluators = new ArrayList<>();
			moveEvaluators.add(whiteEvaluator);
			moveEvaluators.add(blackEvaluator);
			while(count > 0) {
				Game game = new Game();
				game.setMoveEvaluators(moveEvaluators);
				game.playAIGame();
				gameStates.add(game.getGameState());
				count --;
			}
			int whiteWins = 0;
			int blackWins = 0;
			int draws = 0;
			for(Game.GameState gameState : gameStates) {
				if (gameState == GameState.WON_BY_WHITE) {
					whiteWins ++;
				}
				if (gameState == GameState.WON_BY_BLACK) {
					blackWins ++;
				}
				if (gameState == GameState.DRAWN) {
					draws ++;
				}
			}
			LOGGER.info("Black evaluation constants :");
			for(double d : doubles) {
				LOGGER.info(String.valueOf(d));
			}
			LOGGER.info("White : " + whiteWins);
			LOGGER.info("Black : " + blackWins);
			LOGGER.info("Draw : " + draws);
			iterations --;
		}
	}
	
	private double[] getRandomDoubles(int number, double max) {
		double[] output = new double[number];
		Random rnd = new Random();
		int count = 0;
		while(count < number) {
			output[count] = rnd.nextDouble() * max;
			count ++;
		}
		return output;
	}
	
	public static void main(String[] args) {
		new AIHoner().hone();
	}
}
