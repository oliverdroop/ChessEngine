package chess.api.ai.openings;

import chess.api.FENReader;
import chess.api.PieceConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class OpeningBookTest {

    @Test
    void testOpeningBookResponsesAreAllValid() {
        for(Opening opening : OpeningBook.getOpenings()) {
            for(int fenIndex = 0; fenIndex < opening.fens.size() - 1; fenIndex++) {
                final String currentFen = opening.fens.get(fenIndex);
                final String nextFen = opening.fens.get(fenIndex + 1);

                final PieceConfiguration parentConfiguration = FENReader.read(currentFen);
                final PieceConfiguration[] childConfigurations = parentConfiguration.getOnwardConfigurations();
                final List<String> actualChildFENs = Arrays.stream(childConfigurations).map(PieceConfiguration::toString).toList();

                assertThat(actualChildFENs).contains(nextFen);
            }
        }
    }
}
