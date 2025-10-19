package chess.api.pieces;

public class Rook extends Piece {

    private static final int[][] DIRECTIONAL_LIMITS = {{0, -1, 7}, {-1, 0, 7}, {1, 0, 7}, {0, 1, 7}};

    public static final String AN_CODE = "R";

    public static int[][] getDirectionalLimits() {
        return DIRECTIONAL_LIMITS;
    }

    @Override
    public String getANCode() {
        return AN_CODE;
    }

    public static char getFENCode(int pieceBitFlag) {
        return (char) (82 + (getSide(pieceBitFlag) * 32));
    }
}
