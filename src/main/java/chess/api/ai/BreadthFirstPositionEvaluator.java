package chess.api.ai;

import chess.api.PieceConfiguration;
import chess.api.storage.ephemeral.InMemoryTrie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.*;

import static chess.api.PieceConfiguration.*;
import static chess.api.ai.TimingUtil.logTime;
import static chess.api.storage.ephemeral.MoveHistoryConverter.*;
import static java.util.Arrays.copyOfRange;

public class BreadthFirstPositionEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(BreadthFirstPositionEvaluator.class);

    public static PieceConfiguration getBestMoveRecursively(PieceConfiguration pieceConfiguration, int depth) {
        final InMemoryTrie inMemoryTrie = new InMemoryTrie();
        final short[] initialHistoricMoves = new short[]{};
        pieceConfiguration.setHistoricMoves(initialHistoricMoves);
        inMemoryTrie.setScore(fromMoves(initialHistoricMoves), 0.0);
        PieceConfiguration currentConfiguration;
        PieceConfiguration parentConfiguration = null;
        int currentDepth = 0;

        final long t1 = System.currentTimeMillis();
        while(currentDepth < depth) {
            final boolean isMaximumDepth = currentDepth >= depth - 1;
            final Map<BigInteger, Double> trieMapCopy = new TreeMap<>(inMemoryTrie.getTrieMap());
            for(BigInteger historicMovesKey : trieMapCopy.keySet()) {
                final short[] historicMoves = toMoves(historicMovesKey);
                final int trailingEmptyShorts = inMemoryTrie.countTrailingEmptyShorts(historicMovesKey);
                if (historicMoves.length < currentDepth || trailingEmptyShorts > 1) {
                    continue;
                }
                final int historicMovesLastIndex = historicMoves.length - 1 - trailingEmptyShorts;
                if (historicMovesLastIndex >= 0) {
                    final short[] historicMovesExceptFinal = copyOfRange(historicMoves, 0, historicMovesLastIndex);
                    if (parentConfiguration == null || !Arrays.equals(parentConfiguration.getHistoricMoves(), historicMovesExceptFinal)) {
                        parentConfiguration = toNewConfigurationFromMoves(pieceConfiguration, historicMovesExceptFinal);
                    }
                    currentConfiguration = toNewConfigurationFromMove(parentConfiguration, historicMoves[historicMovesLastIndex]);
                } else {
                    currentConfiguration = pieceConfiguration;
                }

                final List<PieceConfiguration> onwardConfigurations = currentConfiguration.getOnwardConfigurations();
                final Double gameEndValue = getEndgameValue(onwardConfigurations.size(), currentConfiguration);
                if (gameEndValue != null) {
                    inMemoryTrie.setScore(fromMoves(currentConfiguration.getHistoricMoves()).shiftLeft(16), gameEndValue);
                    continue;
                }
                storeConfigurationScores(onwardConfigurations, inMemoryTrie, isMaximumDepth);
            }
            currentDepth++;
            if (currentDepth < depth) {
                inMemoryTrie.shiftKeysLeft();
            }
        }
        LOGGER.info("Populating the InMemoryTrie took {} milliseconds", System.currentTimeMillis() - t1);

        final short bestMove = logTime("Calculating the best move", () -> getBestOnwardMoveScorePair(inMemoryTrie).move());
        if (bestMove != -1) {
            final PieceConfiguration bestConfiguration = toNewConfigurationFromMove(pieceConfiguration, bestMove);
            if (bestConfiguration.getHalfMoveClock() <= NO_CAPTURE_OR_PAWN_MOVE_LIMIT) {
                bestConfiguration.setHigherBitFlags();
                return bestConfiguration;
            }
        }
        return null;
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
        } else if (currentConfiguration.getHalfMoveClock() > NO_CAPTURE_OR_PAWN_MOVE_LIMIT) {
            return (double) -Float.MAX_VALUE;
        }
        return null;
    }

    private static void storeConfigurationScores(List<PieceConfiguration> onwardConfigurations, InMemoryTrie inMemoryTrie, boolean isMaximumDepth) {
        PieceConfiguration bestOnwardConfiguration = null;
        double bestOnwardScore = -Double.MAX_VALUE;
        for(PieceConfiguration onwardConfiguration : onwardConfigurations) {
            final double onwardScore = getConfigurationScore(onwardConfiguration);
            if (!isMaximumDepth) {
                // Store all the onward scores because we are not yet at the maximum depth
                final BigInteger key = fromMoves(onwardConfiguration.getHistoricMoves());
                inMemoryTrie.setScore(key, onwardScore);
            } else if (onwardScore > bestOnwardScore) {
                // Calculate which score to store because we are at the maximum depth
                bestOnwardScore = onwardScore;
                bestOnwardConfiguration = onwardConfiguration;
            }
        }
        if (isMaximumDepth) {
            // Only store the best score because we are at the maximum depth
            final BigInteger key = fromMoves(bestOnwardConfiguration.getHistoricMoves());
            inMemoryTrie.setScore(key, bestOnwardScore);
        }
    }

    private static double getConfigurationScore(PieceConfiguration onwardConfiguration) {
        // Set all the bit flags in the onward configuration
        onwardConfiguration.setHigherBitFlags();
        final int valueComparison = onwardConfiguration.getValueDifferential();
        final double onwardThreatValue = onwardConfiguration.getLesserScore();
        return valueComparison + onwardThreatValue;
    }

    private static MoveScorePair getBestOnwardMoveScorePair(InMemoryTrie inMemoryTrie) {
        final TreeMap<BigInteger, Double> childMap = inMemoryTrie.getChildren(BigInteger.ZERO);
        return getBestMoveScorePair(inMemoryTrie, childMap);
    }

    private static MoveScorePair getBestMoveScorePair(InMemoryTrie inMemoryTrie, TreeMap<BigInteger, Double> siblingMap) {
        short bestMove = -1;
        double bestScore = -Double.MAX_VALUE;
        for(BigInteger historicMovesKey : siblingMap.keySet()) {
            final double value = getCumulativeValue(historicMovesKey, inMemoryTrie);
            if (value > bestScore) {
                final int trailingEmptyShorts = inMemoryTrie.countTrailingEmptyShorts(historicMovesKey);
                bestMove = historicMovesKey.shiftRight(16 * trailingEmptyShorts).shortValue();
                bestScore = value;
            }
        }
        return new MoveScorePair(bestMove, bestScore);
    }

    private static double getCumulativeValue(BigInteger historicMoves, InMemoryTrie inMemoryTrie) {
        final double value = inMemoryTrie.getScore(historicMoves);
        final TreeMap<BigInteger, Double> children = inMemoryTrie.getChildren(historicMoves);
        if (children.isEmpty()) {
            return value;
        }
        final MoveScorePair bestChildMove = getBestMoveScorePair(inMemoryTrie, children);
        if (bestChildMove.move() != -1) {
            return -(bestChildMove.score() * 0.99) - value;
        }
        return value;
    }
}
