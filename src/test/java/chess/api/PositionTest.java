package chess.api;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

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

    private static Stream<Arguments> getCoordinateStrings() {
        return Stream.of(
                Arguments.of("a1", 0),
                Arguments.of("h1", 7),
                Arguments.of("a8", 56),
                Arguments.of("h8", 63)
        );
    }
}
