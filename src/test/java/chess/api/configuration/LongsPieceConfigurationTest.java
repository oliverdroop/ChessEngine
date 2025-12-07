package chess.api.configuration;

import chess.api.FENReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static chess.api.BitUtil.hasBitFlag;
import static chess.api.FENWriter.STARTING_POSITION;
import static chess.api.configuration.PieceConfiguration.*;
import static org.assertj.core.api.Assertions.assertThat;

public class LongsPieceConfigurationTest {

    private static final String BLACK_TURN_FEN = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1";

    @Test
    void getPieceAtPosition_startingPosition_playerKing() {
        PieceConfiguration pieceConfiguration = FENReader.read(STARTING_POSITION, LongsPieceConfiguration.class);
        pieceConfiguration.setHigherBitFlags();

        final int playerKingPieceData = pieceConfiguration.getPieceAtPosition(4);
        assertThat(pieceConfiguration).isInstanceOf(LongsPieceConfiguration.class);
        assertThat(hasBitFlag(playerKingPieceData, KING_OCCUPIED)).as("Expected king occupation flag").isTrue();
        assertThat(hasBitFlag(playerKingPieceData, WHITE_OCCUPIED)).as("Expected white occupation flag").isTrue();
        assertThat(hasBitFlag(playerKingPieceData, PLAYER_OCCUPIED)).as("Expected player occupation flag").isTrue();
    }

    @Test
    void getPieceAtPosition_startingPosition_opposingKing() {
        PieceConfiguration pieceConfiguration = FENReader.read(STARTING_POSITION, LongsPieceConfiguration.class);
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

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void countUndevelopedPiecesBySide_startingPosition(int side) {
        LongsPieceConfiguration pieceConfiguration = (LongsPieceConfiguration) FENReader.read(
            STARTING_POSITION, LongsPieceConfiguration.class);

        final int undevelopedPlayerPieces = pieceConfiguration.countUndevelopedPiecesBySide(side);

        assertThat(undevelopedPlayerPieces).isEqualTo(16);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void countUndevelopedPiecesBySide_kingsOnly(int side) {
        LongsPieceConfiguration pieceConfiguration = (LongsPieceConfiguration) FENReader.read(
            "4k3/8/8/8/8/8/8/4K3 b KQkq - 0 1", LongsPieceConfiguration.class);

        final int undevelopedPlayerPieces = pieceConfiguration.countUndevelopedPiecesBySide(side);

        assertThat(undevelopedPlayerPieces).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void countUndevelopedPiecesBySide_swappedKings(int side) {
        LongsPieceConfiguration pieceConfiguration = (LongsPieceConfiguration) FENReader.read(
            "4K3/8/8/8/8/8/8/4k3 b KQkq - 0 1", LongsPieceConfiguration.class);

        final int undevelopedPlayerPieces = pieceConfiguration.countUndevelopedPiecesBySide(side);

        assertThat(undevelopedPlayerPieces).isEqualTo(0);
    }

    @Test
    void getLesserScore_favoursPieceDevelopment() {
        PieceConfiguration oneKnightMovedTwice = FENReader.read("rnbqkbnr/pppp1ppp/8/4p1N1/8/8/PPPPPPPP/RNBQKB1R b KQkq - 1 2", LongsPieceConfiguration.class);
        PieceConfiguration bothKnightsMovedOnce = FENReader.read("rnbqkbnr/pppp1ppp/8/4p3/8/2N2N2/PPPPPPP/R1BQKB1R b KQkq - 1 2", LongsPieceConfiguration.class);
        oneKnightMovedTwice.setHigherBitFlags();
        bothKnightsMovedOnce.setHigherBitFlags();

        final double lessDevelopedLesserScore = oneKnightMovedTwice.getLesserScore();
        final double moreDevelopedLesserScore = bothKnightsMovedOnce.getLesserScore();

        assertThat(moreDevelopedLesserScore)
            .as("The lesser score should favour piece development")
            .isLessThan(lessDevelopedLesserScore);
    }
}
