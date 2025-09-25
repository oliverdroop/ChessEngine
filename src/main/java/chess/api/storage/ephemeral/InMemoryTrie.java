package chess.api.storage.ephemeral;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class InMemoryTrie {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryTrie.class);

    private static final Comparator<short[]> SHORT_ARRAY_COMPARATOR = new LengthFirstShortArrayComparator();

    private final SortedMap<short[], double[]> trieMap = new TreeMap<>(SHORT_ARRAY_COMPARATOR);

    public InMemoryTrie() {}

    public SortedMap<short[], double[]> getTrieMap() {
        return trieMap;
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
}
