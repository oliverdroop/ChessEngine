package chess.api.ai;

import chess.api.FENReader;
import chess.api.FENWriter;
import chess.api.PieceConfiguration;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore
public class DepthFirstPositionEvaluatorTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DepthFirstPositionEvaluatorTest.class);

    @Test
    void testGetBestMoveRecursively() {
        PieceConfiguration pieceConfiguration = FENReader.read(FENWriter.STARTING_POSITION);

        LOGGER.info(DepthFirstPositionEvaluator.getBestMoveRecursively(pieceConfiguration, 2).toString());
    }

    @Test
    void testGetBestMoveRecursively_choosesCentre() {
        PieceConfiguration pieceConfiguration = FENReader.read("rnbqkbnr/pppppppp/8/8/P7/8/1PPPPPPP/RNBQKBNR b KQkq - 0 1");

        LOGGER.info(DepthFirstPositionEvaluator.getBestMoveRecursively(pieceConfiguration, 4).toString());
    }

    @Test
    void testPlayAIGame() {
        PieceConfiguration pieceConfiguration = FENReader.read(FENWriter.STARTING_POSITION);
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
