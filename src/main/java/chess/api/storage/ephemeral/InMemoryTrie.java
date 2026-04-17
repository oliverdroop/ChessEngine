package chess.api.storage.ephemeral;

import chess.api.ai.BreadthFirstPositionEvaluator;
import com.google.common.collect.TreeMultimap;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
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
            if (key.length != maxDepth) {
                continue;
            }
            final double[] values = new double[currentDepth];
            for(int moveIndex = 1; moveIndex < currentDepth; moveIndex++) {
                final short[] moveHistory = Arrays.copyOfRange(key, 0, startingDepth + moveIndex);
                values[moveIndex - 1] = trieMap.getOrDefault(moveHistory, 0.0); // Only null at end of game
            }
            final double value = entry.getValue();
            values[currentDepth - 1] = value;
            multimap.put(getBranchValue(values), key);
        }

        // Prune the branches with the most pronounced downward trend in values
//        final double cutoffProportion = 1 - (1 / Math.pow(10, currentDepth));
//        final int cutoffIndex = (int) Math.floor(multimap.size() * cutoffProportion);
        final int cutoffIndex = Math.max(multimap.size() - 1024, 0);
        final double cutoffKey = multimap.entries().stream().map(Map.Entry::getKey).toList().get(cutoffIndex);
        final AtomicInteger pruneCount = new AtomicInteger(0);
        multimap
            .asMap()
            .headMap(cutoffKey)
            .values()
            .stream()
            .flatMap(Collection::stream)
            .forEach(moveHistory -> {
                pruneCount.incrementAndGet();
                trieMap.remove(moveHistory);
            });
        LOGGER.info("Pruned {} values with depth {}", pruneCount.get(), currentDepth);
    }

    private double getBranchValue(double[] values) {
        final SimpleRegression simpleRegression = new SimpleRegression();
        simpleRegression.addData(0, values[0]);
        for(int valueIndex = 1; valueIndex < values.length; valueIndex++) {
            final double parentValue = values[valueIndex - 1];
            final double childValue = values[valueIndex];
            simpleRegression.addData(valueIndex, BreadthFirstPositionEvaluator.accumulate(parentValue, childValue));
        }
        return simpleRegression.predict(values.length);
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
