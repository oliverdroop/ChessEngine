package chess.api;

import chess.api.pieces.Piece;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class PositionEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PositionEvaluator.class);

    private static final int NO_CAPTURE_OR_PAWN_MOVE_LIMIT = 99;

    public static int getValueDifferential(PieceConfiguration pieceConfiguration) {
        int valueDifferential = 0;
        final int turnSide = pieceConfiguration.getTurnSide().ordinal();
        for (int positionBitFlag : pieceConfiguration.getPositionBitFlags()) {
            // Is it a piece?
            final int pieceBitFlag = positionBitFlag & PieceConfiguration.ALL_PIECE_FLAGS_COMBINED;
            if (pieceBitFlag == 0) {
                continue;
            }
            final int value = Piece.getValue(pieceBitFlag);
            // Is it a black piece?
            final int isBlackOccupied = (positionBitFlag & PieceConfiguration.BLACK_OCCUPIED) >> 9;
            // Is it a player or opposing piece?
            final int turnSideFactor = 1 - ((turnSide ^ isBlackOccupied) << 1);
            valueDifferential += value * turnSideFactor;
        }
        return valueDifferential;
    }

    public static PieceConfiguration getBestMoveRecursively(PieceConfiguration pieceConfiguration, int depth) {
        Optional<Map.Entry<PieceConfiguration, Double>> optionalBestEntry = getBestPieceConfigurationToScoreEntryRecursively(pieceConfiguration, depth, 1);
        return optionalBestEntry.map(Map.Entry::getKey).orElse(null);
    }

    public static double getBestScoreDifferentialRecursively(PieceConfiguration pieceConfiguration, int depth, int turnSideFactor) {
        Optional<Map.Entry<PieceConfiguration, Double>> optionalBestEntry = getBestPieceConfigurationToScoreEntryRecursively(pieceConfiguration, depth, turnSideFactor);
        if (optionalBestEntry.isPresent()) {
            Map.Entry<PieceConfiguration, Double> bestEntry = optionalBestEntry.get();
            if (bestEntry.getKey().getHalfMoveClock() > NO_CAPTURE_OR_PAWN_MOVE_LIMIT) {
                return -Float.MAX_VALUE;
            }
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
        for (PieceConfiguration onwardPieceConfiguration : onwardPieceConfigurations) {
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
        double threatValue = -(pieceConfiguration.countThreatFlags() / (double) 64);
        adjustValuesByConstant(pieceConfigurationValueMap, threatValue);

        return pieceConfigurationValueMap.entrySet().stream().min(entryComparator());
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
                .map(FENWriter::write)
                .collect(Collectors.toList());

        fenMap.put(fen, onwardFENs);
        return fenMap;
    }

    public static void addToFENMap(Map<String, Collection<String>> fenMap, String fen) {
        PieceConfiguration configuration = FENReader.read(fen);

        Collection<String> onwardFENs = configuration.getPossiblePieceConfigurations().stream()
                .map(FENWriter::write)
                .collect(Collectors.toList());

        fenMap.put(fen, onwardFENs);
    }

    public static void addToFENMapAsync(Map<String, Collection<String>> fenMap, String fen, ExecutorService executor) throws Exception {
        PieceConfiguration configuration = FENReader.read(fen);

        Callable pcCallable = new PCCallable(configuration);
        Future<List<PieceConfiguration>> onwardConfigurations = executor.submit(pcCallable);
        Collection<String> onwardFENs = onwardConfigurations.get().stream()
                .map(FENWriter::write)
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
