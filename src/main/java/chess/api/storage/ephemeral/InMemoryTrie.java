package chess.api.storage.ephemeral;

import chess.api.*;

import java.util.*;
import java.util.stream.Collectors;


public class InMemoryTrie {

    private final TrieNode rootNode;

//    private final Map<short[], TrieNode> trieMap = new TreeMap<>(new ShortArrayComparator());

    public InMemoryTrie() {
        this.rootNode = new TrieNode(null);
    }

    public Optional<Set<Short>> getAvailableMoves(short[] movesSoFar) {
        final Optional<TrieNode> optionalTrieNode = getNodeAtPath(movesSoFar);
        if (optionalTrieNode.isPresent()) {
            final Map<Short, TrieNode> onwardNodes = optionalTrieNode.get().getOnwardNodes();
            if (onwardNodes != null) {
                return Optional.of(onwardNodes.keySet());
            }
        }
        return Optional.empty();
    }

    public void setAvailableMoves(short[] movesSoFar, Collection<Short> availableMoves) {
        final Optional<TrieNode> node = getNodeAtPath(movesSoFar);
        if (node.isPresent()) {
            final TreeMap<Short, TrieNode> onwardNodes = availableMoves.stream()
                .collect(
                    Collectors.toMap(
                        move -> move, // Key function
                        move -> new TrieNode(null), // Value function
                        (v1, v2) -> v1, // Merge function (required but not used)
                        TreeMap::new // Map supplier
                    ));
            node.get().setOnwardNodes(onwardNodes);
        }
    }

    public Optional<TrieNode> getNodeAtPath(short[] movesSoFar) {
        if (movesSoFar == null) {
            return Optional.empty();
        }
        int index = 0;
        TrieNode currentNode = rootNode;
        while(index < movesSoFar.length) {
            final short moveTo = movesSoFar[index];
            final Map<Short, TrieNode> onwardNodes = currentNode.getOnwardNodes();
            final TrieNode onwardNode = onwardNodes.get(moveTo);

            if (onwardNode != null) {
                currentNode = onwardNode;
            } else {
                return Optional.empty();
            }
            index++;
        }
        return Optional.of(currentNode);
    }

    public void prune(short[] branchToPreserve) {
        TrieNode currentNode = rootNode;
        int index = 0;
        while (index < branchToPreserve.length && currentNode != null) {
            final short nextMove = branchToPreserve[index];
            currentNode.getOnwardNodes().keySet().removeIf(move -> move != nextMove);
            currentNode = currentNode.getOnwardNodes().get(nextMove);
            index++;
        }
    }
}
