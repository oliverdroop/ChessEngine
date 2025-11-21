package com.oliverdroop.chess.api.ai;

import com.oliverdroop.chess.api.GameEndType;
import com.oliverdroop.chess.api.configuration.PieceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class DepthFirstPositionEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DepthFirstPositionEvaluator.class);

    public static PieceConfiguration getBestMoveRecursively(PieceConfiguration pieceConfiguration, int depth) {
        final Optional<ConfigurationScorePair> optionalBestEntry = getBestConfigurationScorePairRecursively(pieceConfiguration, depth);
        return optionalBestEntry.map(ConfigurationScorePair::pieceConfiguration).orElse(null);
    }

    static double getBestScoreDifferentialRecursively(PieceConfiguration pieceConfiguration, int depth) {
        // The entry object below consists of a PieceConfiguration and a Double representing the score
        final Optional<ConfigurationScorePair> optionalBestEntry = getBestConfigurationScorePairRecursively(pieceConfiguration, depth);
        if (optionalBestEntry.isPresent()) {
            return optionalBestEntry.get().score();
        } else if (pieceConfiguration.isCheck()) {
            // Checkmate
            return Float.MAX_VALUE;
        }
        // Stalemate
        return -Float.MAX_VALUE;
    }

    static Optional<ConfigurationScorePair> getBestConfigurationScorePairRecursively(PieceConfiguration pieceConfiguration, int depth) {
        final int currentDiff = pieceConfiguration.adjustForDraw(pieceConfiguration.getValueDifferential());

        depth--;
        final List<PieceConfiguration> onwardPieceConfigurations = pieceConfiguration.getOnwardConfigurations();
        final int onwardConfigurationCount = onwardPieceConfigurations.size();
        final double[] onwardConfigurationScores = new double[onwardConfigurationCount];
        for (int i = 0; i < onwardConfigurationCount; i++) {
            PieceConfiguration onwardConfiguration = onwardPieceConfigurations.get(i);

            int nextDiff = onwardConfiguration.adjustForDraw(onwardConfiguration.getValueDifferential());
            double comparison = currentDiff - nextDiff;
            if (depth > 0) {
                comparison += getBestScoreDifferentialRecursively(onwardConfiguration, depth) * 0.99; // This modifier adjusts for uncertainty at depth
                // Below is where the position can be evaluated for more than just the value differential (because the position bit flags have been calculated)
            }
            onwardConfigurationScores[i] = comparison;
        }

        final double threatValue = pieceConfiguration.getLesserScore();
        int bestOnwardConfigurationIndex = -1;
        double bestOnwardConfigurationScore = -Double.MAX_VALUE;
        for(int i = 0; i < onwardConfigurationCount; i++) {
            double onwardConfigurationScore = onwardConfigurationScores[i] + threatValue;
            if (onwardConfigurationScore > bestOnwardConfigurationScore) {
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
        if (finalConfiguration.isCheck()) {
            return GameEndType.values()[1 - finalConfiguration.getTurnSide()];
        } else if (finalConfiguration.isDraw()) {
            return GameEndType.DRAW;
        } else {
            return GameEndType.STALEMATE;
        }
    }
}
