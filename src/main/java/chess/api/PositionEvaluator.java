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
        Optional<Object[]> optionalBestEntry = getBestPieceConfigurationToScoreEntryRecursively(pieceConfiguration, depth, 1);
        return (PieceConfiguration) optionalBestEntry.map(obj -> obj[0]).orElse(null);
    }

    public static double getBestScoreDifferentialRecursively(PieceConfiguration pieceConfiguration, int depth, int turnSideFactor) {
        Optional<Object[]> optionalBestEntry = getBestPieceConfigurationToScoreEntryRecursively(pieceConfiguration, depth, turnSideFactor);
        if (optionalBestEntry.isPresent()) {
            Object[] bestEntry = optionalBestEntry.get();
            if (((PieceConfiguration) bestEntry[0]).getHalfMoveClock() > NO_CAPTURE_OR_PAWN_MOVE_LIMIT) {
                return -Float.MAX_VALUE;
            }
            return turnSideFactor * ((double) bestEntry[1]);
        } else if (pieceConfiguration.isCheck()) {
            // Checkmate
            return Float.MAX_VALUE;
        }
        // Stalemate
        return -Float.MAX_VALUE;
    }

    public static Optional<Object[]> getBestPieceConfigurationToScoreEntryRecursively(PieceConfiguration pieceConfiguration, int depth, int turnSideFactor) {
//        Map<PieceConfiguration, Double> pieceConfigurationValueMap = new HashMap<>();
        final double currentDiff = getValueDifferential(pieceConfiguration);

        depth--;
        List<PieceConfiguration> onwardPieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();
        final int onwardConfigurationCount = onwardPieceConfigurations.size();
        final double[] onwardConfigurationScores = new double[onwardConfigurationCount];
        for (int i = 0; i < onwardConfigurationCount; i++) {
            PieceConfiguration onwardPieceConfiguration = onwardPieceConfigurations.get(i);
            double nextDiff = getValueDifferential(onwardPieceConfiguration);
            if (depth > 0) {
                nextDiff += getBestScoreDifferentialRecursively(onwardPieceConfiguration, depth, -turnSideFactor);
                // Below is where the position can be evaluated for more than just the value differential (because the position bit flags have been calculated)
            }
            onwardConfigurationScores[i] = nextDiff - currentDiff;
        }

        final double threatValue = -(pieceConfiguration.countThreatFlags() / (double) 64);
        int bestOnwardConfigurationIndex = -1;
        double bestOnwardConfigurationScore = -Double.MAX_VALUE;
        for(int i = 0; i < onwardConfigurationCount; i++) {
            double onwardConfigurationScore = onwardConfigurationScores[i] + threatValue;
            if (onwardConfigurationScore > bestOnwardConfigurationScore) {
                bestOnwardConfigurationScore = onwardConfigurationScore;
                bestOnwardConfigurationIndex = i;
            }
        }

        if (bestOnwardConfigurationIndex >= 0) {
            return Optional.of(new Object[]{onwardPieceConfigurations.get(bestOnwardConfigurationIndex), bestOnwardConfigurationScore});
        }
        return Optional.empty();
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
