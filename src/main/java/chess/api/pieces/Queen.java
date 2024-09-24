package chess.api.pieces;

public class Queen extends Piece{

    public static final String AN_CODE = "Q";

    private static final int[][] DIRECTIONAL_LIMITS = {{-1, -1, 7}, {1, -1, 7}, {-1, 1, 7}, {1, 1, 7}, {0, -1, 7}, {-1, 0, 7}, {1, 0, 7}, {0, 1, 7}};

    public static int[][] getDirectionalLimits() {
        return DIRECTIONAL_LIMITS;
    }

    @Override
    public String getANCode() {
        return AN_CODE;
    }

    public static char getFENCode(int pieceBitFlag) {
        return (char) (81 + (getSide(pieceBitFlag) * 32));
    }
}
