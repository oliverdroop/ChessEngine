package chess.api.storage.ephemeral;

import com.google.common.collect.TreeMultimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class InMemoryTrie {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryTrie.class);

    private static final Comparator<short[]> SHORT_ARRAY_COMPARATOR = new SawtoothShortArrayComparator();

    private static final BinaryOperator<Double> MERGE_FUNCTION = (d1, d2) -> d2;

    private static final Supplier<NavigableMap<short[], Double>> TREE_MAP_SUPPLIER = () -> new ConcurrentSkipListMap<>(SHORT_ARRAY_COMPARATOR);

    private final NavigableMap<short[], Double> trieMap = TREE_MAP_SUPPLIER.get();

    public InMemoryTrie() {}

    public NavigableMap<short[], Double> getTrieMap() {
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

    public Map<short[], Double> getChildren(short[] moveHistory) {
        return getDescendants(moveHistory)
            .entrySet()
            .stream()
            .filter(entry -> entry.getKey().length == moveHistory.length + 1)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, MERGE_FUNCTION, TREE_MAP_SUPPLIER));
    }

    public void prune(int currentDepth) {
        final int maxDepth = trieMap.keySet().stream()
            .map(k -> k.length)
            .max(Comparator.naturalOrder())
            .orElse(0);
        final int startingDepth = maxDepth - currentDepth;
        final TreeMultimap<Double, short[]> multimap = TreeMultimap.create(Comparator.naturalOrder(), SHORT_ARRAY_COMPARATOR);

        for(Map.Entry<short[], Double> entry : trieMap.entrySet()) {
            final short[] key = entry.getKey();
            if (key.length < maxDepth) {
                continue;
            }
            final double[] values = new double[currentDepth];
            for(int moveIndex = 1; moveIndex < maxDepth - startingDepth; moveIndex++) {
                short[] moveHistory = Arrays.copyOfRange(key, 0, startingDepth + moveIndex);
                values[moveIndex - 1] = trieMap.get(moveHistory);
            }
            final double value = entry.getValue();
            values[maxDepth - startingDepth - 1] = value;

            final double branchValue = getBranchValue(values);

            multimap.put(branchValue, key);
        }

        // Prune the uninteresting branches
        final int cutoffIndex = (int) Math.floor(multimap.size() * 0.9);
        final double cutoffKey = multimap.entries().stream().map(Map.Entry::getKey).toList().get(cutoffIndex);
        for(double branchValue : multimap.keySet().headSet(cutoffKey)) {
            NavigableSet<short[]> moveHistories = multimap.get(branchValue);
            for(short[] moveHistory : moveHistories) {
                trieMap.remove(moveHistory);
            }
        }
    }

    private double getBranchValue(double[] values) {
        // Calculate the standard deviation from the mean
        final int currentDepth = values.length;
        final double meanValue = Arrays.stream(values).sum() / currentDepth;
        return Math.sqrt(
            Arrays.stream(values)
                .map(v -> Math.pow(v - meanValue, 2))
                .sum()
                / currentDepth);
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
