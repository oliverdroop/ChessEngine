package chess.api.configuration;

import chess.api.FENReader;
import chess.api.FENWriter;
import org.junit.jupiter.api.Test;

import static chess.api.BitUtil.hasBitFlag;
import static chess.api.configuration.PieceConfiguration.*;
import static org.assertj.core.api.Assertions.assertThat;

public class LongsPieceConfigurationTest {

    private static final String BLACK_TURN_FEN = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1";

    @Test
    void getPieceAtPosition_startingPosition_playerKing() {
        PieceConfiguration pieceConfiguration = FENReader.read(FENWriter.STARTING_POSITION, LongsPieceConfiguration.class);
        pieceConfiguration.setHigherBitFlags();

        final int playerKingPieceData = pieceConfiguration.getPieceAtPosition(4);
        assertThat(pieceConfiguration).isInstanceOf(LongsPieceConfiguration.class);
        assertThat(hasBitFlag(playerKingPieceData, KING_OCCUPIED)).as("Expected king occupation flag").isTrue();
        assertThat(hasBitFlag(playerKingPieceData, WHITE_OCCUPIED)).as("Expected white occupation flag").isTrue();
        assertThat(hasBitFlag(playerKingPieceData, PLAYER_OCCUPIED)).as("Expected player occupation flag").isTrue();
    }

    @Test
    void getPieceAtPosition_startingPosition_opposingKing() {
        PieceConfiguration pieceConfiguration = FENReader.read(FENWriter.STARTING_POSITION, LongsPieceConfiguration.class);
        pieceConfiguration.setHigherBitFlags();

        final int opposingKingPieceData = pieceConfiguration.getPieceAtPosition(60);
        assertThat(pieceConfiguration).isInstanceOf(LongsPieceConfiguration.class);
        assertThat(hasBitFlag(opposingKingPieceData, KING_OCCUPIED)).as("Expected king occupation flag").isTrue();
        assertThat(hasBitFlag(opposingKingPieceData, BLACK_OCCUPIED)).as("Expected black occupation flag").isTrue();
        assertThat(hasBitFlag(opposingKingPieceData, OPPONENT_OCCUPIED)).as("Expected opponent occupation flag").isTrue();
    }

    @Test
    void getPieceAtPosition_blackTurn_opposingKing() {
        PieceConfiguration pieceConfiguration = FENReader.read(BLACK_TURN_FEN, LongsPieceConfiguration.class);
        pieceConfiguration.setHigherBitFlags();

        final int opposingKingPieceData = pieceConfiguration.getPieceAtPosition(4);
        assertThat(pieceConfiguration).isInstanceOf(LongsPieceConfiguration.class);
        assertThat(hasBitFlag(opposingKingPieceData, KING_OCCUPIED)).as("Expected king occupation flag").isTrue();
        assertThat(hasBitFlag(opposingKingPieceData, WHITE_OCCUPIED)).as("Expected white occupation flag").isTrue();
        assertThat(hasBitFlag(opposingKingPieceData, OPPONENT_OCCUPIED)).as("Expected opponent occupation flag").isTrue();
    }

    @Test
    void getPieceAtPosition_blackTurn_playerKing() {
        PieceConfiguration pieceConfiguration = FENReader.read(BLACK_TURN_FEN, LongsPieceConfiguration.class);
        pieceConfiguration.setHigherBitFlags();

        final int playerKingPieceData = pieceConfiguration.getPieceAtPosition(60);
        assertThat(pieceConfiguration).isInstanceOf(LongsPieceConfiguration.class);
        assertThat(hasBitFlag(playerKingPieceData, KING_OCCUPIED)).as("Expected king occupation flag").isTrue();
        assertThat(hasBitFlag(playerKingPieceData, BLACK_OCCUPIED)).as("Expected black occupation flag").isTrue();
        assertThat(hasBitFlag(playerKingPieceData, PLAYER_OCCUPIED)).as("Expected player occupation flag").isTrue();
    }
}
