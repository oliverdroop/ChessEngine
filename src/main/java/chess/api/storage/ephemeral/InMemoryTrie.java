package chess.api.storage.ephemeral;

import chess.api.*;

import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static chess.api.MoveDescriber.describeMove;
import static chess.api.MoveDescriber.getMoveFromAlgebraicNotation;
import static chess.api.PieceConfiguration.*;
import static java.lang.String.format;

public class InMemoryTrie {

    private final TrieNode rootNode;

    public InMemoryTrie() {
        final PieceConfiguration startingConfiguration = FENReader.read(FENWriter.STARTING_POSITION);
        final Set<TrieNode> onwardNodes = startingConfiguration
            .getPossiblePieceConfigurations()
            .stream()
            .map(onwardConfiguration -> getMoveFromAlgebraicNotation(
                onwardConfiguration.getAlgebraicNotation(startingConfiguration)
            ))
            .map(shrt -> new TrieNode(shrt, null))
            .collect(Collectors.toSet());
        this.rootNode = new TrieNode(null, onwardNodes);
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
                        .collect(Collectors.toSet()));
            }
        }
        return Optional.empty();
    }

    public Optional<Set<Short>> getAvailableMoves2(short[] movesSoFar) {
        int index = 0;
        if (movesSoFar == null) {
            return Optional.empty();
        }
        TrieNode currentNode = rootNode;
        while(index < movesSoFar.length) {
            final short moveTo = movesSoFar[index];
            final Optional<TrieNode> onwardNode = currentNode.getOnwardNodes()
                .stream()
                .filter(trieNode -> trieNode.getMoveTo() == moveTo)
                .findFirst();

            if (onwardNode.isEmpty()) {
                return Optional.empty();
            } else if (onwardNode.get().getOnwardNodes() == null) {
                // Construct the next part of the trie
                final short[] path = Arrays.copyOfRange(movesSoFar, 0, index + 1);
                final PieceConfiguration currentConfiguration = getPieceConfigurationFromPath(path);
                onwardNode.get().setOnwardNodes(currentConfiguration.getPossiblePieceConfigurations()
                    .stream()
                    .map(onwardConfiguration -> onwardConfiguration.getAlgebraicNotation(currentConfiguration))
                    .map(MoveDescriber::getMoveFromAlgebraicNotation)
                    .map(move -> new TrieNode(move, null))
                    .collect(Collectors.toSet()));
            }
            currentNode = onwardNode.get();
            index++;
        }
        return Optional.of(currentNode.getOnwardNodes().stream().map(TrieNode::getMoveTo).collect(Collectors.toSet()));
    }

    public void setAvailableMoves(short[] movesSoFar, Collection<String> algebraicNotations) {
        Optional<TrieNode> node = getNodeAtPath(movesSoFar);
        if (node.isPresent()) {
            final Set<TrieNode> onwardNodes = algebraicNotations.stream()
                .map(MoveDescriber::getMoveFromAlgebraicNotation)
                .map(onwardMove -> new TrieNode(onwardMove, null))
                .collect(Collectors.toSet());
            node.get().setOnwardNodes(onwardNodes);
        }
    }

    public Optional<TrieNode> getNodeAtPath(short[] movesSoFar) {
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
    
    private static PieceConfiguration getPieceConfigurationFromPath(short[] path) {
        PieceConfiguration currentConfiguration = FENReader.read(FENWriter.STARTING_POSITION);
        int index = 0;
        while(index < path.length) {
            final short move = path[index];
            final PieceConfiguration finalCurrentConfiguration = currentConfiguration;
            Optional<PieceConfiguration> nextConfiguration = currentConfiguration.getPossiblePieceConfigurations()
                .stream()
                .filter(onwardConfiguration -> getMoveFromAlgebraicNotation(onwardConfiguration.getAlgebraicNotation(finalCurrentConfiguration)) == move)
                .findFirst();
            if (nextConfiguration.isEmpty()) {
                return null;
            }
            currentConfiguration = nextConfiguration.get();
            index++;
        }
        return currentConfiguration;
    }
}
