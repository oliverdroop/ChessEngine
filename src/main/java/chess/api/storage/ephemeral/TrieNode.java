package chess.api.storage.ephemeral;

import java.util.List;
import java.util.Set;

public record TrieNode(List<Short> path, Set<Short> onwards) {}
