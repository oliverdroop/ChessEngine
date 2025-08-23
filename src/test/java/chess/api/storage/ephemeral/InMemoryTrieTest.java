package chess.api.storage.ephemeral;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class InMemoryTrieTest {

//    private InMemoryTrie inMemoryTrie = new InMemoryTrie();

    @ParameterizedTest
    @MethodSource("getAlgebraicNotationArguments")
    void getMoveFromAlgebraicNotation(String algebraicNotation, short expectedMoveShort){
        assertThat(InMemoryTrie.getMoveFromAlgebraicNotation(algebraicNotation)).isEqualTo(expectedMoveShort);
    }

    private static Stream<Arguments> getAlgebraicNotationArguments() {
        return Stream.of(
            Arguments.of("b7a8", (short) 0b0000110001111000),
            Arguments.of("b7xa8q", (short) 0b1000110001111000),
            Arguments.of("b7xa8r", (short) 0b0100110001111000),
            Arguments.of("b7xa8b", (short) 0b0010110001111000),
            Arguments.of("b7xa8n", (short) 0b0001110001111000)
        );
        //b7xa8k
        //                b7xa8n
        //                b7xa8b
        //                a3a4
        //                a3xa4
    }
}
