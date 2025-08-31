package chess.api.storage.ephemeral;

import java.util.*;

public class InMemoryTrie {

    private static final IntArrayComparator SHORT_ARRAY_COMPARATOR = new IntArrayComparator();

    private final Map<short[], short[]> trieMap = new TreeMap<>(SHORT_ARRAY_COMPARATOR);

    public InMemoryTrie() {
        trieMap.put(new short[]{}, null);
    }

    public Optional<short[]> getAvailableMoves(short[] movesSoFar) {
        final Optional<short[]> optionalTrieNode = getNodeAtPath(movesSoFar);
        if (optionalTrieNode.isPresent()) {
            final short[] onwardMoves = optionalTrieNode.get();
            return Optional.of(onwardMoves);
        }
        return Optional.empty();
    }

    public void setAvailableMoves(short[] movesSoFar, short[] availableMoves) {
        trieMap.put(movesSoFar, availableMoves);
    }

    public Optional<short[]> getNodeAtPath(short[] movesSoFar) {
        if (movesSoFar == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(trieMap.get(movesSoFar));
    }

    public void prune(short[] branchToPreserve) {
        trieMap.keySet().removeIf(
            moveHistory -> {
                int comparison = SHORT_ARRAY_COMPARATOR.compare(moveHistory, branchToPreserve);
                return Math.abs(comparison) == 2;
            });
    }
}
