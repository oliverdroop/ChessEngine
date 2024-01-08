package chess.api.pieces;

import chess.api.PieceConfiguration;
import chess.api.Position;
import chess.api.Side;

import java.util.List;

public class Bishop extends Piece {

    private static final int[][] DIRECTIONAL_LIMITS = { {-1, -1, 7}, {1, -1, 7}, {-1, 1, 7}, {1, 1, 7} };

    public static final String AN_CODE = "B";

    public static int[][] getDirectionalLimits() {
        return DIRECTIONAL_LIMITS;
    }

    @Override
    public String getANCode() {
        return AN_CODE;
    }

    public static char getFENCode(int pieceBitFlag) {
        return (char) (66 + (getSide(pieceBitFlag).ordinal() * 32));
    }

    public static int getValue() {
        return 3;
    }
}
