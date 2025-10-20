package chess.api.pieces;

public class Knight extends Piece{

    private static final int[][] DIRECTIONAL_LIMITS = {{-1, -2, 1}, {1, -2, 1}, {-2, -1, 1}, {2, -1, 1}, {-2, 1, 1}, {2, 1, 1}, {-1, 2, 1}, {1, 2, 1}};

    public static int[][] getDirectionalLimits() {
        return DIRECTIONAL_LIMITS;
    }

    public static char getFENCode(int pieceBitFlag) {
        return (char) (78 + (getSide(pieceBitFlag) * 32));
    }
}
