package chess.api.ai;

import chess.api.GameEndType;
import chess.api.PieceConfiguration;
import chess.api.storage.ephemeral.InMemoryTrie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static chess.api.PieceConfiguration.NO_CAPTURE_OR_PAWN_MOVE_LIMIT;

public class DepthFirstPositionEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DepthFirstPositionEvaluator.class);

    static final InMemoryTrie IN_MEMORY_TRIE = new InMemoryTrie();

    public static PieceConfiguration getBestMoveRecursively(PieceConfiguration pieceConfiguration, int depth) {
        IN_MEMORY_TRIE.prune(pieceConfiguration.getHistoricMoves());
        final Optional<ConfigurationScorePair> optionalBestEntry = getBestConfigurationScorePairRecursively(pieceConfiguration, depth);
        return optionalBestEntry.map(ConfigurationScorePair::pieceConfiguration).orElse(null);
    }

    static double getBestScoreDifferentialRecursively(PieceConfiguration pieceConfiguration, int depth) {
        // The entry object below consists of a PieceConfiguration and a Double representing the score
        final Optional<ConfigurationScorePair> optionalBestEntry = getBestConfigurationScorePairRecursively(pieceConfiguration, depth);
        final double scoreDifferential;
        final int turnSide = pieceConfiguration.getTurnSide();
        final short[] currentConfigurationHistoricMoves = pieceConfiguration.getHistoricMoves();
        if (optionalBestEntry.isPresent()) {
            scoreDifferential = optionalBestEntry.get().score();
            final short[] onwardConfigurationHistoricMoves = optionalBestEntry.get().pieceConfiguration().getHistoricMoves();
            IN_MEMORY_TRIE.setScoreDifferential(onwardConfigurationHistoricMoves, turnSide, scoreDifferential);
        } else if (pieceConfiguration.isCheck()) {
            // Checkmate
            scoreDifferential = Float.MAX_VALUE;
            IN_MEMORY_TRIE.setScoreDifferential(currentConfigurationHistoricMoves, turnSide, scoreDifferential);
        } else {
            // Stalemate
            scoreDifferential = -Float.MAX_VALUE;
            IN_MEMORY_TRIE.setScoreDifferential(currentConfigurationHistoricMoves, turnSide, scoreDifferential);
        }
        return scoreDifferential;
    }

    static boolean isFiftyMoveRuleFailure(PieceConfiguration pieceConfiguration) {
        return pieceConfiguration.getHalfMoveClock() > NO_CAPTURE_OR_PAWN_MOVE_LIMIT;
    }

    static Optional<ConfigurationScorePair> getBestConfigurationScorePairRecursively(PieceConfiguration pieceConfiguration, int depth) {
        final double currentDiff = pieceConfiguration.getValueDifferential();

        depth--;
        final List<PieceConfiguration> onwardPieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();
        final int onwardConfigurationCount = onwardPieceConfigurations.size();
        final double[] onwardConfigurationScores = new double[onwardConfigurationCount];
        final boolean[] fiftyMoveRuleChecks = new boolean[onwardConfigurationCount];
        for (int i = 0; i < onwardConfigurationCount; i++) {
            PieceConfiguration onwardPieceConfiguration = onwardPieceConfigurations.get(i);

            fiftyMoveRuleChecks[i] = isFiftyMoveRuleFailure(onwardPieceConfiguration);

            final int onwardConfigurationTurnSide = onwardPieceConfiguration.getTurnSide();
            final short[] onwardConfigurationHistoricMoves = onwardPieceConfiguration.getHistoricMoves();
            final Optional<double[]> scoreDifferentialsByTurnSide = IN_MEMORY_TRIE.getScoreDifferential(onwardConfigurationHistoricMoves);
            double comparison;
            if (scoreDifferentialsByTurnSide.isEmpty() || scoreDifferentialsByTurnSide.get()[onwardConfigurationTurnSide] == 0.0) {
                double nextDiff = onwardPieceConfiguration.getValueDifferential();
                comparison = currentDiff - nextDiff;
                if (depth > 0) {
                    comparison += getBestScoreDifferentialRecursively(onwardPieceConfiguration, depth) * 0.99; // This modifier adjusts for uncertainty at depth
                    // Below is where the position can be evaluated for more than just the value differential (because the position bit flags have been calculated)
                }
            } else {
                comparison = scoreDifferentialsByTurnSide.get()[onwardConfigurationTurnSide];
            }
            onwardConfigurationScores[i] = comparison;
        }

        final double threatValue = pieceConfiguration.getLesserScore();
        int bestOnwardConfigurationIndex = -1;
        double bestOnwardConfigurationScore = -Double.MAX_VALUE;
        for(int i = 0; i < onwardConfigurationCount; i++) {
            double onwardConfigurationScore = onwardConfigurationScores[i] + threatValue;
            if (onwardConfigurationScore > bestOnwardConfigurationScore && !fiftyMoveRuleChecks[i]) {
                bestOnwardConfigurationScore = onwardConfigurationScore;
                bestOnwardConfigurationIndex = i;
            }
        }

        if (bestOnwardConfigurationIndex >= 0) {
            final PieceConfiguration bestOnwardConfiguration = onwardPieceConfigurations.get(bestOnwardConfigurationIndex);
            return Optional.of(new ConfigurationScorePair(bestOnwardConfiguration, -bestOnwardConfigurationScore));
        }
        return Optional.empty();
    }

    public static GameEndType deriveGameEndType(PieceConfiguration finalConfiguration) {
        if (finalConfiguration.isCheck() || finalConfiguration.getHalfMoveClock() == NO_CAPTURE_OR_PAWN_MOVE_LIMIT) {
            return GameEndType.values()[1 - finalConfiguration.getTurnSide()];
        } else {
            return GameEndType.STALEMATE;
        }
    }
}
