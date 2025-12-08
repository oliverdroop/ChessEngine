package chess.api.ai;

import chess.api.configuration.PieceConfiguration;
import chess.api.storage.ephemeral.InMemoryTrie;

import java.util.*;

import static chess.api.configuration.PieceConfiguration.*;
import static java.util.Arrays.copyOfRange;

public class BreadthFirstPositionEvaluator {

    public static PieceConfiguration getBestMoveRecursively(PieceConfiguration originalConfiguration, int depth) {
        final InMemoryTrie inMemoryTrie = new InMemoryTrie();
        final short[] initialHistoricMoves;
        if (originalConfiguration.getHistoricMoves() != null) {
            initialHistoricMoves = originalConfiguration.getHistoricMoves();
        } else {
            initialHistoricMoves = new short[]{};
        }
        final int initialHistoricMovesLength = initialHistoricMoves.length;
        originalConfiguration.setHistoricMoves(initialHistoricMoves);
        inMemoryTrie.setScore(initialHistoricMoves, 0.0);
        PieceConfiguration currentConfiguration;
        PieceConfiguration parentConfiguration = null;
        int currentDepth = 0;

        while(currentDepth < depth) {
            final boolean isMaximumDepth = currentDepth >= depth - 1;
            final Map<short[], Double> trieMapCopy = new TreeMap<>(inMemoryTrie.getTrieMap());
            for(short[] historicMoves : trieMapCopy.keySet()) {
                if (historicMoves.length - initialHistoricMovesLength != currentDepth) {
                    continue;
                }
                final int historicMovesLastIndex = historicMoves.length - 1;
                final CurrentAndParentConfigurations currentAndParentConfigurations = getCurrentAndParentConfigurations(
                    historicMovesLastIndex, initialHistoricMovesLength, historicMoves, originalConfiguration,
                    parentConfiguration);
                currentConfiguration = currentAndParentConfigurations.currentConfiguration;
                parentConfiguration = currentAndParentConfigurations.parentConfiguration;

                final List<PieceConfiguration> onwardConfigurations = currentConfiguration.getOnwardConfigurations();
                final Double gameEndValue = getEndgameValue(onwardConfigurations.size(), currentConfiguration);
                if (gameEndValue != null) {
                    inMemoryTrie.setScore(currentConfiguration.getHistoricMoves(), gameEndValue);
                    continue;
                }
                final double currentLesserScore = currentConfiguration.getLesserScore();
                storeConfigurationScores(onwardConfigurations, inMemoryTrie, isMaximumDepth, currentLesserScore);
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

    private static CurrentAndParentConfigurations getCurrentAndParentConfigurations(
        int historicMovesLastIndex,
        int initialHistoricMovesLength,
        short[] historicMoves,
        PieceConfiguration originalConfiguration,
        PieceConfiguration parentConfiguration
    ) {
        PieceConfiguration currentConfiguration;
        if (historicMovesLastIndex >= initialHistoricMovesLength) {
            final short[] additionalMovesExceptFinal = copyOfRange(
                historicMoves, initialHistoricMovesLength, historicMovesLastIndex);
            if (parentConfiguration == null
                || !Arrays.equals(parentConfiguration.getHistoricMoves(), additionalMovesExceptFinal)
            ) {
                parentConfiguration = toNewConfigurationFromMoves(originalConfiguration, additionalMovesExceptFinal);
            }
            currentConfiguration = toNewConfigurationFromMove(
                parentConfiguration, historicMoves[historicMovesLastIndex]);
        } else {
            currentConfiguration = originalConfiguration;
        }
        return new CurrentAndParentConfigurations(currentConfiguration, parentConfiguration);
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
            double currentLesserScore) {
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

    private static double getConfigurationScore(PieceConfiguration onwardConfiguration, double currentLesserScore, boolean isMaximumDepth) {
        // Set all the bit flags in the onward configuration
        final int onwardValueComparison = onwardConfiguration.adjustForDraw(onwardConfiguration.getValueDifferential(), !isMaximumDepth);
        return onwardValueComparison + currentLesserScore;
    }

    private static MoveScorePair getBestOnwardMoveScorePair(InMemoryTrie inMemoryTrie, short[] startingNode) {
        final TreeMap<short[], Double> childMap = inMemoryTrie.getChildren(startingNode);
        return getBestMoveScorePair(inMemoryTrie, childMap);
    }

    private static MoveScorePair getBestMoveScorePair(InMemoryTrie inMemoryTrie, TreeMap<short[], Double> siblingMap) {
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
        final TreeMap<short[], Double> children = inMemoryTrie.getChildren(historicMoves);
        if (children.isEmpty()) {
            return value;
        }
        final MoveScorePair bestChildMove = getBestMoveScorePair(inMemoryTrie, children);
        if (bestChildMove.move() != -1) {
            return -(bestChildMove.score() * 0.99) - value;
        }
        return value;
    }

    private record CurrentAndParentConfigurations(
        PieceConfiguration currentConfiguration, PieceConfiguration parentConfiguration){}
}
