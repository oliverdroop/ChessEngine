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
        depth -= 1;
        final InMemoryTrie inMemoryTrie = new InMemoryTrie();
        final short[] initialHistoricMoves = new short[]{};
        pieceConfiguration.setHistoricMoves(initialHistoricMoves);
        inMemoryTrie.setScoreDifferential(initialHistoricMoves, 0.0);
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
                    inMemoryTrie.setScoreDifferential(currentConfiguration.getHistoricMoves(), gameEndValue);
                    continue;
                }
                final int currentValueDifferential = currentConfiguration.getValueDifferential();
//                final double currentThreatValue = currentConfiguration.getLesserScore() * (1 - ((currentDepth % 2) * 2));
                final double currentThreatValue = currentConfiguration.getLesserScore();
                final ConfigurationScorePair[] onwardConfigurationScores = getOnwardConfigurationScores(currentValueDifferential, currentThreatValue, onwardConfigurations);
                Arrays.sort(onwardConfigurationScores);

                Arrays.stream(onwardConfigurationScores).forEach(
                    configurationScorePair -> inMemoryTrie.setScoreDifferential(
                        configurationScorePair.pieceConfiguration().getHistoricMoves(), configurationScorePair.score()
                    )
                );
            }
            currentDepth++;
        }

        final short bestMove = getBestStoredMove(inMemoryTrie);
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

    private static short getBestStoredMove(InMemoryTrie inMemoryTrie) {
        short bestMove = -1;
        double bestScore = -Double.MAX_VALUE;
        for(short[] historicMoves : inMemoryTrie.getTrieMap().keySet()) {
            if (historicMoves.length == 0) {
                continue;
            }

            double value = 0;
            for(int i = 1; i <= historicMoves.length; i++) {
                final short[] historicMovesSubArray = Arrays.copyOfRange(historicMoves, 0, i);
                final int ancestralSign = -1 + ((i % 2) * 2);
                final double ancestralValue = inMemoryTrie.getTrieMap().get(historicMovesSubArray) * ancestralSign;
                value += ancestralValue * UNCERTAINTY_ADJUSTMENTS[i];
            }

            if (value > bestScore) {
                bestMove = historicMoves[0];
                bestScore = value;
            }
        }
        return bestMove;
    }

    private static ConfigurationScorePair[] getOnwardConfigurationScores(
            int currentValueDifferential, double currentThreatValue, List<PieceConfiguration> onwardConfigurations) {
        final ConfigurationScorePair[] onwardConfigurationScorePairs = new ConfigurationScorePair[onwardConfigurations.size()];
        for (int i = 0; i < onwardConfigurationScorePairs.length; i++) {
            final PieceConfiguration onwardConfiguration = onwardConfigurations.get(i);
            // Set all the bit flags in the onward configuration
            onwardConfiguration.setHigherBitFlags();
            final double valueComparison = currentValueDifferential - onwardConfiguration.getValueDifferential();
//            final double onwardFullScore = valueComparison + currentThreatValue;
            final double onwardFullScore = valueComparison;
            onwardConfigurationScorePairs[i] = new ConfigurationScorePair(onwardConfiguration, onwardFullScore);
        }
        return onwardConfigurationScorePairs;
    }
}
