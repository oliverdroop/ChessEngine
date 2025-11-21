package com.oliverdroop.chess.api.pieces;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.oliverdroop.chess.api.configuration.PieceConfiguration.*;
import static com.oliverdroop.chess.api.pieces.PieceType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PieceTest {

    @ParameterizedTest
    @MethodSource("getPieceValues")
    void testGetValue(int pieceTypeFlag, int expectedValue) {
        final char pieceCode = Piece.getFENCode(pieceTypeFlag);
        assertThat(Piece.getValue(pieceTypeFlag))
                .as("Unexpected piece value for %c", pieceCode)
                .isEqualTo(expectedValue);
    }

    @ParameterizedTest
    @MethodSource("getPieceTypeArguments")
    void testGetPieceType(int pieceTypeFlag, PieceType expected) {
        assertThat(Piece.getPieceType(pieceTypeFlag + 63 + THREATENED)).isEqualTo(expected);
    }

    @Test
    void testGetPieceType_throwsException() {
        assertThatThrownBy(() -> Piece.getPieceType(63 + THREATENED))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage("No piece type recognised from which to get PieceType enum");
    }

    @Test
    void testGetFENCode_throwsException() {
        assertThatThrownBy(() -> Piece.getFENCode(0))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage("No piece type recognised from which to get FEN code");
    }

    private static Stream<Arguments> getPieceValues() {
        return Stream.of(
                Arguments.of(KING_OCCUPIED, 0),
                Arguments.of(PAWN_OCCUPIED, 1),
                Arguments.of(KNIGHT_OCCUPIED, 3),
                Arguments.of(BISHOP_OCCUPIED, 3),
                Arguments.of(ROOK_OCCUPIED, 5),
                Arguments.of(QUEEN_OCCUPIED, 9)
        );
    }

    private static Stream<Arguments> getPieceTypeArguments() {
        return Stream.of(
            Arguments.of(KING_OCCUPIED, KING),
            Arguments.of(KNIGHT_OCCUPIED, KNIGHT),
            Arguments.of(BISHOP_OCCUPIED, BISHOP),
            Arguments.of(ROOK_OCCUPIED, ROOK),
            Arguments.of(QUEEN_OCCUPIED, QUEEN),
            Arguments.of(PAWN_OCCUPIED, PAWN)
        );
    }
}
