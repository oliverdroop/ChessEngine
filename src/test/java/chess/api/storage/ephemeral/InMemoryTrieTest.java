package chess.api.storage.ephemeral;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class InMemoryTrieTest {

    private InMemoryTrie inMemoryTrie = new InMemoryTrie();

    @ParameterizedTest
    @MethodSource("getAlgebraicNotationArguments")
    void getMoveFromAlgebraicNotation(String algebraicNotation, short expectedMoveShort){
        assertThat(InMemoryTrie.getMoveFromAlgebraicNotation(algebraicNotation)).isEqualTo(expectedMoveShort);
    }

    @Test
    void getAvailableMoves_fromRoot() {
        Optional<Set<Short>> availableMoves = inMemoryTrie.getAvailableMoves(new short[]{});
        assertThat(availableMoves).isPresent();
        assertThat(availableMoves.get()).contains((short) 0b0000001000010000, (short) 0b0000001000011000);
    }

    @Test
    void getAvailableMoves_fromIllegalFirstMove() {
        Optional<Set<Short>> availableMoves = inMemoryTrie.getAvailableMoves(new short[]{0b0000000001111111});
        assertThat(availableMoves).isEmpty();
    }

    @Test
    void getAvailableMoves_fromValidFirstMove() {
        Optional<Set<Short>> availableMoves = inMemoryTrie.getAvailableMoves(new short[]{0b0000001000010000});
        assertThat(availableMoves).isPresent();
        assertThat(availableMoves.get()).contains((short) 0b0000111110101111);
    }

    @Test
    void getAvailableMoves_fromValidSecondMove() {
        Optional<Set<Short>> availableMoves = inMemoryTrie.getAvailableMoves(
            new short[]{0b0000001000010000, 0b0000111110101111});
        assertThat(availableMoves).isPresent();
        assertThat(availableMoves.get()).contains((short) 0b0000001001010001);
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
