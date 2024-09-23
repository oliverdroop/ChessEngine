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
    void testStartingDifferential() {
        PieceConfiguration pieceConfiguration = FENReader.read("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");

        assertThat(PositionEvaluator.getValueDifferential(pieceConfiguration))
                .as("The starting position piece values should be equal")
                .isEqualTo(0);
    }

    @Test
    void testPlayerTeamTotalValue() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/8/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");

        assertThat(PositionEvaluator.getValueDifferential(pieceConfiguration))
                .as("The player side values should be totalled correctly")
                .isEqualTo(39);
    }

    @Test
    void testOpponentTotalValue() {
        PieceConfiguration pieceConfiguration = FENReader.read("rnbqkbnr/pppppppp/8/8/8/8/8/8 w KQkq - 0 1");

        assertThat(PositionEvaluator.getValueDifferential(pieceConfiguration))
                .as("The opposing side values should be totalled correctly")
                .isEqualTo(-39);
    }

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
    void testPCTreeCallable() throws ExecutionException, InterruptedException {
        PieceConfiguration pc = FENReader.read(FENWriter.STARTING_POSITION);
        ExecutorService executor = Executors.newFixedThreadPool(4);

        List<String> onwardFENs = pc.getPossiblePieceConfigurations().stream()
                .map(oc -> FENWriter.write(oc))
                .collect(Collectors.toList());
        Map<String, Collection<String>> fenMap = new HashMap<>();
        fenMap.put(FENWriter.STARTING_POSITION, onwardFENs);

        int rounds = 2;
        while (rounds > 0) {
            Set<String> fenMapKeys = fenMap.keySet().stream().collect(Collectors.toSet());
            for (String fen : fenMapKeys) {
                List<Future<Map<String, Collection<String>>>> onwardFENMaps = new ArrayList<>();
                for (String onwardFEN : fenMap.get(fen)) {
                    PieceConfiguration onwardConfiguration = FENReader.read(onwardFEN);
                    PCTreeCallable pcTreeCallable = new PCTreeCallable(onwardConfiguration, 2);
                    onwardFENMaps.add(executor.submit(pcTreeCallable));
                }
                for (Future<Map<String, Collection<String>>> futureOnwardFENMap : onwardFENMaps) {
                    Map<String, Collection<String>> onwardFENMap = futureOnwardFENMap.get();
                    fenMap.putAll(onwardFENMap);
                }
            }
            rounds --;
        }
        assertThat(fenMap).hasSize(124991);
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
        mateConfiguration.logGameHistory();
    }
}
