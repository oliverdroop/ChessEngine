package chess.api.storage.ephemeral;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static chess.api.storage.ephemeral.MoveHistoryConverter.getLengthInShorts;
import static chess.api.storage.ephemeral.MoveHistoryConverter.toMoves;

public class InMemoryTrie {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryTrie.class);

    private static final Comparator<short[]> SHORT_ARRAY_COMPARATOR = new SawtoothShortArrayComparator();

    private static final Comparator<BigInteger> BIG_INTEGER_COMPARATOR = new BigIntegerComparator();

    private static final BinaryOperator<Double> MERGE_FUNCTION = (d1, d2) -> d2;

    private static final Supplier<TreeMap<BigInteger, Double>> TREE_MAP_SUPPLIER = () -> new TreeMap<>(BIG_INTEGER_COMPARATOR);

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

    public TreeMap<BigInteger, Double> getChildren(BigInteger moveHistory) {
        return getDescendants(moveHistory)
            .entrySet()
            .stream()
            .filter(entry -> getLengthInShorts(entry.getKey()) == getLengthInShorts(moveHistory) + 1)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, MERGE_FUNCTION, TREE_MAP_SUPPLIER));
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

    private NavigableMap<BigInteger, Double> getDescendants(BigInteger moveHistoryKey) {
        final Double value = trieMap.get(moveHistoryKey);
        if (value == null) {
            return Collections.emptyNavigableMap();
        }
        BigInteger higherKey = trieMap.higherKey(moveHistoryKey);
        while(higherKey != null
            && getLengthInShorts(higherKey) >= getLengthInShorts(moveHistoryKey)
            && Arrays.equals(toMoves(moveHistoryKey), Arrays.copyOfRange(toMoves(higherKey), 0, getLengthInShorts(moveHistoryKey)))) {
            higherKey = trieMap.higherKey(higherKey);
        }
        if (higherKey == null) {
            return trieMap.tailMap(moveHistoryKey, false);
        }
        return trieMap.subMap(moveHistoryKey, false, higherKey, false);
    }
}
