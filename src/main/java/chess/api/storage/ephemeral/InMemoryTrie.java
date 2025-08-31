package chess.api.storage.ephemeral;

import java.util.*;

public class InMemoryTrie {

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
        trieMap.keySet().removeIf(
            moveHistory -> {
                if (moveHistory.length < branchToPreserve.length) {
                    return true;
                }
                int comparison = SHORT_ARRAY_COMPARATOR.compare(moveHistory, branchToPreserve);
                return Math.abs(comparison) == 2;
            });
    }
}
