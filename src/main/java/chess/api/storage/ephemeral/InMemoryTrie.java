package chess.api.storage.ephemeral;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class InMemoryTrie {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryTrie.class);

    private static final Comparator<short[]> SHORT_ARRAY_COMPARATOR = new SawtoothShortArrayComparator();

    private static final BinaryOperator<Double> MERGE_FUNCTION = (d1, d2) -> d2;

    private static final Supplier<TreeMap<short[], Double>> TREE_MAP_SUPPLIER = () -> new TreeMap<>(SHORT_ARRAY_COMPARATOR);

    private final TreeMap<short[], Double> trieMap = TREE_MAP_SUPPLIER.get();

    public InMemoryTrie() {}

    public TreeMap<short[], Double> getTrieMap() {
        return trieMap;
    }

    public Double getScore(short[] moveHistory) {
        return trieMap.get(moveHistory);
    }

    public void setScore(short[] moveHistory, double score) {
        if (moveHistory == null) {
            throw new IllegalArgumentException("Cannot set the score for a null collection of moves");
        }
        trieMap.put(moveHistory, score);
    }

    public TreeMap<short[], Double> getChildren(short[] moveHistory) {
        return getDescendants(moveHistory)
            .entrySet()
            .stream()
            .filter(entry -> entry.getKey().length == moveHistory.length + 1)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, MERGE_FUNCTION, TREE_MAP_SUPPLIER));
    }

    private NavigableMap<short[], Double> getDescendants(short[] moveHistory) {
        final Double value = trieMap.get(moveHistory);
        if (value == null) {
            return Collections.emptyNavigableMap();
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
