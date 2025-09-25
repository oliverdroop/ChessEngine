package chess.api.storage.ephemeral;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class InMemoryTrie {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryTrie.class);

    private static final Comparator<short[]> SHORT_ARRAY_COMPARATOR = new LengthFirstShortArrayComparator();

    private final SortedMap<short[], Double> trieMap = new TreeMap<>(SHORT_ARRAY_COMPARATOR);

    public InMemoryTrie() {}

    public SortedMap<short[], Double> getTrieMap() {
        return trieMap;
    }

    public Optional<Double> getScoreDifferential(short[] movesSoFar) {
        if (movesSoFar == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(trieMap.get(movesSoFar));
    }

    public void setScoreDifferential(short[] movesSoFar, double scoreDifferential) {
        if (movesSoFar == null) {
            return;
        }
        trieMap.put(movesSoFar, scoreDifferential);
    }
}
