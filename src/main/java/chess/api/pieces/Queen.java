package chess.api.pieces;

import chess.api.PieceConfiguration;
import chess.api.Position;
import chess.api.Side;

import java.util.List;

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
        return (char) (81 + (getSide(pieceBitFlag).ordinal() * 32));
    }

    public static int getValue() {
        return 9;
    }
}
