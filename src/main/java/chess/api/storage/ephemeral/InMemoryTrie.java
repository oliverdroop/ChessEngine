package chess.api.storage.ephemeral;

import chess.api.FENReader;
import chess.api.FENWriter;
import chess.api.PieceConfiguration;
import chess.api.Position;

import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class InMemoryTrie {

    private static final Map<String, Integer> PROMOTION_BIT_FLAGS = Map.of("q", 8, "r", 4, "b", 2, "n", 1);

    private final TrieNode rootNode;

    public InMemoryTrie() {
        final PieceConfiguration startingConfiguration = FENReader.read(FENWriter.STARTING_POSITION);
        final Set<Short> onwardMoves = startingConfiguration
            .getPossiblePieceConfigurations()
            .stream()
            .map(onwardConfiguration -> getMoveFromAlgebraicNotation(
                onwardConfiguration.getAlgebraicNotation(startingConfiguration)
            ))
            .collect(Collectors.toSet());
        this.rootNode = new TrieNode(Collections.emptyList(), onwardMoves);
    }

    static short getMoveFromAlgebraicNotation(String algebraicNotation) {
        final Pattern positionPattern = Pattern.compile("[a-h][1-8]");
        final Pattern promotionPattern = Pattern.compile("[qrbn]$");
        final int[] toAndFromPositions = positionPattern.matcher(algebraicNotation)
            .results()
            .mapToInt(matchResult -> Position.getPosition(matchResult.group()))
            .toArray();

        if (toAndFromPositions.length != 2) {
            throw new IllegalArgumentException(
                format("Unable to get move from algebraic notation %s", algebraicNotation));
        }
        final String promotionString = promotionPattern.matcher(algebraicNotation)
            .results()
            .map(MatchResult::group)
            .reduce(String::concat)
            .orElse("");
        final int promotionInt = PROMOTION_BIT_FLAGS.getOrDefault(promotionString, 0);
        final int outputInt = (promotionInt << 12) | (toAndFromPositions[0] << 6) | (toAndFromPositions[1]);
        return (short) outputInt;
    }
}
