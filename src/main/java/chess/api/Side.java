package chess.api;

public enum Side {
    WHITE, BLACK;

    public String getAbbreviation() {
        return this.name().substring(0, 1).toLowerCase();
    }

    public Side getOpposingSide() {
        return Side.values()[1 - this.ordinal()];
    }
}
