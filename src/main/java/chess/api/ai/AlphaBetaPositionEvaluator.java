package chess.api.ai;

import chess.api.configuration.PieceConfiguration;
import chess.api.storage.ephemeral.SawtoothShortArrayComparator;
import com.google.common.collect.TreeMultimap;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static chess.api.configuration.PieceConfiguration.toNewConfigurationFromMove;
import static chess.api.configuration.PieceConfiguration.toNewConfigurationFromMoves;

public class AlphaBetaPositionEvaluator {

    private static final Comparator<short[]> SHORT_ARRAY_COMPARATOR = new SawtoothShortArrayComparator();

    private static final Supplier<TreeMap<Double, PieceConfiguration>> TREE_MAP_SUPPLIER = () -> new TreeMap<>(Comparator.reverseOrder());

    public static PieceConfiguration getBestMoveRecursively(PieceConfiguration originalConfiguration, int depth) {
        final TreeMap<Double, PieceConfiguration> treeMap = originalConfiguration.getOnwardConfigurations()
            .stream()
            .collect(Collectors.toMap(
                pc -> -alphaBetaMax(pc, -Double.MAX_VALUE, Double.MAX_VALUE, depth - 1),
                Function.identity(),
                (d1, d2) -> d1,
                TREE_MAP_SUPPLIER));
        return treeMap
            .values()
            .stream()
            .findFirst()
            .orElse(null);
    }

    private static Double getEndgameValue(int onwardConfigurationCount, PieceConfiguration currentConfiguration) {
        if (onwardConfigurationCount == 0) {
            final double mateValue;
            if (currentConfiguration.isCheck()) {
                mateValue = Float.MAX_VALUE;
            } else {
                mateValue = -Float.MAX_VALUE;
            }
            return mateValue;
        }
        return null;
    }

    private static double alphaBetaMax(PieceConfiguration configuration, double alpha, double beta, int depthLeft) {
        final List<PieceConfiguration> childConfigurations = configuration.getOnwardConfigurations();
        final Double gameEndValue = getEndgameValue(childConfigurations.size(), configuration);
        if (gameEndValue != null) {
            return -gameEndValue * (Math.pow(0.99, 5 - depthLeft));
        }
        if (depthLeft == 0) {
//            return gameEndValue == null ? evaluate(configuration) : gameEndValue;
            return evaluate(configuration);
        }
        double bestValue = -Double.MAX_VALUE;
        for (PieceConfiguration childConfiguration : childConfigurations) {
            final double score = alphaBetaMin(childConfiguration, alpha, beta, depthLeft - 1);
            if (score > bestValue) {
                bestValue = score;
                if(score > alpha) {
                    alpha = score; // alpha acts like max in MiniMax
                }
            }
            if (score >= beta) {
                return score;   // fail soft beta-cutoff
            }
        }
        return bestValue;
    }

    private static double alphaBetaMin(PieceConfiguration configuration, double alpha, double beta, int depthLeft) {
        final List<PieceConfiguration> childConfigurations = configuration.getOnwardConfigurations();
        final Double gameEndValue = getEndgameValue(childConfigurations.size(), configuration);
        if (gameEndValue != null) {
            return gameEndValue * (Math.pow(0.99, 5 - depthLeft));
        }
        if (depthLeft == 0) {
//            return gameEndValue == null ? -evaluate(configuration) : gameEndValue;
            return -evaluate(configuration);
        }
        double bestValue = Double.MAX_VALUE;
        for (PieceConfiguration childConfiguration : childConfigurations) {
            final double score = alphaBetaMax(childConfiguration, alpha, beta, depthLeft - 1);
            if (score < bestValue) {
                bestValue = score;
                if (score < beta) {
                    beta = score; // beta acts like min in MiniMax
                }
            }
            if (score <= alpha) {
                return score; // fail soft alpha-cutoff, break can also be used here
            }
        }
        return bestValue;
    }

    private static double evaluate(PieceConfiguration configuration) {
        final int valueDifferential = configuration.getValueDifferential();
        return configuration.adjustForDraw(valueDifferential, true);
//        return configuration.getValueDifferential() + configuration.getLesserScore();
    }

//    public static PieceConfiguration getBestMoveRecursively2(PieceConfiguration originalConfiguration, int depth) {
//        final long startTime = Instant.now().toEpochMilli();
//        final long endTime = startTime + (depth * 100L);
//        final TreeMultimap<Double, short[]> multimap = TreeMultimap.create(Comparator.reverseOrder(), SHORT_ARRAY_COMPARATOR);
//        final Optional<short[]> optionalHistoricMoves = Optional.ofNullable(originalConfiguration.getHistoricMoves());
//        final short[] originalHistoricMoves = optionalHistoricMoves.orElse(new short[]{});
//        final double originalValue = originalConfiguration.getValueDifferential() + originalConfiguration.getLesserScore();
//        originalConfiguration.setHistoricMoves(originalHistoricMoves);
//        multimap.put(originalValue, originalHistoricMoves);
//
////        double bestScore = -Double.MAX_VALUE;
////        short[] bestMoveHistory = null;
//        while(Instant.now().toEpochMilli() < endTime) {
//            final TreeMultimap<Double, short[]> additionsMap = TreeMultimap.create(Comparator.reverseOrder(), SHORT_ARRAY_COMPARATOR);
//            final TreeMultimap<Double, short[]> removalsMap = TreeMultimap.create(Comparator.reverseOrder(), SHORT_ARRAY_COMPARATOR);
//            for(Map.Entry<Double, short[]> entry : multimap.entries()) {
//                final double score = entry.getKey();
//                final short[] moveHistory = entry.getValue();
//                final PieceConfiguration currentConfiguration = toNewConfigurationFromMoves(originalConfiguration, moveHistory);
//                final List<PieceConfiguration> childConfigurations = currentConfiguration.getOnwardConfigurations();
//
//                final Double gameEndValue = getEndgameValue(childConfigurations.size(), currentConfiguration);
//                if (gameEndValue != null) {
//                    additionsMap.put(gameEndValue, moveHistory);
//                    continue;
//                }
//
////                final double currentLesserScore = currentConfiguration.getLesserScore();
//                for(PieceConfiguration childConfiguration : childConfigurations) {
//                    final short[] moveHistoryPlusChildMove = childConfiguration.getHistoricMoves();
//                    final int turnAdjustment = 1 - ((moveHistoryPlusChildMove.length % 2) * 2);
////                    final int turnAdjustment = 1;
////                    final short move = moveHistoryPlusChildMove[moveHistoryPlusChildMove.length - 1];
//                    final double childScore = (childConfiguration.getValueDifferential() + childConfiguration.getLesserScore()) * -turnAdjustment;
//                    additionsMap.put(childScore, moveHistoryPlusChildMove);
//                }
//                removalsMap.put(score, moveHistory);
//            }
//            for(Map.Entry<Double, short[]> entry : removalsMap.entries()) {
//                multimap.remove(entry.getKey(), entry.getValue());
//            }
//            multimap.putAll(additionsMap);
//        }
//        final short[] bestMoveHistory = multimap.get(multimap.keySet().first()).first();
//        if (bestMoveHistory.length > 0) {
//            final short bestMove = bestMoveHistory[0];
//            return toNewConfigurationFromMove(originalConfiguration, bestMove);
//        }
//        return null;
//    }
}
