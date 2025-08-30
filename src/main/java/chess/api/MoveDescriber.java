package chess.api;

import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import static chess.api.PieceConfiguration.*;
import static chess.api.PieceConfiguration.KNIGHT_OCCUPIED;
import static java.lang.String.format;

public class MoveDescriber {

    private static final Map<String, Integer> PROMOTION_BIT_FLAGS = Map.of(
        "q", QUEEN_OCCUPIED,
        "r", ROOK_OCCUPIED,
        "b", BISHOP_OCCUPIED,
        "n", KNIGHT_OCCUPIED
    );

    public static short describeMove(int posFrom, int posTo, int promotionTypeBitFlag) {
        final int outputInt = (promotionTypeBitFlag << 1) | (posFrom << 6) | posTo;
        return (short) outputInt;
    }

    public static short getMoveFromAlgebraicNotation(String algebraicNotation) {
        final Pattern positionPattern = Pattern.compile("[a-h][1-8]");
        final Pattern promotionPattern = Pattern.compile("[qrbn]$");
        final int[] fromAndToPositions = positionPattern.matcher(algebraicNotation)
            .results()
            .mapToInt(matchResult -> Position.getPosition(matchResult.group()))
            .toArray();

        if (fromAndToPositions.length != 2) {
            throw new IllegalArgumentException(
                format("Unable to get move from algebraic notation %s", algebraicNotation));
        }
        final String promotionString = promotionPattern.matcher(algebraicNotation)
            .results()
            .map(MatchResult::group)
            .reduce(String::concat)
            .orElse("");
        final int promotionInt = PROMOTION_BIT_FLAGS.getOrDefault(promotionString, 0);
        return describeMove(fromAndToPositions[0], fromAndToPositions[1], promotionInt);
    }
}
