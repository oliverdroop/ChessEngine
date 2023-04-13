package chess.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
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
        } else if (pieceConfiguration.isCheck()) {
            // Check
            return Integer.MAX_VALUE;
        }
        // Stalemate
        return 0;
    }

    public static Optional<Map.Entry<PieceConfiguration, Integer>> getBestPieceConfigurationToValueEntryRecursively(PieceConfiguration pieceConfiguration, int depth) {
        Map<PieceConfiguration, Integer> pieceConfigurationValueMap = new HashMap<>();

        depth--;
        List<PieceConfiguration> onwardPieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();
        onwardPieceConfigurations = onwardPieceConfigurations.stream().sorted().collect(Collectors.toList());
        for (PieceConfiguration onwardPieceConfiguration : onwardPieceConfigurations) {

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

    public static Map<String, Collection<String>> buildFENMap(String fen) {
        PieceConfiguration configuration = FENReader.read(fen);
        Map<String, Collection<String>> fenMap = new HashMap<>();

        Collection<String> onwardFENs = configuration.getPossiblePieceConfigurations().stream()
                .map(pc -> FENWriter.write(pc))
                .collect(Collectors.toList());

        fenMap.put(fen, onwardFENs);
        return fenMap;
    }

    public static void addToFENMap(Map<String, Collection<String>> fenMap, String fen) {
        PieceConfiguration configuration = FENReader.read(fen);

        Collection<String> onwardFENs = configuration.getPossiblePieceConfigurations().stream()
                .map(pc -> FENWriter.write(pc))
                .collect(Collectors.toList());

        fenMap.put(fen, onwardFENs);
    }

    public static void addToFENMapAsync(Map<String, Collection<String>> fenMap, String fen, ExecutorService executor) throws Exception {
        PieceConfiguration configuration = FENReader.read(fen);

        PCCallable pcCallable = new PCCallable(configuration);
        Future<List<PieceConfiguration>> onwardConfigurations = executor.submit(pcCallable);
        Collection<String> onwardFENs = onwardConfigurations.get().stream()
                .map(pc -> FENWriter.write(pc))
                .collect(Collectors.toList());

        fenMap.put(fen, onwardFENs);
    }

    public static Comparator<PieceConfiguration> pieceConfigurationComparator() {
        return Comparator.comparingInt(PositionEvaluator::getValueDifferential).reversed();
    }

    public static Comparator<Map.Entry<PieceConfiguration, Integer>> entryComparator() {
        return Comparator.comparingInt(Map.Entry::getValue);
    }
}
