package chess.api.ai;

import chess.api.configuration.PieceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static chess.api.ai.DepthFirstPositionEvaluator.getBestScoreDifferentialRecursively;

public class ConcurrentPositionEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConcurrentPositionEvaluator.class);

    private static final int CONCURRENCY_DEPTH_THRESHOLD = 5;

    private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();

    private static final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    public static PieceConfiguration getBestMoveRecursively(PieceConfiguration pieceConfiguration, int depth) {
        final Optional<ConfigurationScorePair> optionalBestEntry;
        LOGGER.debug("Thread pool size is {}", THREAD_POOL_SIZE);
        if (depth >= CONCURRENCY_DEPTH_THRESHOLD && THREAD_POOL_SIZE > 1) {
            // Use a multithreading method
            optionalBestEntry = getBestConfigurationScorePairConcurrently(pieceConfiguration, depth);
        } else {
            // Use a single-threaded method
            optionalBestEntry = DepthFirstPositionEvaluator.getBestConfigurationScorePairRecursively(pieceConfiguration, depth);
        }
        return optionalBestEntry.map(ConfigurationScorePair::pieceConfiguration).orElse(null);
    }

    private static Optional<ConfigurationScorePair> getBestConfigurationScorePairConcurrently(PieceConfiguration pieceConfiguration, int depth) {
        final double currentDiff = pieceConfiguration.getValueDifferential();

        depth--;
        final List<PieceConfiguration> onwardPieceConfigurations = pieceConfiguration.getOnwardConfigurations();
        final int onwardConfigurationCount = onwardPieceConfigurations.size();
        final CompletableFuture<Double>[] onwardConfigurationScoreFutures = new CompletableFuture[onwardConfigurationCount];
        final boolean[] fiftyMoveRuleChecks = new boolean[onwardConfigurationCount];
        final boolean[] threefoldRepetitionChecks = new boolean[onwardConfigurationCount];

        for (int i = 0; i < onwardConfigurationCount; i++) {
            PieceConfiguration onwardPieceConfiguration = onwardPieceConfigurations.get(i);

            fiftyMoveRuleChecks[i] = DepthFirstPositionEvaluator.isFiftyMoveRuleFailure(onwardPieceConfiguration);
            threefoldRepetitionChecks[i] = onwardPieceConfiguration.isThreefoldRepetitionFailure();

            CompletableFuture<Double> comparisonFuture = CompletableFuture.supplyAsync(
                getCallableComparison(onwardPieceConfiguration, currentDiff, depth), executorService);
            onwardConfigurationScoreFutures[i] = comparisonFuture;
        }

        final double threatValue = pieceConfiguration.getLesserScore();
        int bestOnwardConfigurationIndex = -1;
        double bestOnwardConfigurationScore = -Double.MAX_VALUE;
        for(int i = 0; i < onwardConfigurationCount; i++) {
            double onwardConfigurationScore = onwardConfigurationScoreFutures[i].join() + threatValue;
            if (onwardConfigurationScore > bestOnwardConfigurationScore
                    && !fiftyMoveRuleChecks[i]
                    && !threefoldRepetitionChecks[i]) {
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
            final double nextDiff = onwardPieceConfiguration.getValueDifferential();
            final double comparison = currentDiff - nextDiff;
            final double recursiveDiff = getBestScoreDifferentialRecursively(onwardPieceConfiguration, depth) * 0.99;
            return comparison + recursiveDiff;
        };
    }
}
