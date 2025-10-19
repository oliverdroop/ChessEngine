package chess.api.pieces;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static chess.api.configuration.PieceConfiguration.*;
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

    @Test
    void testGetPieceType_throwsException() {
        assertThatThrownBy(() -> Piece.getPieceType(0))
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
}
