package chess.api.pieces;

public enum PieceType {
    PAWN(1),
    BISHOP(3),
    KNIGHT(3),
    ROOK(5),
    QUEEN(9),
    KING(0);

    private int value;

    private PieceType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
