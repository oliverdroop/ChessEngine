package chess.api.storage.ephemeral;

public class MoveLink {

    private short move;
    private MoveLink parentMoveLink;
    private MoveLink[] childMoveLinks;

    public MoveLink(short move, MoveLink parentMoveLink, MoveLink[] childMoveLinks) {
        this.move = move;
        this.parentMoveLink = parentMoveLink;
        this.childMoveLinks = childMoveLinks;
    }

    public short getMove() {
        return move;
    }

    public void setMove(short move) {
        this.move = move;
    }

    public MoveLink getParentMove() {
        return parentMoveLink;
    }

    public void setParentMove(MoveLink parentMoveLink) {
        this.parentMoveLink = parentMoveLink;
    }

    public MoveLink[] getChildMoves() {
        return childMoveLinks;
    }

    public void setChildMoves(MoveLink[] childMoveLinks) {
        this.childMoveLinks = childMoveLinks;
    }
}
