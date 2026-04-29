package chess.api.ai;

import chess.api.configuration.PieceConfiguration;
import chess.api.storage.ephemeral.InMemoryTrie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static chess.api.configuration.PieceConfiguration.*;

public class BreadthFirstPositionEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(BreadthFirstPositionEvaluator.class);

    private static final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    public static PieceConfiguration getBestMoveRecursively(PieceConfiguration originalConfiguration, int depth) {
        final InMemoryTrie inMemoryTrie = new InMemoryTrie();
        final short[] initialHistoricMoves;
        if (originalConfiguration.getHistoricMoves() != null) {
            initialHistoricMoves = originalConfiguration.getHistoricMoves();
        } else {
            initialHistoricMoves = new short[]{};
            originalConfiguration.setHistoricMoves(initialHistoricMoves);
        }
        final int initialHistoricMovesLength = initialHistoricMoves.length;
        inMemoryTrie.setScore(initialHistoricMoves, originalConfiguration.getValueDifferential());
        int currentDepth = 0;

        while(currentDepth < depth) {
            final boolean isMaximumDepth = currentDepth >= depth - 1;
            final List<CompletableFuture<Void>> futures = new ArrayList<>();
            for(short[] historicMoves : inMemoryTrie.getTrieMap().keySet()) {
                if (historicMoves.length - initialHistoricMovesLength != currentDepth) {
                    continue;
                }
                final Runnable runnable = () -> calculateAndScoreOnwardConfigurations(
                    historicMoves, initialHistoricMovesLength, originalConfiguration, inMemoryTrie, isMaximumDepth);
                final CompletableFuture<Void> future = CompletableFuture.runAsync(runnable, executorService);
                futures.add(future);
            }
            futures.forEach(CompletableFuture::join);
            if (currentDepth > 1) {
                inMemoryTrie.prune(currentDepth);
            }
            currentDepth++;
        }

        final short bestMove = getBestOnwardMoveScorePair(inMemoryTrie, initialHistoricMoves).move();
        if (bestMove != -1) {
            final PieceConfiguration bestConfiguration = toNewConfigurationFromMove(originalConfiguration, bestMove);
            bestConfiguration.setHigherBitFlags();
            return bestConfiguration;
        }
        return null;
    }

    public static double accumulate(double parentValue, double childValue) {
        return -(childValue * 0.99) - parentValue;
    }

    private static void calculateAndScoreOnwardConfigurations(
        short[] historicMoves,
        int initialHistoricMovesLength,
        PieceConfiguration originalConfiguration,
        InMemoryTrie inMemoryTrie,
        boolean isMaximumDepth)
    {
        final short[] historicMovesFromOriginal = Arrays.copyOfRange(historicMoves, initialHistoricMovesLength, historicMoves.length);
        final PieceConfiguration currentConfiguration = PieceConfiguration.toNewConfigurationFromMoves(
            originalConfiguration, historicMovesFromOriginal);

        final List<PieceConfiguration> onwardConfigurations = currentConfiguration.getOnwardConfigurations();
        final Double gameEndValue = getEndgameValue(onwardConfigurations.size(), currentConfiguration);
        if (gameEndValue != null) {
            inMemoryTrie.setScore(currentConfiguration.getHistoricMoves(), gameEndValue);
            return;
        }
        final double currentLesserScore = currentConfiguration.getLesserScore();
        storeConfigurationScores(onwardConfigurations, inMemoryTrie, isMaximumDepth, currentLesserScore);
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

    private static void storeConfigurationScores(
        List<PieceConfiguration> onwardConfigurations,
        InMemoryTrie inMemoryTrie,
        boolean isMaximumDepth,
        double currentLesserScore)
    {
        PieceConfiguration bestOnwardConfiguration = null;
        double bestOnwardScore = -Double.MAX_VALUE;
        for(PieceConfiguration onwardConfiguration : onwardConfigurations) {
            final double onwardScore = getConfigurationScore(onwardConfiguration, currentLesserScore, isMaximumDepth);
            if (!isMaximumDepth) {
                // Store all the onward scores because we are not yet at the maximum depth
                final short[] key = onwardConfiguration.getHistoricMoves();
                inMemoryTrie.setScore(key, onwardScore);
            } else if (onwardScore > bestOnwardScore) {
                // Calculate which score to store because we are at the maximum depth
                bestOnwardScore = onwardScore;
                bestOnwardConfiguration = onwardConfiguration;
            }
        }
        if (isMaximumDepth) {
            // Only store the best score because we are at the maximum depth
            final short[] key = bestOnwardConfiguration.getHistoricMoves();
            inMemoryTrie.setScore(key, bestOnwardScore);
        }
    }

    private static double getConfigurationScore(
        PieceConfiguration onwardConfiguration,
        double currentLesserScore,
        boolean isMaximumDepth)
    {
        // Set all the bit flags in the onward configuration
        final int onwardValueComparison = onwardConfiguration.getDrawResult(
            onwardConfiguration.getValueDifferential(), !isMaximumDepth).score();
        return onwardValueComparison + currentLesserScore;
    }

    private static MoveScorePair getBestOnwardMoveScorePair(InMemoryTrie inMemoryTrie, short[] startingNode) {
        final Map<short[], Double> childMap = inMemoryTrie.getChildren(startingNode);
        return getBestMoveScorePair(inMemoryTrie, childMap);
    }

    private static MoveScorePair getBestMoveScorePair(InMemoryTrie inMemoryTrie, Map<short[], Double> siblingMap) {
        short bestMove = -1;
        double bestScore = -Double.MAX_VALUE;
        for(short[] historicMoves : siblingMap.keySet()) {
            final double value = getCumulativeValue(historicMoves, inMemoryTrie);
            if (value > bestScore) {
                bestMove = historicMoves[historicMoves.length - 1];
                bestScore = value;
            }
        }
        return new MoveScorePair(bestMove, bestScore);
    }

    private static double getCumulativeValue(short[] historicMoves, InMemoryTrie inMemoryTrie) {
        final double value = inMemoryTrie.getScore(historicMoves);
        final Map<short[], Double> children = inMemoryTrie.getChildren(historicMoves);
        if (children.isEmpty()) {
            return value;
        }
        final MoveScorePair bestChildMove = getBestMoveScorePair(inMemoryTrie, children);
        if (bestChildMove.move() != -1) {
            return accumulate(value, bestChildMove.score());
        }
        return value;
    }
}
