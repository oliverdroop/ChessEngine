package chessTests.api;

import chess.api.FENReader;
import chess.api.PieceConfiguration;
import chess.api.PositionEvaluator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(BlockJUnit4ClassRunner.class)
public class PositionEvaluatorTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PositionEvaluatorTest.class);

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
        PieceConfiguration pieceConfiguration = FENReader.read("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");

        LOGGER.info(PositionEvaluator.getBestMoveRecursively(pieceConfiguration, 1).toString());
    }


}
