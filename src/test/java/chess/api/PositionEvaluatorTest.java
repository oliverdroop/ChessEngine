package chess.api;

import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

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
        PieceConfiguration previousConfiguration = null;

        while(pieceConfiguration != null) {
            previousConfiguration = pieceConfiguration;
            LOGGER.debug(pieceConfiguration.toString());
            pieceConfiguration = PositionEvaluator.getBestMoveRecursively(pieceConfiguration, 4);
        }

        if (previousConfiguration.isCheck()) {
            Side winner = Side.values()[previousConfiguration.getOpposingSide()];
            LOGGER.info("Win for {}", winner);
        } else {
            LOGGER.info("Stalemate");
        }
    }

    @Test
    void testPlayAIGames() {
        final int matchesToPlay = 100;
        int matchNumber = 0;
        final int maxDepth = 5;
        while (matchNumber < matchesToPlay) {
            int depth = 1;
            int gameNumber = 0;
            while (depth < maxDepth) {
                final Integer winner0 = playGame(matchNumber, gameNumber, depth);
                WeightingConfig.swapWeightings();
                final Integer winner1 = playGame(matchNumber, gameNumber + 1, depth);
                if (winner0 != null && winner1 != null) {
                    // Neither game was a stalemate
                    if (winner0.equals(winner1)) {
                        // Neither of the weighting configurations is measurably superior
                        if (depth < maxDepth - 1) {
                            depth++;
                            gameNumber += 2;
                        } else {
                            WeightingConfig.breedWeightings();
                            break;
                        }
                    } else {
                        // One of the weighting configs is superior
                        WeightingConfig.generateRandomWeightings(1 - winner1);
                        break;
                    }
                } else if (winner0 == null && winner1 == null) {
                    // Both games were a stalemate
                    if (depth < maxDepth - 1) {
                        depth++;
                        gameNumber += 2;
                    } else {
                        WeightingConfig.breedWeightings();
                        break;
                    }
                } else if (winner0 == null) {
                    // The first game was a stalemate
                    WeightingConfig.generateRandomWeightings(1 - winner1);
                    break;
                } else {
                    // The second game was a stalemate
                    WeightingConfig.generateRandomWeightings(winner0);
                    break;
                }
            }
            matchNumber++;
        }
        WeightingConfig.logWeightings(0);
        WeightingConfig.logWeightings(1);
    }

    private Integer playGame(int matchNumber, int gameNumber, int depth) {
        PieceConfiguration pieceConfiguration = FENReader.read(FENWriter.STARTING_POSITION);
        PieceConfiguration previousConfiguration = null;

        while (pieceConfiguration != null) {
            previousConfiguration = pieceConfiguration;
            LOGGER.debug(pieceConfiguration.toString());
            pieceConfiguration = PositionEvaluator.getBestMoveRecursively(pieceConfiguration, depth);
        }

        LOGGER.info(previousConfiguration.toString());
        if (previousConfiguration.isCheck()) {
            Side winner = Side.values()[previousConfiguration.getOpposingSide()];
            LOGGER.info("Match {} game {} win for {}", matchNumber, gameNumber, winner);
            return winner.ordinal();
        } else {
            LOGGER.info("Match {} game {} stalemate", matchNumber, gameNumber);
            return null;
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
}
