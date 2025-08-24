package chess.api.storage.ephemeral;

import java.util.Set;

public class TrieNode {
    private Short moveTo;
    private Set<TrieNode> onwardNodes;

    public TrieNode(Short moveTo, Set<TrieNode> onwardNodes) {
        this.moveTo = moveTo;
        this.onwardNodes = onwardNodes;
    }

    public Short getMoveTo() {
        return moveTo;
    }

    public void setMoveTo(Short moveTo) {
        this.moveTo = moveTo;
    }

    public Set<TrieNode> getOnwardNodes() {
        return onwardNodes;
    }

    public void setOnwardNodes(Set<TrieNode> onwardNodes) {
        this.onwardNodes = onwardNodes;
    }
}
