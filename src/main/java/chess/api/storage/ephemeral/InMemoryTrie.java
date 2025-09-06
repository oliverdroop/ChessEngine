package chess.api.storage.ephemeral;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class InMemoryTrie {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryTrie.class);

    private static final ShortArrayComparator SHORT_ARRAY_COMPARATOR = new ShortArrayComparator();

    private static final int MAXIMUM_SEARCH_DEPTH = 5;

    private final Map<short[], double[]> trieMap = new TreeMap<>(SHORT_ARRAY_COMPARATOR);

    public InMemoryTrie() {
        trieMap.put(new short[]{}, null);
    }

    public Optional<double[]> getScoreDifferential(short[] movesSoFar) {
        if (movesSoFar == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(trieMap.get(movesSoFar));
    }

    public void setScoreDifferential(short[] movesSoFar, int turnSide, double scoreDifferential) {
        if (movesSoFar == null) {
            return;
        }
        final Optional<double[]> optionalScoreDifferentialsByDepth = getScoreDifferential(movesSoFar);
        final double[] scoreDifferentialsByTurnSide = optionalScoreDifferentialsByDepth
            .orElseGet(() -> new double[2]);
        scoreDifferentialsByTurnSide[turnSide] = scoreDifferential;
        trieMap.put(movesSoFar, scoreDifferentialsByTurnSide);
    }

    public synchronized void prune(short[] branchToPreserve) {
        if (branchToPreserve == null) {
            return;
        }
        long t1 = System.currentTimeMillis();
        trieMap.keySet().removeIf(
            moveHistory -> {
                if (moveHistory.length > 5 && moveHistory.length < branchToPreserve.length) {
                    return true;
                }
                int comparison = SHORT_ARRAY_COMPARATOR.compare(moveHistory, branchToPreserve);
                return Math.abs(comparison) == 2;
            });
        long t2 = System.currentTimeMillis();
        LOGGER.debug("Pruning trie took {} ms", t2 - t1);
    }
}
