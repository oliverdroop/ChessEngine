package chess.api.ai;

import chess.api.FENReader;
import chess.api.FENWriter;
import chess.api.configuration.IntsPieceConfiguration;
import chess.api.configuration.LongsPieceConfiguration;
import chess.api.configuration.PieceConfiguration;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static chess.api.ai.DepthFirstPositionEvaluator.getBestMoveRecursively;
import static org.assertj.core.api.Assertions.assertThat;


public class DepthFirstPositionEvaluatorTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DepthFirstPositionEvaluatorTest.class);

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testGetBestMoveRecursively(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = FENReader.read(FENWriter.STARTING_POSITION, configurationClass);

        PieceConfiguration newConfiguration = getBestMoveRecursively(pieceConfiguration, 2);

        assertThat(newConfiguration).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testGetBestMoveRecursively_returnsNull(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = FENReader.read("k7/7R/8/8/8/8/8/1R5K b - - 0 50", configurationClass);

        PieceConfiguration newConfiguration = getBestMoveRecursively(pieceConfiguration, 2);

        assertThat(newConfiguration).isNull();
    }

    @Disabled
    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testGetBestMoveRecursively_choosesCentre(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = FENReader.read("rnbqkbnr/pppppppp/8/8/P7/8/1PPPPPPP/RNBQKBNR b KQkq - 0 1", configurationClass);

        LOGGER.info(getBestMoveRecursively(pieceConfiguration, 4).toString());
    }

    @Disabled
    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testPlayAIGame_DepthFirstVsBreadthFirst(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = FENReader.read(FENWriter.STARTING_POSITION, configurationClass);
        PieceConfiguration previousConfiguration = null;

        while(pieceConfiguration != null) {
            LOGGER.info(pieceConfiguration.toString());
            previousConfiguration = pieceConfiguration;
            if (pieceConfiguration.getTurnSide() == 0) {
                pieceConfiguration = getBestMoveRecursively(pieceConfiguration, 4);
            } else {
                pieceConfiguration = BreadthFirstPositionEvaluator.getBestMoveRecursively(pieceConfiguration, 4);
            }
        }
        LOGGER.info(previousConfiguration.deriveGameEndType().toString());
    }

    @Disabled
    @Test
    void testPlayAIGame_IntsVsLongs() {
        PieceConfiguration pieceConfiguration = FENReader.read(FENWriter.STARTING_POSITION, IntsPieceConfiguration.class);
        PieceConfiguration previousConfiguration = null;

        while(pieceConfiguration != null) {
            LOGGER.info(pieceConfiguration.toString());
            previousConfiguration = pieceConfiguration;
            if (pieceConfiguration.getTurnSide() == 0) {
                PieceConfiguration input = FENReader.read(FENWriter.write(pieceConfiguration), IntsPieceConfiguration.class);
                pieceConfiguration = getBestMoveRecursively(input, 4);
            } else {
                PieceConfiguration input = FENReader.read(FENWriter.write(pieceConfiguration), LongsPieceConfiguration.class);
                pieceConfiguration = getBestMoveRecursively(input, 4);
            }
        }
        LOGGER.info(previousConfiguration.deriveGameEndType().toString());
    }

    @Disabled
    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testRandomGame(Class<? extends PieceConfiguration> configurationClass) {
        Random rnd = new Random();
        PieceConfiguration mateConfiguration = null;
        while (mateConfiguration == null) {
            PieceConfiguration pc = FENReader.read(FENWriter.STARTING_POSITION, configurationClass);
            List<PieceConfiguration> ccs = pc.getOnwardConfigurations();
            while (!ccs.isEmpty() && pc.getHalfMoveClock() < 50) {
//            LOGGER.info(FENWriter.write(pc));
                int i = rnd.nextInt(ccs.size());
                pc = ccs.get(i);
                ccs = pc.getOnwardConfigurations();
            }
            LOGGER.info(FENWriter.write(pc));
            if (ccs.isEmpty()) {
                mateConfiguration = pc;
            }
        }
//        mateConfiguration.logGameHistory();
    }
}
