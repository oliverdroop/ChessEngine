package chess.api;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static chess.api.PieceConfiguration.*;
import static org.assertj.core.api.Assertions.assertThat;

public class PositionTest {

    @ParameterizedTest
    @MethodSource("getCoordinateStrings")
    void testGetPositionFromCoordinateString(String coordinateString, int expectedPosition) {
        int result = Position.getPosition(coordinateString);
        assertThat(result)
                .as("Unexpected position bit flag returned for %s", coordinateString)
                .isEqualTo(expectedPosition);
    }

    @ParameterizedTest
    @MethodSource("getExpectedTranslationsTowardsThreat")
    void testApplyTranslationTowardsThreat(int directionalBitFlag, int expectedPosition) {
        final int startingPosition = 18;
        assertThat(Position.applyTranslationTowardsThreat(directionalBitFlag, startingPosition))
                .as("Unexpected translation towards threat")
                .isEqualTo(expectedPosition);
    }

    private static Stream<Arguments> getCoordinateStrings() {
        return Stream.of(
                Arguments.of("a1", 0),
                Arguments.of("h1", 7),
                Arguments.of("a8", 56),
                Arguments.of("h8", 63)
        );
    }

    private static Stream<Arguments> getExpectedTranslationsTowardsThreat() {
        return Stream.of(
                Arguments.of(DIRECTION_SW, 27),
                Arguments.of(DIRECTION_W, 19),
                Arguments.of(DIRECTION_NW, 11),
                Arguments.of(DIRECTION_N, 10),
                Arguments.of(DIRECTION_NE, 9),
                Arguments.of(DIRECTION_E, 17),
                Arguments.of(DIRECTION_SE, 25),
                Arguments.of(DIRECTION_S, 26),
                Arguments.of(DIRECTION_ANY_KNIGHT, 18)
        );
    }
}
