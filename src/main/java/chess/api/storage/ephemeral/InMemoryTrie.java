package chess.api.storage.ephemeral;

import java.util.*;

public class InMemoryTrie {

    private static final IntArrayComparator SHORT_ARRAY_COMPARATOR = new IntArrayComparator();

    private final Map<int[], int[]> trieMap = new TreeMap<>(SHORT_ARRAY_COMPARATOR);

    public InMemoryTrie() {
        trieMap.put(new int[]{}, null);
    }

    public Optional<int[]> getAvailableMoves(int[] movesSoFar) {
        final Optional<int[]> optionalTrieNode = getNodeAtPath(movesSoFar);
        if (optionalTrieNode.isPresent()) {
            final int[] onwardMoves = optionalTrieNode.get();
            return Optional.of(onwardMoves);
        }
        return Optional.empty();
    }

    public void setAvailableMoves(int[] movesSoFar, int[] availableMoves) {
        trieMap.put(movesSoFar, availableMoves);
    }

    public Optional<int[]> getNodeAtPath(int[] movesSoFar) {
        if (movesSoFar == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(trieMap.get(movesSoFar));
    }

    public void prune(int[] branchToPreserve) {
        trieMap.keySet().removeIf(
            moveHistory -> {
                int comparison = SHORT_ARRAY_COMPARATOR.compare(moveHistory, branchToPreserve);
                return Math.abs(comparison) == 2;
            });
    }
}
