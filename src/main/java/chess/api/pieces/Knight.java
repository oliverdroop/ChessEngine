package chess.api.pieces;

public class Knight extends Piece{

    private static final int[][] DIRECTIONAL_LIMITS = {{-1, -2, 1}, {1, -2, 1}, {-2, -1, 1}, {2, -1, 1}, {-2, 1, 1}, {2, 1, 1}, {-1, 2, 1}, {1, 2, 1}};

    public static final String AN_CODE = "N";

    public static int[][] getDirectionalLimits() {
        return DIRECTIONAL_LIMITS;
    }

    @Override
    public String getANCode() {
        return AN_CODE;
    }

    public static char getFENCode(int pieceBitFlag) {
        return (char) (78 + (getSide(pieceBitFlag) * 32));
    }
}
