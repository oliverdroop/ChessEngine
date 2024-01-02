package chess.api.pieces;

import chess.api.Position;
import chess.api.Side;

import java.util.List;

public class Knight extends Piece{

    private static final int[][] DIRECTIONAL_LIMITS = {{-1, -2, 1}, {1, -2, 1}, {-2, -1, 1}, {2, -1, 1}, {-2, 1, 1}, {2, 1, 1}, {-1, 2, 1}, {1, 2, 1}};

    public static final String AN_CODE = "N";

    public Knight(Side side, int position) {
        super(side, PieceType.KNIGHT, position);
    }

    @Override
    public int[][] getDirectionalLimits() {
        return DIRECTIONAL_LIMITS;
    }

    @Override
    public String getANCode() {
        return AN_CODE;
    }

    public static int[][] getKnightDirectionalLimits() {
        return DIRECTIONAL_LIMITS;
    }

    @Override
    public int[] stampThreatFlags(int[] positionBitFlags) {
        return stampSimpleThreatFlags(positionBitFlags);
    }

    public char getFENCode() {
        return (char) (78 + (getSide().ordinal() * 32));
    }

    public int getValue() {
        return 3;
    }
}
