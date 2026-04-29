package chess.api.ai;

import chess.api.configuration.PieceConfiguration;
import chess.api.storage.ephemeral.SawtoothShortArrayComparator;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.IntToDoubleFunction;
import java.util.function.Supplier;

public class AlphaBetaPositionEvaluator {

    private static final Comparator<short[]> SHORT_ARRAY_COMPARATOR = new SawtoothShortArrayComparator();
    private static final Comparator<PieceConfiguration> PIECE_CONFIGURATION_COMPARATOR = (pc1, pc2) -> SHORT_ARRAY_COMPARATOR.compare(pc1.getHistoricMoves(), pc2.getHistoricMoves());
    private static final Supplier<SortedMap<Double, SortedSet<PieceConfiguration>>> TREE_MAP_SUPPLIER = () -> new ConcurrentSkipListMap<>(Comparator.reverseOrder());
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newVirtualThreadPerTaskExecutor();
    private static final IntToDoubleFunction DEPTH_DECAY_FUNCTION = depthLeft -> Math.pow(1.01, depthLeft);

    public static PieceConfiguration getBestMoveRecursively(PieceConfiguration originalConfiguration, int depth) {
        final SortedMap<Double, SortedSet<PieceConfiguration>> treeMap = TREE_MAP_SUPPLIER.get();
        originalConfiguration.getOnwardConfigurations()
            .parallelStream()
            .map(childConfiguration -> getCalculationFuture(childConfiguration, treeMap, depth))
            .forEach(CompletableFuture::join);
        return treeMap
            .values()
            .stream()
            .findFirst()
            .map(SortedSet::first)
            .orElse(null);
    }

    private static CompletableFuture<Void> getCalculationFuture(
        PieceConfiguration childConfiguration,
        SortedMap<Double, SortedSet<PieceConfiguration>> treeMap,
        int depth
    ) {
        return CompletableFuture.runAsync(
            () -> calculateAndAddToMap(childConfiguration, treeMap, depth),
            EXECUTOR_SERVICE);
    }

    private static void calculateAndAddToMap(
        PieceConfiguration childConfiguration,
        SortedMap<Double, SortedSet<PieceConfiguration>> treeMap,
        int depth
    ) {
        final DrawResult drawResult = childConfiguration.getDrawResult(childConfiguration.getValueDifferential(), true);
        final double score = drawResult.isDraw() ? -drawResult.score() : -alphaBetaMax(childConfiguration, -Double.MAX_VALUE, Double.MAX_VALUE, depth - 1);
        final SortedSet<PieceConfiguration> singletonSet = new TreeSet<>(PIECE_CONFIGURATION_COMPARATOR);
        singletonSet.add(childConfiguration);
        treeMap.merge(score, singletonSet, (set1, set2) -> {
            set1.addAll(set2);
            return set1;
        });
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
        if (depthLeft == 0) {
            return evaluate(configuration);
        }
        final List<PieceConfiguration> childConfigurations = configuration.getOnwardConfigurations();
        final Double gameEndValue = getEndgameValue(childConfigurations.size(), configuration);
        if (gameEndValue != null) {
            return -gameEndValue * DEPTH_DECAY_FUNCTION.applyAsDouble(depthLeft);
        }
        double bestValue = -Double.MAX_VALUE;
        for (PieceConfiguration childConfiguration : childConfigurations) {
            final DrawResult drawResult = childConfiguration.getDrawResult(childConfiguration.getValueDifferential(), true);
            final double score = drawResult.isDraw() ? drawResult.score() : alphaBetaMin(childConfiguration, alpha, beta, depthLeft - 1);
            if (score > bestValue) {
                bestValue = score;
                if(score > alpha) {
                    alpha = score;
                }
            }
            if (score >= beta) {
                return score;
            }
        }
        return bestValue;
    }

    private static double alphaBetaMin(PieceConfiguration configuration, double alpha, double beta, int depthLeft) {
        if (depthLeft == 0) {
            return -evaluate(configuration);
        }
        final List<PieceConfiguration> childConfigurations = configuration.getOnwardConfigurations();
        final Double gameEndValue = getEndgameValue(childConfigurations.size(), configuration);
        if (gameEndValue != null) {
            return gameEndValue * DEPTH_DECAY_FUNCTION.applyAsDouble(depthLeft);
        }
        double bestValue = Double.MAX_VALUE;
        for (PieceConfiguration childConfiguration : childConfigurations) {
            final DrawResult drawResult = childConfiguration.getDrawResult(childConfiguration.getValueDifferential(), true);
            final double score = drawResult.isDraw() ? drawResult.score() : alphaBetaMax(childConfiguration, alpha, beta, depthLeft - 1);
            if (score < bestValue) {
                bestValue = score;
                if (score < beta) {
                    beta = score;
                }
            }
            if (score <= alpha) {
                return score;
            }
        }
        return bestValue;
    }

    private static double evaluate(PieceConfiguration configuration) {
        final int valueDifferential = configuration.getValueDifferential();
        final DrawResult drawResult = configuration.getDrawResult(valueDifferential, false);
        return drawResult.score();
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
