package chess.api.pieces;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static chess.api.PieceConfiguration.*;
import static org.assertj.core.api.Assertions.assertThat;

public class PieceTest {

    @ParameterizedTest
    @MethodSource("getPieceValues")
    void testGetValue(int pieceTypeFlag, int expectedValue) {
        final char pieceCode = Piece.getFENCode(pieceTypeFlag);
        assertThat(Piece.getValue(pieceTypeFlag))
                .as("Unexpected piece value for %c", pieceCode)
                .isEqualTo(expectedValue);
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
