package chess.api.pieces;

import chess.api.Position;
import chess.api.Side;

import java.util.List;

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

    public static int[][] getKnightDirectionalLimits() {
        return DIRECTIONAL_LIMITS;
    }

    public static char getFENCode(int pieceBitFlag) {
        return (char) (78 + (getSide(pieceBitFlag).ordinal() * 32));
    }

    public static int getValue() {
        return 3;
    }
}
