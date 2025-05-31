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

    @ParameterizedTest
    @MethodSource("getExpectedTranslations")
    void testApplyTranslation(int position, int x, int y, boolean isNotNegative) {
        assertThat(Position.applyTranslation(position, x, y) >= 0).isEqualTo(isNotNegative);
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
                Arguments.of(DIRECTION_ANY_KNIGHT, -1)
        );
    }

    private static Stream<Arguments> getExpectedTranslations() {
        return Stream.of(
            Arguments.of(0, 0, 0, true),
            Arguments.of(0, 0, 1, true),
            Arguments.of(0, 0, 2, true),
            Arguments.of(0, 1, 0, true),
            Arguments.of(0, 1, 1, true),
            Arguments.of(0, 1, 2, true),
            Arguments.of(0, 2, 2, true),
            Arguments.of(0, -1, 0, false),
            Arguments.of(0, -1, 1, false),
            Arguments.of(0, -1, -1, false),
            Arguments.of(0, -1, -2, false),
            Arguments.of(0, -2, 0, false),
            Arguments.of(0, -2, -1, false),
            Arguments.of(7, -1, 0, true),
            Arguments.of(7, 0, 1, true),
            Arguments.of(7, 0, -1, false),
            Arguments.of(63, 1, 0, false),
            Arguments.of(63, 0, 1, false),
            Arguments.of(56, 1, 0, true),
            Arguments.of(56, -1, 0, false),
            Arguments.of(56, 0, 1, false)
        );
    }
}
