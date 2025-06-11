package chess.api.ai;

import chess.api.FENReader;
import chess.api.FENWriter;
import chess.api.PieceConfiguration;
import chess.api.Side;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static chess.api.PieceConfiguration.isFiftyMoveRuleFailure;
import static org.assertj.core.api.Assertions.assertThat;

@Ignore
public class PositionEvaluatorTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PositionEvaluatorTest.class);

    @Test
    void testGetBestMoveRecursively() {
        PieceConfiguration pieceConfiguration = FENReader.read(FENWriter.STARTING_POSITION);

        LOGGER.info(PositionEvaluator.getBestMoveRecursively(pieceConfiguration, 2).toString());
    }

    @Test
    void testGetBestMoveRecursively_choosesCentre() {
        PieceConfiguration pieceConfiguration = FENReader.read("rnbqkbnr/pppppppp/8/8/P7/8/1PPPPPPP/RNBQKBNR b KQkq - 0 1");

        LOGGER.info(PositionEvaluator.getBestMoveRecursively(pieceConfiguration, 4).toString());
    }

    @Test
    void testPlayAIGame() {
        PieceConfiguration pieceConfiguration = FENReader.read(FENWriter.STARTING_POSITION);

        while(pieceConfiguration != null) {
            LOGGER.info(pieceConfiguration.toString());
            pieceConfiguration = PositionEvaluator.getBestMoveRecursively(pieceConfiguration, 4);
        }
    }

    @Test
    void testRandomGame() {
        Random rnd = new Random();
        PieceConfiguration mateConfiguration = null;
        while (mateConfiguration == null) {
            PieceConfiguration pc = FENReader.read(FENWriter.STARTING_POSITION);
            List<PieceConfiguration> ccs = pc.getPossiblePieceConfigurations();
            while (!ccs.isEmpty() && pc.getHalfMoveClock() < 50) {
//            LOGGER.info(FENWriter.write(pc));
                int i = rnd.nextInt(ccs.size());
                pc = ccs.get(i);
                ccs = pc.getPossiblePieceConfigurations();
            }
            LOGGER.info(FENWriter.write(pc));
            if (ccs.isEmpty()) {
                mateConfiguration = pc;
            }
        }
//        mateConfiguration.logGameHistory();
    }

    @Test
    void testHeuristicEvaluation() {
        final int generationSize = 10;
        HeuristicPositionEvaluator bestEvaluator = new HeuristicPositionEvaluator();
        boolean inferior = true;
        int generationNumber = 0;
        while (inferior) {
            int bestGameEndScore = Integer.MIN_VALUE;
            for (int siblingIndex = 0; siblingIndex < generationSize; siblingIndex++) {
                HeuristicPositionEvaluator evaluator = new HeuristicPositionEvaluator(bestEvaluator, new HeuristicPositionEvaluator());
                PieceConfiguration pc = FENReader.read(FENWriter.STARTING_POSITION);
                PieceConfiguration previousConfiguration = null;
                // Play a game against the concurrent position evaluator
                while (pc != null && !isFiftyMoveRuleFailure(pc)) {
                    LOGGER.debug(pc.toString());
                    previousConfiguration = pc;
                    if (pc.getTurnSide() == 0) {
                        pc = evaluator.getBestMove(pc);
                    } else {
                        pc = ConcurrentPositionEvaluator.getBestMoveRecursively(pc, 4);
                    }
                }
                if (previousConfiguration == null) {
                    throw new RuntimeException("Somehow the previous configuration was null");
                }
                // Output game end type
                Integer losingSide = null;
                if (pc != null && isFiftyMoveRuleFailure(pc)) {
                    LOGGER.info("Generation {} game {} : Fifty Move Win for {}",
                        generationNumber, siblingIndex, Side.values()[pc.getOpposingSide()]);
                    LOGGER.info(pc.toString());
                    losingSide = pc.getTurnSide();
                } else if (previousConfiguration.isCheck()) {
                    LOGGER.info("Generation {} game {} : Win for {}",
                        generationNumber, siblingIndex, Side.values()[previousConfiguration.getOpposingSide()]);
                    losingSide = previousConfiguration.getTurnSide();
                } else {
                    LOGGER.info("Generation {} game {} : Stalemate", generationNumber, siblingIndex);
                }
                LOGGER.info(previousConfiguration.toString());
                int gameEndScore = 0;
                if (losingSide != null) {
                    if (losingSide == 1) {
                        gameEndScore += Short.MAX_VALUE;
                        inferior = false;
                    } else {
                        gameEndScore += Short.MIN_VALUE;
                    }
                }
                gameEndScore += previousConfiguration.getFullMoveNumber() + previousConfiguration.getValueDifferential();
                if (gameEndScore > bestGameEndScore) {
                    bestGameEndScore = gameEndScore;
                    bestEvaluator = evaluator;
                }
            }
            generationNumber++;
        }
    }
}
