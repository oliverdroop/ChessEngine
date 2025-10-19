package chess.api;

import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import static chess.api.configuration.PieceConfiguration.*;
import static chess.api.configuration.PieceConfiguration.KNIGHT_OCCUPIED;
import static java.lang.String.format;

public class MoveDescriber {

    private static final Map<String, Integer> PROMOTION_BIT_FLAGS = Map.of(
        "q", QUEEN_OCCUPIED,
        "r", ROOK_OCCUPIED,
        "b", BISHOP_OCCUPIED,
        "n", KNIGHT_OCCUPIED
    );
    private static final Pattern POSITION_PATTERN = Pattern.compile("[a-h][1-8]");
    private static final Pattern PROMOTION_PATTERN = Pattern.compile("[qrbn]$");

    public static short describeMove(int posFrom, int posTo, int promotionTypeBitFlag) {
        final int outputInt = (promotionTypeBitFlag << 1) | (posFrom << 6) | posTo;
        return (short) outputInt;
    }

    public static short getMoveFromAlgebraicNotation(String algebraicNotation) {
        final int[] fromAndToPositions = POSITION_PATTERN.matcher(algebraicNotation)
            .results()
            .mapToInt(matchResult -> Position.getPosition(matchResult.group()))
            .toArray();

        if (fromAndToPositions.length != 2 || fromAndToPositions[0] == fromAndToPositions[1]) {
            throw new IllegalArgumentException(
                format("Unable to get move from algebraic notation %s", algebraicNotation));
        }
        final String promotionString = PROMOTION_PATTERN.matcher(algebraicNotation)
            .results()
            .map(MatchResult::group)
            .reduce(String::concat)
            .orElse("");
        final int promotionInt = PROMOTION_BIT_FLAGS.getOrDefault(promotionString, 0);
        return describeMove(fromAndToPositions[0], fromAndToPositions[1], promotionInt);
    }
}
