package chess.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class PositionEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PositionEvaluator.class);

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
        Optional<Map.Entry<PieceConfiguration, Integer>> optionalBestEntry = getBestPieceConfigurationToValueEntryRecursively(pieceConfiguration, depth);
        if (optionalBestEntry.isPresent()) {
            return optionalBestEntry.get().getKey();
        }
        return null;
    }

    public static int getBestValueDifferentialRecursively(PieceConfiguration pieceConfiguration, int depth) {
        Optional<Map.Entry<PieceConfiguration, Integer>> optionalBestEntry = getBestPieceConfigurationToValueEntryRecursively(pieceConfiguration, depth);
        if (optionalBestEntry.isPresent()) {
            return optionalBestEntry.get().getValue();
        }
        return Integer.MIN_VALUE;
    }

    public static Optional<Map.Entry<PieceConfiguration, Integer>> getBestPieceConfigurationToValueEntryRecursively(PieceConfiguration pieceConfiguration, int depth) {
        Map<PieceConfiguration, Integer> pieceConfigurationValueMap = new HashMap<>();

        depth--;
        for (PieceConfiguration onwardPieceConfiguration : pieceConfiguration.getPossiblePieceConfigurations()) {
            if (onwardPieceConfiguration == null) {
                continue;
            }

            Integer valueDiff = null;
            if (depth > 0) {
                int turnSideFactor = 1 - ((depth % 2) * 2);
                valueDiff = turnSideFactor * getBestValueDifferentialRecursively(onwardPieceConfiguration, depth);
            } else {
                valueDiff = -getValueDifferential(onwardPieceConfiguration);
            }
            pieceConfigurationValueMap.put(onwardPieceConfiguration, valueDiff);
        }

        return pieceConfigurationValueMap.entrySet().stream()
                .sorted(entryComparator())
                .findFirst();
    }

    public static Comparator<PieceConfiguration> pieceConfigurationComparator() {
        return Comparator.comparingInt(PositionEvaluator::getValueDifferential).reversed();
    }

    public static Comparator<Map.Entry<PieceConfiguration, Integer>> entryComparator() {
        return Comparator.comparingInt(Map.Entry::getValue);
    }
}
