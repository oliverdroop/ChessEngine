package chess.api;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MoveDescriberTest {

    @ParameterizedTest
    @MethodSource("getAlgebraicNotationArguments")
    void getMoveFromAlgebraicNotation(String algebraicNotation, short expectedMoveInteger){
        assertThat(MoveDescriber.getMoveFromAlgebraicNotation(algebraicNotation)).isEqualTo(expectedMoveInteger);
    }

    @ParameterizedTest
    @ValueSource(strings = {"b7", "xa8q", "b7b7", "b6b7b8", "i1h1",  "b8b9", "", "nonsense"})
    void getMoveFromAlgebraicNotation_shouldThrowException(String algebraicNotation) {
        assertThatThrownBy(() -> MoveDescriber.getMoveFromAlgebraicNotation(algebraicNotation))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage(format("Unable to get move from algebraic notation %s", algebraicNotation));
    }

    private static Stream<Arguments> getAlgebraicNotationArguments() {
        return Stream.of(
            Arguments.of("b7a8", (short) 0b0000110001111000),
            Arguments.of("h2g1", (short) 0b0000001111000110),
            Arguments.of("b7xa8q", (short) 0b1000110001111000),
            Arguments.of("b7xa8r", (short) 0b0100110001111000),
            Arguments.of("b7xa8b", (short) 0b0010110001111000),
            Arguments.of("b7xa8n", (short) 0b0001110001111000)
        );
    }
}
