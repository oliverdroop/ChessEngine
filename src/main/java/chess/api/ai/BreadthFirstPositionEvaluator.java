package chess.api.ai;

import chess.api.PieceConfiguration;
import chess.api.storage.ephemeral.InMemoryTrie;

import java.util.*;

import static chess.api.PieceConfiguration.NO_CAPTURE_OR_PAWN_MOVE_LIMIT;

public class BreadthFirstPositionEvaluator {

    public static PieceConfiguration getBestMoveRecursively(PieceConfiguration pieceConfiguration, int depth) {
        depth -= 1;
        final InMemoryTrie inMemoryTrie = new InMemoryTrie();
        final short[] initialHistoricMoves = new short[]{};
        pieceConfiguration.setHistoricMoves(initialHistoricMoves);
        inMemoryTrie.setScoreDifferential(initialHistoricMoves, pieceConfiguration.getTurnSide(),0.0);
        PieceConfiguration currentConfiguration;
        int currentDepth = 0;

        while(currentDepth < depth) {
            final Map<short[], double[]> trieMapCopy = new TreeMap<>(inMemoryTrie.getTrieMap());
            for(short[] historicMoves : trieMapCopy.keySet()) {
                if (historicMoves.length != currentDepth) {
                    continue;
                }
                currentConfiguration = PieceConfiguration.toNewConfigurationFromMoves(pieceConfiguration, historicMoves);

                final List<PieceConfiguration> onwardConfigurations = currentConfiguration.getPossiblePieceConfigurations();
                final int onwardConfigurationCount = onwardConfigurations.size();
                if (onwardConfigurationCount == 0) {
                    final double mateValue;
                    if (currentConfiguration.isCheck()) {
                        mateValue = Float.MAX_VALUE;
                    } else {
                        mateValue = -Float.MAX_VALUE;
                    }
                    inMemoryTrie.setScoreDifferential(currentConfiguration.getHistoricMoves(), currentConfiguration.getTurnSide(), mateValue);
                    continue;
                } else if (currentConfiguration.getHalfMoveClock() > NO_CAPTURE_OR_PAWN_MOVE_LIMIT) {
                    inMemoryTrie.setScoreDifferential(currentConfiguration.getHistoricMoves(), currentConfiguration.getTurnSide(), -Float.MAX_VALUE);
                    continue;
                }
                final int currentValueDifferential = currentConfiguration.getValueDifferential();
                final ConfigurationScorePair[] onwardConfigurationScores = getOnwardConfigurationScores(currentValueDifferential, onwardConfigurations);
                Arrays.sort(onwardConfigurationScores);

                Arrays.stream(onwardConfigurationScores).forEach(configurationScorePair -> {
                    final PieceConfiguration onwardConfiguration = configurationScorePair.pieceConfiguration();
                    inMemoryTrie.setScoreDifferential(
                        configurationScorePair.pieceConfiguration().getHistoricMoves(),
                        onwardConfiguration.getTurnSide(), configurationScorePair.score()
                    );
                });
            }
            currentDepth++;
        }

        final short bestMove = getBestMove(pieceConfiguration, depth, inMemoryTrie);
        if (bestMove != -1) {
            final PieceConfiguration bestConfiguration = PieceConfiguration.toNewConfigurationFromMove(pieceConfiguration, bestMove);
            if (bestConfiguration.getHalfMoveClock() <= NO_CAPTURE_OR_PAWN_MOVE_LIMIT) {
                return bestConfiguration;
            }
        }
        return null;
    }

    private static short getBestMove(PieceConfiguration pieceConfiguration, int depth, InMemoryTrie inMemoryTrie) {
        short bestMove = -1;
        double bestScore = -Double.MAX_VALUE;
        final int turnSide = depth % 2 == 0 ? pieceConfiguration.getTurnSide() : pieceConfiguration.getOpposingSide();
        final int opposingSide = 1 - turnSide;
        for(short[] historicMoves : inMemoryTrie.getTrieMap().keySet()) {
            if (historicMoves.length == 0) {
                continue;
            }

            double value = 0;
            for(int i = 1; i <= historicMoves.length; i++) {
                final short[] historicMovesSubArray = Arrays.copyOfRange(historicMoves, 0, i);
                final boolean sameSide = i % 2 == 0;
                final int ancestralTurnSide = sameSide ? turnSide : opposingSide;
                final int ancestralSign = sameSide ? -1 : 1;
                final double ancestralValue = inMemoryTrie.getTrieMap().get(historicMovesSubArray)[ancestralTurnSide] * ancestralSign;
                value += ancestralValue * Math.pow(0.99, i);
            }

            if (value > bestScore) {
                bestMove = historicMoves[0];
                bestScore = value;
            }
        }
        return bestMove;
    }

    private static ConfigurationScorePair[] getOnwardConfigurationScores(
            int currentValueDifferential, List<PieceConfiguration> onwardConfigurations) {
        final ConfigurationScorePair[] onwardConfigurationScorePairs = new ConfigurationScorePair[onwardConfigurations.size()];
        for (int i = 0; i < onwardConfigurationScorePairs.length; i++) {
            final PieceConfiguration onwardConfiguration = onwardConfigurations.get(i);
            // Set all the bit flags in the onward configuration
            onwardConfiguration.setHigherBitFlags();
            final int onwardValueDifferential = currentValueDifferential - onwardConfiguration.getValueDifferential();
//            final double onwardLesserScore = onwardConfiguration.getLesserScore();
//            final double onwardFullScore = onwardValueDifferential + onwardLesserScore;
            final double onwardFullScore = onwardValueDifferential;
            onwardConfigurationScorePairs[i] = new ConfigurationScorePair(onwardConfiguration, onwardFullScore);
        }
        return onwardConfigurationScorePairs;
    }
}
