package chess.api.storage.ephemeral;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class InMemoryTrie {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryTrie.class);

    private static final Comparator<short[]> SHORT_ARRAY_COMPARATOR = new SawtoothShortArrayComparator();

    private final TreeMap<short[], Double> trieMap = new TreeMap<>(SHORT_ARRAY_COMPARATOR);

    public InMemoryTrie() {}

    public SortedMap<short[], Double> getTrieMap() {
        return trieMap;
    }

    public Double getScore(short[] moveHistory) {
        return trieMap.get(moveHistory);
    }

    public TreeMap<short[], Double> getChildren(short[] moveHistory) {
        final TreeMap<short[], Double> treeMap = new TreeMap<>(SHORT_ARRAY_COMPARATOR);
        treeMap.putAll(
            getDescendants(moveHistory)
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().length == moveHistory.length + 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
        return treeMap;
    }

    public void setScore(short[] moveHistory, double score) {
        if (moveHistory == null) {
            return;
        }
        trieMap.put(moveHistory, score);
    }

    private NavigableMap<short[], Double> getDescendants(short[] moveHistory) {
        final Double value = trieMap.get(moveHistory);
        if (value == null) {
            return null;
        }
        short[] higherKey = trieMap.higherKey(moveHistory);
        while(higherKey != null
            && higherKey.length >= moveHistory.length
            && Arrays.equals(moveHistory, Arrays.copyOfRange(higherKey, 0, moveHistory.length))) {
            higherKey = trieMap.higherKey(higherKey);
        }
        if (higherKey == null) {
            return trieMap.tailMap(moveHistory, false);
        }
        return trieMap.subMap(moveHistory, false, higherKey, false);
    }
}
