package chess.api;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class PositionEvaluator {

    public static int getValueDifferential(PieceConfiguration pieceConfiguration) {
        Map<Side, Integer> valueMap = pieceConfiguration.getPieces().stream()
                .collect(Collectors.groupingBy(piece -> piece.getSide(), Collectors.summingInt(piece -> piece.getValue())));
        Integer turnSideValue = valueMap.get(pieceConfiguration.getTurnSide());
        Integer opposingSideValue = valueMap.get(pieceConfiguration.getTurnSide().getOpposingSide());
        if (turnSideValue != null && opposingSideValue != null) {
            return turnSideValue - opposingSideValue;
        } else if (turnSideValue == null && opposingSideValue == null) {
            return 0;
        } else if (turnSideValue == null) {
            return -opposingSideValue;
        } else if (opposingSideValue == null) {
            return turnSideValue;
        }
        return 0;
    }

    public static PieceConfiguration getBestMoveRecursively(PieceConfiguration pieceConfiguration, int depth) {
        int currentDepth = depth;
        while (currentDepth > 0) {
            List<PieceConfiguration> nextConfigurations = pieceConfiguration.getPossiblePieceConfigurations();
            Optional<PieceConfiguration> bestConfiguration = nextConfigurations.stream()
                    .sorted(Comparator.comparingInt(PositionEvaluator::getValueDifferential))
                    .findFirst();

            currentDepth--;
            if (bestConfiguration.isPresent()) {
                return getBestMoveRecursively(bestConfiguration.get(), currentDepth);
            }
        }
        return null;
    }
}
