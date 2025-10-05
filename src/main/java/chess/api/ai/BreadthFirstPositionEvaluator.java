package chess.api.ai;

import chess.api.PieceConfiguration;
import chess.api.storage.ephemeral.InMemoryTrie;

import java.util.*;

import static chess.api.PieceConfiguration.*;
import static java.util.Arrays.copyOfRange;

public class BreadthFirstPositionEvaluator {

    public static PieceConfiguration getBestMoveRecursively(PieceConfiguration pieceConfiguration, int depth) {
        final InMemoryTrie inMemoryTrie = new InMemoryTrie();
        final short[] initialHistoricMoves = new short[]{};
        pieceConfiguration.setHistoricMoves(initialHistoricMoves);
        inMemoryTrie.setScore(initialHistoricMoves, 0.0);
        PieceConfiguration currentConfiguration;
        PieceConfiguration parentConfiguration = null;
        int currentDepth = 0;

        while(currentDepth < depth) {
            final Map<short[], Double> trieMapCopy = new TreeMap<>(inMemoryTrie.getTrieMap());
            for(short[] historicMoves : trieMapCopy.keySet()) {
                if (historicMoves.length != currentDepth) {
                    continue;
                }
                final int historicMovesLastIndex = historicMoves.length - 1;
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
                    inMemoryTrie.setScore(currentConfiguration.getHistoricMoves(), gameEndValue);
                    continue;
                }
                onwardConfigurations.forEach(
                    onwardConfiguration -> storeConfigurationScore(onwardConfiguration, inMemoryTrie));
            }
            currentDepth++;
        }

        final short bestMove = getBestOnwardMoveScorePair(inMemoryTrie, initialHistoricMoves).move();
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

    private static void storeConfigurationScore(PieceConfiguration onwardConfiguration, InMemoryTrie inMemoryTrie) {
        // Set all the bit flags in the onward configuration
        onwardConfiguration.setHigherBitFlags();
        final int valueComparison = onwardConfiguration.getValueDifferential();
        final double onwardThreatValue = onwardConfiguration.getLesserScore();
        final double onwardFullScore = valueComparison + onwardThreatValue;
        inMemoryTrie.setScore(onwardConfiguration.getHistoricMoves(), onwardFullScore);
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
}
