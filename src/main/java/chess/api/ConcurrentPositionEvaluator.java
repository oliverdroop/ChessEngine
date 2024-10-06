package chess.api;

import chess.api.pieces.Piece;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static chess.api.PositionEvaluator.getBestScoreDifferentialRecursively;

public class ConcurrentPositionEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConcurrentPositionEvaluator.class);

    private static final int CONCURRENCY_DEPTH_THRESHOLD = 5;

    private static final ExecutorService executorService = Executors.newFixedThreadPool(16);

    @VisibleForTesting
    static int getValueDifferential(PieceConfiguration pieceConfiguration) {
        int valueDifferential = 0;
        final int turnSide = pieceConfiguration.getTurnSide();
        for (int positionBitFlag : pieceConfiguration.getPositionBitFlags()) {
            // Is it a piece?
            final int pieceBitFlag = positionBitFlag & PieceConfiguration.ALL_PIECE_FLAGS_COMBINED;
            if (pieceBitFlag == 0) {
                continue;
            }
            final int value = Piece.getValue(pieceBitFlag);
            // Is it a black piece?
            final int isBlackOccupied = (positionBitFlag & PieceConfiguration.BLACK_OCCUPIED) >> 9;
            // Is it a player or opposing piece?
            final int turnSideFactor = 1 - ((turnSide ^ isBlackOccupied) << 1);
            valueDifferential += value * turnSideFactor;
        }
        return valueDifferential;
    }

    public static PieceConfiguration getBestMoveRecursively(PieceConfiguration pieceConfiguration, int depth) {
        final Optional<ConfigurationScorePair> optionalBestEntry;
        if (depth >= CONCURRENCY_DEPTH_THRESHOLD) {
            // Use a multithreading method
            optionalBestEntry = getBestConfigurationScorePairConcurrently(pieceConfiguration, depth);
        } else {
            // Use a single-threaded method
            optionalBestEntry = PositionEvaluator.getBestConfigurationScorePairRecursively(pieceConfiguration, depth);
        }
        return optionalBestEntry.map(ConfigurationScorePair::pieceConfiguration).orElse(null);
    }

    private static Optional<ConfigurationScorePair> getBestConfigurationScorePairConcurrently(PieceConfiguration pieceConfiguration, int depth) {
        final double currentDiff = getValueDifferential(pieceConfiguration);

        depth--;
        final List<PieceConfiguration> onwardPieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();
        final int onwardConfigurationCount = onwardPieceConfigurations.size();
        final CompletableFuture<Double>[] onwardConfigurationScoreFutures = new CompletableFuture[onwardConfigurationCount];
        final boolean[] fiftyMoveRuleChecks = new boolean[onwardConfigurationCount];

        for (int i = 0; i < onwardConfigurationCount; i++) {
            PieceConfiguration onwardPieceConfiguration = onwardPieceConfigurations.get(i);

            fiftyMoveRuleChecks[i] = PositionEvaluator.isFiftyMoveRuleFailure(onwardPieceConfiguration);

            CompletableFuture<Double> comparisonFuture = CompletableFuture.supplyAsync(
                    getCallableComparison(onwardPieceConfiguration, currentDiff, depth), executorService);
            onwardConfigurationScoreFutures[i] = comparisonFuture;
        }

        final double threatValue = pieceConfiguration.getLesserScore();
        int bestOnwardConfigurationIndex = -1;
        double bestOnwardConfigurationScore = -Double.MAX_VALUE;
        for(int i = 0; i < onwardConfigurationCount; i++) {
            double onwardConfigurationScore = onwardConfigurationScoreFutures[i].join() + threatValue;
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

    private static Supplier<Double> getCallableComparison(
            PieceConfiguration onwardPieceConfiguration, double currentDiff, int depth) {
        return () -> {
            final double nextDiff = getValueDifferential(onwardPieceConfiguration);
            final double comparison = currentDiff - nextDiff;
            final double recursiveDiff = getBestScoreDifferentialRecursively(onwardPieceConfiguration, depth) * 0.99;
            return comparison + recursiveDiff;
        };
    }
}
