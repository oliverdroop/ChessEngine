package chess.api.ai;

import chess.api.FENReader;
import chess.api.FENWriter;
import chess.api.PieceConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class OpeningBookTest {

    @Test
    void testOpeningBookIsFullyTraversable() {
        List<String> values = OpeningBook.openings.values().stream().flatMap(Collection::stream).toList();
        for(String key : OpeningBook.openings.keySet()) {
            if (!FENWriter.STARTING_POSITION.equals(key)) {
                assertThat(values).contains(key);
            }
        }
    }

    @Test
    void testOpeningBookResponsesAreAllValid() {
        for(String key : OpeningBook.openings.keySet()) {
            List<String> values = OpeningBook.openings.get(key);
            PieceConfiguration parentConfiguration = FENReader.read(key);
            List<PieceConfiguration> childConfigurations = parentConfiguration.getPossiblePieceConfigurations();
            List<String> actualChildFENs = childConfigurations.stream().map(PieceConfiguration::toString).toList();

            for(String value : values) {
                assertThat(actualChildFENs).contains(value);
            }
        }
    }
}
