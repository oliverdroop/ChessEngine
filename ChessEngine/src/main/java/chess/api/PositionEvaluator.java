package chess.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Callable;
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

    public static int getCentrePositionDifferential(PieceConfiguration pieceConfiguration) {
        return 0;
//        Map<Side, Long> map = pieceConfiguration.getPieces().stream()
//                .filter(piece -> Arrays.stream(PieceConfiguration.CENTRE_POSITIONS)
//                        .anyMatch(centrePosition -> centrePosition == piece.getPosition()))
//                .collect(Collectors.groupingBy(Piece::getSide, Collectors.counting()));
//        int turnSideCentralPieceCount = map.containsKey(pieceConfiguration.getTurnSide())
//                ? map.get(pieceConfiguration.getTurnSide()).intValue()
//                : 0;
//        int opposingSideCentralPieceCount = map.containsKey(pieceConfiguration.getOpposingSide())
//                ? map.get(pieceConfiguration.getOpposingSide()).intValue()
//                : 0;
//        return turnSideCentralPieceCount - opposingSideCentralPieceCount;
    }

    public static PieceConfiguration getBestMoveRecursively(PieceConfiguration pieceConfiguration, int depth) {
        Optional<Map.Entry<PieceConfiguration, Double>> optionalBestEntry = getBestPieceConfigurationToScoreEntryRecursively(pieceConfiguration, depth, 1);
        if (optionalBestEntry.isPresent()) {
            return optionalBestEntry.get().getKey();
        }
        return null;
    }

    public static double getBestScoreDifferentialRecursively(PieceConfiguration pieceConfiguration, int depth, int turnSideFactor) {
        Optional<Map.Entry<PieceConfiguration, Double>> optionalBestEntry = getBestPieceConfigurationToScoreEntryRecursively(pieceConfiguration, depth, turnSideFactor);
        if (optionalBestEntry.isPresent()) {
//            int turnSideFactor = isTurnSide ? 1 : -1;
            return turnSideFactor * optionalBestEntry.get().getValue();
        } else if (pieceConfiguration.isCheck()) {
            // Checkmate
            return Float.MAX_VALUE;
        }
        // Stalemate
        return -Float.MAX_VALUE;
    }

    public static Optional<Map.Entry<PieceConfiguration, Double>> getBestPieceConfigurationToScoreEntryRecursively(PieceConfiguration pieceConfiguration, int depth, int turnSideFactor) {
        Map<PieceConfiguration, Double> pieceConfigurationValueMap = new HashMap<>();
        double currentDiff = getValueDifferential(pieceConfiguration);

        depth--;
        List<PieceConfiguration> onwardPieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();
        for (int i = 0; i < onwardPieceConfigurations.size(); i++){
            PieceConfiguration onwardPieceConfiguration = onwardPieceConfigurations.get(i);
            double nextDiff = getValueDifferential(onwardPieceConfiguration);
            if (depth > 0) {
                nextDiff += getBestScoreDifferentialRecursively(onwardPieceConfiguration, depth, -turnSideFactor);
                // Below is where the position can be evaluated for more than just the value differential (because the position bit flags have been calculated)
            }/* else {
                nextDiff = getValueDifferential(onwardPieceConfiguration);
            }*/
            pieceConfigurationValueMap.put(onwardPieceConfiguration, nextDiff - currentDiff);
        }

//        adjustValuesByDecimatedAverage(pieceConfigurationValueMap);
        double threatValue = -(pieceConfiguration.countThreatFlags() / 64);
        adjustValuesByConstant(pieceConfigurationValueMap, threatValue);

        return pieceConfigurationValueMap.entrySet().stream()
                .sorted(entryComparator())
                .findFirst();
    }

    private static void adjustValuesByConstant(Map<PieceConfiguration, Double> pieceConfigurationValueMap, double constant) {
        if (Math.abs(constant) >= 1) {
            throw new RuntimeException("Adjusting a pieceConfigurationValueMap value by an absolute value greater than 1 is a bad idea");
        }
        pieceConfigurationValueMap.entrySet().forEach(e -> e.setValue(e.getValue() + constant));
    }

    private static void adjustValuesByDecimatedAverage(Map<PieceConfiguration, Double> pieceConfigurationValueMap) {
        // Get an average value for all the onward piece configurations
        double total = 0;
        int count = 0;
        for (double value : pieceConfigurationValueMap.values()) {
            value = value > 9 ? 9 : value;
            value = value < -9 ? -9 : value;
            total += value;
            count ++;
        }
        double decimatedAverage = count > 0 ? total / (count * 10) : 0;
        adjustValuesByConstant(pieceConfigurationValueMap, decimatedAverage);
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

        Callable pcCallable = new PCCallable(configuration);
        Future<List<PieceConfiguration>> onwardConfigurations = executor.submit(pcCallable);
        Collection<String> onwardFENs = onwardConfigurations.get().stream()
                .map(pc -> FENWriter.write(pc))
                .collect(Collectors.toList());

        fenMap.put(fen, onwardFENs);
    }

    public static Comparator<PieceConfiguration> pieceConfigurationComparator() {
        return Comparator.comparingInt(PositionEvaluator::getValueDifferential).reversed();
    }

    public static Comparator<Map.Entry<PieceConfiguration, Double>> entryComparator() {
        return Comparator.<Map.Entry<PieceConfiguration, Double>>comparingDouble(Map.Entry::getValue)
                .thenComparing(Map.Entry::getKey)
                .reversed();
    }
}
