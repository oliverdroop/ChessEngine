package chess.api.storage.ephemeral;

import java.util.Map;

public class TrieNode {

    private Map<Short, TrieNode> onwardNodes;

    public TrieNode(Map<Short, TrieNode> onwardNodes) {
        this.onwardNodes = onwardNodes;
    }

    public Map<Short, TrieNode> getOnwardNodes() {
        return onwardNodes;
    }

    public void setOnwardNodes(Map<Short, TrieNode> onwardNodes) {
        this.onwardNodes = onwardNodes;
    }
}
