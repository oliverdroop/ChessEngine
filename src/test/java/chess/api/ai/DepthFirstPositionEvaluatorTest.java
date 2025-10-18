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

@Disabled
public class DepthFirstPositionEvaluatorTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DepthFirstPositionEvaluatorTest.class);

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testGetBestMoveRecursively(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = FENReader.read(FENWriter.STARTING_POSITION, configurationClass);

        LOGGER.info(DepthFirstPositionEvaluator.getBestMoveRecursively(pieceConfiguration, 2).toString());
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testGetBestMoveRecursively_choosesCentre(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = FENReader.read("rnbqkbnr/pppppppp/8/8/P7/8/1PPPPPPP/RNBQKBNR b KQkq - 0 1", configurationClass);

        LOGGER.info(DepthFirstPositionEvaluator.getBestMoveRecursively(pieceConfiguration, 4).toString());
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testPlayAIGame(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = FENReader.read(FENWriter.STARTING_POSITION, configurationClass);
        PieceConfiguration previousConfiguration = null;

        while(pieceConfiguration != null) {
            LOGGER.info(pieceConfiguration.toString());
            previousConfiguration = pieceConfiguration;
            if (pieceConfiguration.getTurnSide() == 0) {
                pieceConfiguration = DepthFirstPositionEvaluator.getBestMoveRecursively(pieceConfiguration, 4);
            } else {
                pieceConfiguration = BreadthFirstPositionEvaluator.getBestMoveRecursively(pieceConfiguration, 4);
            }
        }
        LOGGER.info(DepthFirstPositionEvaluator.deriveGameEndType(previousConfiguration).toString());
    }

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
