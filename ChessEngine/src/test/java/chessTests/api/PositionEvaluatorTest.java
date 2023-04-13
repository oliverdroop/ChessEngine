package chessTests.api;

import chess.api.FENReader;
import chess.api.FENWriter;
import chess.api.PieceConfiguration;
import chess.api.PositionEvaluator;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(BlockJUnit4ClassRunner.class)
public class PositionEvaluatorTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PositionEvaluatorTest.class);

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Test
    public void testStartingDifferential() {
        PieceConfiguration pieceConfiguration = FENReader.read("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");

        assertThat(PositionEvaluator.getValueDifferential(pieceConfiguration))
                .as("The starting position piece values should be equal")
                .isEqualTo(0);
    }

    @Test
    public void testPlayerTeamTotalValue() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/8/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");

        assertThat(PositionEvaluator.getValueDifferential(pieceConfiguration))
                .as("The player side values should be totalled correctly")
                .isEqualTo(39);
    }

    @Test
    public void testOpponentTotalValue() {
        PieceConfiguration pieceConfiguration = FENReader.read("rnbqkbnr/pppppppp/8/8/8/8/8/8 w KQkq - 0 1");

        assertThat(PositionEvaluator.getValueDifferential(pieceConfiguration))
                .as("The opposing side values should be totalled correctly")
                .isEqualTo(-39);
    }

    @Test
    public void testGetBestMoveRecursively() {
        PieceConfiguration pieceConfiguration = FENReader.read(FENWriter.STARTING_POSITION);

        LOGGER.info(PositionEvaluator.getBestMoveRecursively(pieceConfiguration, 1).toString());
    }

    @Test
    public void testGetBestValueDifferentialRecursively() {
        PieceConfiguration pieceConfiguration = FENReader.read("7k/5P2/8/8/8/8/8/6R1 w - - 0 1");

        int bestValueDifferential = PositionEvaluator.getBestValueDifferentialRecursively(pieceConfiguration, 2);

        assertThat(bestValueDifferential)
                .as("Unexpected value differential for best move assessed to a depth of 2 moves")
                .isEqualTo(14);
    }

    @Test
    public void testGetBestValueDifferentialRecursively_fromStartingPosition() {
        PieceConfiguration pieceConfiguration = FENReader.read(FENWriter.STARTING_POSITION);

        int bestValueDifferential = PositionEvaluator.getBestValueDifferentialRecursively(pieceConfiguration, 4);

        assertThat(bestValueDifferential)
                .as("Unexpected value differential for best move assessed to a depth of 4 moves")
                .isEqualTo(3);
    }

    @Test
    public void testGetBestValueDifferentialRecursively_fromSecondMove() {
        PieceConfiguration pieceConfiguration = FENReader.read("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");

        int bestValueDifferential = PositionEvaluator.getBestValueDifferentialRecursively(pieceConfiguration, 4);

        assertThat(bestValueDifferential)
                .as("Unexpected value differential for best move assessed to a depth of 4 moves")
                .isEqualTo(3);
    }

    @Test
    public void testGetBestPieceConfigurationToValueEntryRecursively_forSecondMove() {
        PieceConfiguration pieceConfiguration = FENReader.read("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");

        Optional<Map.Entry<PieceConfiguration, Integer>> bestEntry = PositionEvaluator
                .getBestPieceConfigurationToValueEntryRecursively(pieceConfiguration, 4);

        assertThat(bestEntry).as("There should be a best entry present").isPresent();
        softly.assertThat(bestEntry.get().getValue())
                .as("Unexpected value differential for best second move assessed to a depth of 4 moves")
                .isEqualTo(3);
        softly.assertThat(bestEntry.get().getKey())
                .as("Unexpected FEN for best second move assessed to a depth of 4 moves")
                .isEqualTo(FENWriter.STARTING_POSITION);
    }

    @Test
    public void testGetBestPieceConfigurationToValueEntryRecursively_forThirdMove() {
        PieceConfiguration pieceConfiguration = FENReader.read("rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2");

        Optional<Map.Entry<PieceConfiguration, Integer>> bestEntry = PositionEvaluator
                .getBestPieceConfigurationToValueEntryRecursively(pieceConfiguration, 4);

        assertThat(bestEntry).as("There should be a best entry present").isPresent();
        softly.assertThat(bestEntry.get().getValue())
                .as("Unexpected value differential for best third move assessed to a depth of 4 moves")
                .isEqualTo(3);
        softly.assertThat(bestEntry.get().getKey())
                .as("Unexpected FEN for best third move assessed to a depth of 4 moves")
                .isEqualTo(FENWriter.STARTING_POSITION);
    }

    @Test
    public void testPlayAIGame() {
        PieceConfiguration pieceConfiguration = FENReader.read(FENWriter.STARTING_POSITION);

        while(pieceConfiguration != null) {
            LOGGER.info(pieceConfiguration.toString());
            pieceConfiguration = PositionEvaluator.getBestMoveRecursively(pieceConfiguration, 4);
        }
    }

    @Test
    public void testFENMap() {
        Map<String, Collection<String>> fenMap = PositionEvaluator.buildFENMap(FENWriter.STARTING_POSITION);

        int depth = 4;
        while (depth > 0) {
            for (Map.Entry<String, Collection<String>> entry : fenMap.entrySet()) {
                Collection<String> value = entry.getValue();

                for (String newKey : value) {
                    if (fenMap.containsKey(newKey)) {
                        continue;
                    }
                    Map<String, Collection<String>> onwardFENMap = PositionEvaluator.buildFENMap(newKey);
                    fenMap = Stream.concat(fenMap.entrySet().stream(), onwardFENMap.entrySet().stream())
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1));
                }
            }
            depth--;
        }
        assertThat(fenMap).hasSize(1);
    }

    @Test
    public void testAddToFENMap() {
        Map<String, Collection<String>> fenMap = PositionEvaluator.buildFENMap(FENWriter.STARTING_POSITION);

        int depth = 4;
        while (depth > 0) {
            Set<String> keys = new HashSet<>(fenMap.keySet());
            for(String key : keys) {
                Collection<String> value = fenMap.get(key);

                for (String newKey : value) {
                    if (fenMap.containsKey(newKey)) {
                        continue;
                    }

                    PositionEvaluator.addToFENMap(fenMap, newKey);
                }
            }
            depth--;
        }
        assertThat(fenMap).hasSize(1);
    }

    @Test
    public void testFENMapAsync() throws Exception {
        Map<String, Collection<String>> fenMap = PositionEvaluator.buildFENMap(FENWriter.STARTING_POSITION);

        ExecutorService executor = Executors.newFixedThreadPool(1);

        int depth = 4;
        while (depth > 0) {
            Set<String> keys = new HashSet<>(fenMap.keySet());
            for(String key : keys) {
                Collection<String> value = fenMap.get(key);

                for (String newKey : value) {
                    if (fenMap.containsKey(newKey)) {
                        continue;
                    }

                    PositionEvaluator.addToFENMapAsync(fenMap, newKey, executor);
                }
            }
            depth--;
        }
        assertThat(fenMap).hasSize(1);
    }

    @Test
    public void testRandomGame() {
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
        mateConfiguration.logGameHistory();
    }
}
