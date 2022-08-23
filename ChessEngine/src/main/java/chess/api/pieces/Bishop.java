package chess.api.pieces;

import chess.api.PieceConfiguration;
import chess.api.Position;
import chess.api.Side;

import java.util.List;

public class Bishop extends Piece {

    private static final int[][] DIRECTIONAL_LIMITS = { {-1, -1, 7}, {1, -1, 7}, {-1, 1, 7}, {1, 1, 7} };

    public Bishop(Side side, int position) {
        super(side, PieceType.BISHOP, position);
    }

    @Override
    public int[][] getDirectionalLimits() {
        return DIRECTIONAL_LIMITS;
    }

    @Override
    public int[] stampThreatFlags(int[] positionBitFlags) {
        return stampSimpleThreatFlags(positionBitFlags);
    }

    public char getFENCode() {
        return (char) (66 + (getSide().ordinal() * 32));
    }

    public int getValue() {
        return 3;
    }
}
