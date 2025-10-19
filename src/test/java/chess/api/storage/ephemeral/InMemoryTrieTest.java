package chess.api.storage.ephemeral;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class InMemoryTrieTest {

    @Test
    void testSetScore_returnsNullForNullInput() {
        final InMemoryTrie inMemoryTrie = new InMemoryTrie();
        assertThatThrownBy(() -> inMemoryTrie.setScore(null, 0.5))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot set the score for a null collection of moves");

    }
}
