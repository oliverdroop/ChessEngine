package chess.api.ai.openings;

import chess.api.FENReader;
import chess.api.FENWriter;
import chess.api.PieceConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Collection;
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
                final List<PieceConfiguration> childConfigurations = parentConfiguration.getPossiblePieceConfigurations();
                final List<String> actualChildFENs = childConfigurations.stream().map(PieceConfiguration::toString).toList();

                assertThat(actualChildFENs).contains(nextFen);
            }
        }
    }
}
