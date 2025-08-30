package chess.api.storage.ephemeral;

import chess.api.*;

import java.util.*;
import java.util.stream.Collectors;


public class InMemoryTrie {

    private final TrieNode rootNode;


    public InMemoryTrie() {
        this.rootNode = new TrieNode(null, null);
    }

    public Optional<Set<Short>> getAvailableMoves(short[] movesSoFar) {
        Optional<TrieNode> optionalTrieNode = getNodeAtPath(movesSoFar);
        if (optionalTrieNode.isPresent()) {
            final Set<TrieNode> onwardNodes = optionalTrieNode.get().getOnwardNodes();
            if (onwardNodes != null) {
                return Optional.of(
                    onwardNodes
                        .stream()
                        .map(TrieNode::getMoveTo)
                        .collect(Collectors.toCollection(HashSet::new)));
            }
        }
        return Optional.empty();
    }

    public void setAvailableMoves(short[] movesSoFar, Collection<String> algebraicNotations) {
        final Optional<TrieNode> node = getNodeAtPath(movesSoFar);
        if (node.isPresent()) {
            final Set<TrieNode> onwardNodes = algebraicNotations.stream()
                .map(MoveDescriber::getMoveFromAlgebraicNotation)
                .map(onwardMove -> new TrieNode(onwardMove, null))
                .collect(Collectors.toSet());
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
            final Optional<TrieNode> onwardNode = currentNode.getOnwardNodes()
                .stream()
                .filter(trieNode -> trieNode.getMoveTo() == moveTo)
                .findFirst();

            if (onwardNode.isPresent()) {
                currentNode = onwardNode.get();
            } else {
                return Optional.empty();
            }
            index++;
        }
        return Optional.of(currentNode);
    }
}
