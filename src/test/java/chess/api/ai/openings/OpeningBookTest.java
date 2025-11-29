package chess.api.ai.openings;

import chess.api.FENReader;
import chess.api.FENWriter;
import chess.api.configuration.IntsPieceConfiguration;
import chess.api.configuration.LongsPieceConfiguration;
import chess.api.configuration.PieceConfiguration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.Stream;

import static java.lang.String.format;
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

    @ParameterizedTest
    @MethodSource("getOpeningBookArguments")
    void testGetOpeningBookResponse(String inputFen, boolean expectOutput, String reason) {
        final PieceConfiguration response = OpeningBook.getOpeningResponse(
            FENReader.read(inputFen, LongsPieceConfiguration.class));

        final String message = format("Expected the output %sto be null %s", expectOutput ? "not " : "", reason);
        assertThat(response != null).as(message).isEqualTo(expectOutput);
    }

    private static Stream<Arguments> getOpeningBookArguments() {
        return Stream.of(
            Arguments.of(
                FENWriter.STARTING_POSITION,
                true,
                "because the input was the starting position"),
            Arguments.of(
                "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
                true,
                "because the input was a standard opening"),
            Arguments.of(
                "rnbqkb1r/pp3ppp/3p1n2/2pP4/8/2N5/PP2PPPP/R1BQKBNR w KQkq - 0 6",
                false,
                "because the input was the very end of a standard opening"
            ),
            Arguments.of(
                "rnb1kb1r/pp3ppp/3p1n2/q1pP4/4P3/2N5/PP3PPP/R1BQKBNR w KQkq - 1 7",
                false,
                "because the input was too late in the game"),
            Arguments.of(
                "rnbqkbnr/pp1ppppp/8/2p5/4PP2/8/PPPP2PP/RNBQKBNR b KQkq f3 0 2",
                false,
                "because the input was not a standard opening")
        );
    }
}
