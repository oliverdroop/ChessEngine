package chess.api.storage.ephemeral;

import chess.api.FENReader;
import chess.api.FENWriter;
import chess.api.PieceConfiguration;
import chess.api.Position;

import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class InMemoryTrie {

    private static final Map<String, Integer> PROMOTION_BIT_FLAGS = Map.of("q", 8, "r", 4, "b", 2, "n", 1);

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
        int index = 0;
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
                    .map(InMemoryTrie::getMoveFromAlgebraicNotation)
                    .map(move -> new TrieNode(move, null))
                    .collect(Collectors.toSet()));
            }
            currentNode = onwardNode.get();
            index++;
        }
        return Optional.of(currentNode.getOnwardNodes().stream().map(TrieNode::getMoveTo).collect(Collectors.toSet()));
    }

    static short getMoveFromAlgebraicNotation(String algebraicNotation) {
        final Pattern positionPattern = Pattern.compile("[a-h][1-8]");
        final Pattern promotionPattern = Pattern.compile("[qrbn]$");
        final int[] fromAndToPositions = positionPattern.matcher(algebraicNotation)
            .results()
            .mapToInt(matchResult -> Position.getPosition(matchResult.group()))
            .toArray();

        if (fromAndToPositions.length != 2) {
            throw new IllegalArgumentException(
                format("Unable to get move from algebraic notation %s", algebraicNotation));
        }
        final String promotionString = promotionPattern.matcher(algebraicNotation)
            .results()
            .map(MatchResult::group)
            .reduce(String::concat)
            .orElse("");
        final int promotionInt = PROMOTION_BIT_FLAGS.getOrDefault(promotionString, 0);
        final int outputInt = (promotionInt << 12) | (fromAndToPositions[0] << 6) | (fromAndToPositions[1]);
        return (short) outputInt;
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
