package chess.api.ai.openings;

import chess.api.FENReader;
import chess.api.configuration.IntsPieceConfiguration;
import chess.api.configuration.LongsPieceConfiguration;
import chess.api.configuration.PieceConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class OpeningBookTest {

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testOpeningBookResponsesAreAllValid(Class<? extends PieceConfiguration> configurationClass) {
        for(Opening opening : OpeningBook.getOpenings()) {
            for(int fenIndex = 0; fenIndex < opening.fens.size() - 1; fenIndex++) {
                final String currentFen = opening.fens.get(fenIndex);
                final String nextFen = opening.fens.get(fenIndex + 1);

                final PieceConfiguration parentConfiguration = FENReader.read(currentFen, configurationClass);
                final List<PieceConfiguration> childConfigurations = parentConfiguration.getOnwardConfigurations();
                final List<String> actualChildFENs = childConfigurations.stream().map(PieceConfiguration::toString).toList();

                assertThat(actualChildFENs).contains(nextFen);
            }
        }
    }
}
