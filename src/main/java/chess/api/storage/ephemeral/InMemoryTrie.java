package chess.api.storage.ephemeral;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static chess.api.storage.ephemeral.MoveHistoryConverter.*;

public class InMemoryTrie {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryTrie.class);

    private static final Comparator<short[]> SHORT_ARRAY_COMPARATOR = new SawtoothShortArrayComparator();

    private static final Comparator<BigInteger> BIG_INTEGER_COMPARATOR = new BigIntegerComparator();

    private static final BinaryOperator<Double> MERGE_FUNCTION = (d1, d2) -> d2;

    private static final Supplier<TreeMap<BigInteger, Double>> TREE_MAP_SUPPLIER = TreeMap::new;

    private final TreeMap<BigInteger, Double> trieMap = TREE_MAP_SUPPLIER.get();

    private int leftShift = 0;

    public InMemoryTrie() {}

    public TreeMap<BigInteger, Double> getTrieMap() {
        return trieMap;
    }

    public Double getScore(BigInteger moveHistory) {
        return trieMap.get(moveHistory);
    }

    public void setScore(BigInteger moveHistory, double score) {
        if (moveHistory == null) {
            return;
        }
        trieMap.put(moveHistory, score);
    }

    public void shiftKeysLeft() {
        final TreeMap<BigInteger, Double> trieMapCopy = new TreeMap<>(trieMap);
        for(Map.Entry<BigInteger, Double> entry : trieMapCopy.entrySet()) {
            final BigInteger oldKey = entry.getKey();
            final Double value = entry.getValue();
            final BigInteger newKey = oldKey.shiftLeft(16);
            trieMap.remove(oldKey);
            trieMap.put(newKey, value);
        }
        leftShift += 16;
    }

    public int countTrailingEmptyShorts(BigInteger value) {
        if (BigInteger.ZERO.equals(value)) {
            return (leftShift / 16) + 1;
        }
        return value.getLowestSetBit() / 16;
    }

    public TreeMap<BigInteger, Double> getChildren(BigInteger moveHistoryKey) {
        final int trailingEmptyShorts = countTrailingEmptyShorts(moveHistoryKey);
        return getDescendants(moveHistoryKey)
            .entrySet()
            .stream()
            .filter(entry -> trailingEmptyShorts - countTrailingEmptyShorts(entry.getKey()) == 1)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, MERGE_FUNCTION, TREE_MAP_SUPPLIER));
    }

    private NavigableMap<BigInteger, Double> getDescendants(BigInteger moveHistoryKey) {
        final Double value = trieMap.get(moveHistoryKey);
        if (value == null) {
            return Collections.emptyNavigableMap();
        }
        final int trailingEmptyShorts = countTrailingEmptyShorts(moveHistoryKey);
        BigInteger higherKey = trieMap.higherKey(moveHistoryKey);
        while(higherKey != null && countTrailingEmptyShorts(higherKey) < trailingEmptyShorts) {
            higherKey = trieMap.higherKey(higherKey);
        }
        if (higherKey == null) {
            return trieMap.tailMap(moveHistoryKey, false);
        }
        return trieMap.subMap(moveHistoryKey, false, higherKey, false);
    }
}
