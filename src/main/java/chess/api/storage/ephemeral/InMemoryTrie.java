package chess.api.storage.ephemeral;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class InMemoryTrie {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryTrie.class);

    private static final ShortArrayComparator SHORT_ARRAY_COMPARATOR = new ShortArrayComparator();

    private final Map<short[], short[]> trieMap = new TreeMap<>(SHORT_ARRAY_COMPARATOR);

    public InMemoryTrie() {
        trieMap.put(new short[]{}, null);
    }

    public Optional<short[]> getAvailableMoves(short[] movesSoFar) {
        if (movesSoFar == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(trieMap.get(movesSoFar));
    }

    public void setAvailableMoves(short[] movesSoFar, short[] availableMoves) {
        trieMap.put(movesSoFar, availableMoves);
    }

    public void prune(short[] branchToPreserve) {
        long t1 = System.currentTimeMillis();
        trieMap.keySet().removeIf(
            moveHistory -> {
                if (moveHistory.length < branchToPreserve.length) {
                    return true;
                }
                int comparison = SHORT_ARRAY_COMPARATOR.compare(moveHistory, branchToPreserve);
                return Math.abs(comparison) == 2;
            });
        long t2 = System.currentTimeMillis();
        LOGGER.debug("Pruning trie took {} ms", t2 - t1);
    }
}
