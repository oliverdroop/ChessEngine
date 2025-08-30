package chess.api.storage.ephemeral;

import chess.api.*;

import java.util.*;
import java.util.stream.Collectors;


public class InMemoryTrie {

    private final TrieNode rootNode;

    public InMemoryTrie() {
        this.rootNode = new TrieNode(null);
    }

    public Optional<Set<Short>> getAvailableMoves(short[] movesSoFar) {
        Optional<TrieNode> optionalTrieNode = getNodeAtPath(movesSoFar);
        if (optionalTrieNode.isPresent()) {
            final Map<Short, TrieNode> onwardNodes = optionalTrieNode.get().getOnwardNodes();
            if (onwardNodes != null) {
                return Optional.of(onwardNodes.keySet());
            }
        }
        return Optional.empty();
    }

    public void setAvailableMoves(short[] movesSoFar, Collection<String> algebraicNotations) {
        final Optional<TrieNode> node = getNodeAtPath(movesSoFar);
        if (node.isPresent()) {
            final Map<Short, TrieNode> onwardNodes = algebraicNotations.stream()
                .map(MoveDescriber::getMoveFromAlgebraicNotation)
                .collect(Collectors.toMap(move -> move, move -> new TrieNode(null)));
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
}
