package chess.api.ai;

import chess.api.PieceConfiguration;
import chess.api.storage.ephemeral.InMemoryTrie;

import java.util.*;

import static chess.api.PieceConfiguration.NO_CAPTURE_OR_PAWN_MOVE_LIMIT;

public class BreadthFirstPositionEvaluator {

    private static final double UNCERTAINTY_FACTOR = 0.99;

    private static final double[] UNCERTAINTY_ADJUSTMENTS = new double[] {
        Math.pow(UNCERTAINTY_FACTOR, 1),
        Math.pow(UNCERTAINTY_FACTOR, 2),
        Math.pow(UNCERTAINTY_FACTOR, 3),
        Math.pow(UNCERTAINTY_FACTOR, 4),
        Math.pow(UNCERTAINTY_FACTOR, 5),
        Math.pow(UNCERTAINTY_FACTOR, 6),
    };

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
                    final short[] historicMovesExceptFinal = Arrays.copyOfRange(historicMoves, 0, historicMovesLastIndex);
                    if (parentConfiguration == null || !Arrays.equals(parentConfiguration.getHistoricMoves(), historicMovesExceptFinal)) {
                        parentConfiguration = PieceConfiguration.toNewConfigurationFromMoves(pieceConfiguration, historicMovesExceptFinal);
                    }
                    currentConfiguration = PieceConfiguration.toNewConfigurationFromMove(parentConfiguration, historicMoves[historicMovesLastIndex]);
                } else {
                    currentConfiguration = pieceConfiguration;
                }

                final List<PieceConfiguration> onwardConfigurations = currentConfiguration.getPossiblePieceConfigurations();
                final Double gameEndValue = getEndgameValue(onwardConfigurations.size(), currentConfiguration);
                if (gameEndValue != null) {
                    inMemoryTrie.setScore(currentConfiguration.getHistoricMoves(), gameEndValue);
                    continue;
                }
                final ConfigurationScorePair[] onwardConfigurationScores = getOnwardConfigurationScores(onwardConfigurations);

                Arrays.stream(onwardConfigurationScores).forEach(
                    configurationScorePair -> inMemoryTrie.setScore(
                        configurationScorePair.pieceConfiguration().getHistoricMoves(), configurationScorePair.score()
                    )
                );
            }
            currentDepth++;
        }

        final short bestMove = getBestOnwardMoveScorePair(inMemoryTrie, new short[]{}).move();
        if (bestMove != -1) {
            final PieceConfiguration bestConfiguration = PieceConfiguration.toNewConfigurationFromMove(pieceConfiguration, bestMove);
            if (bestConfiguration.getHalfMoveClock() <= NO_CAPTURE_OR_PAWN_MOVE_LIMIT) {
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

    private static ConfigurationScorePair[] getOnwardConfigurationScores(
        List<PieceConfiguration> onwardConfigurations) {
        final ConfigurationScorePair[] onwardConfigurationScorePairs = new ConfigurationScorePair[onwardConfigurations.size()];
        for (int i = 0; i < onwardConfigurationScorePairs.length; i++) {
            final PieceConfiguration onwardConfiguration = onwardConfigurations.get(i);
            // Set all the bit flags in the onward configuration
            onwardConfiguration.setHigherBitFlags();
            final int valueComparison = onwardConfiguration.getValueDifferential();
            final double onwardThreatValue = onwardConfiguration.getLesserScore();
            final double onwardFullScore = valueComparison + onwardThreatValue;
            onwardConfigurationScorePairs[i] = new ConfigurationScorePair(onwardConfiguration, onwardFullScore);
        }
        return onwardConfigurationScorePairs;
    }

    private static MoveScorePair getBestOnwardMoveScorePair(InMemoryTrie inMemoryTrie, short[] startingNode) {
        short bestMove = -1;
        double bestScore = -Double.MAX_VALUE;
        final Map<short[], Double> childEntries = inMemoryTrie.getChildren(startingNode);
        for(short[] historicMoves : childEntries.keySet()) {
            final double value = -getCumulativeValue(historicMoves, inMemoryTrie);
            if (value > bestScore) {
                bestMove = historicMoves[historicMoves.length - 1];
                bestScore = value;
            }
        }
        return new MoveScorePair(bestMove, bestScore);
    }

    private static double getCumulativeValue(short[] historicMoves, InMemoryTrie inMemoryTrie) {
        final double value = inMemoryTrie.getScore(historicMoves);
        if (!inMemoryTrie.getChildren(historicMoves).isEmpty()) {
            final MoveScorePair bestChildMove = getBestOnwardMoveScorePair(inMemoryTrie, historicMoves);
            if (bestChildMove.move() != -1) {
                return (bestChildMove.score() * 0.99) + value;
            }
        }
        return -value;
    }
}
