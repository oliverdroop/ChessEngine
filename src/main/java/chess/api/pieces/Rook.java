package chess.api.pieces;

import chess.api.PieceConfiguration;
import com.google.common.collect.ImmutableMap;

import java.util.List;

public class Rook extends Piece {

    private static final int[][] DIRECTIONAL_LIMITS = {{0, -1, 7}, {-1, 0, 7}, {1, 0, 7}, {0, 1, 7}};

    private static final ImmutableMap<Integer, Integer> CASTLE_POSITION_MAPPINGS = ImmutableMap.of(0, 2, 7, 6, 56, 58, 63, 62);

    public static final String AN_CODE = "R";

    public static int[][] getDirectionalLimits() {
        return DIRECTIONAL_LIMITS;
    }

    protected static void removeCastlingOptions(int pieceBitFlag, PieceConfiguration pieceConfiguration) {
        if (CASTLE_POSITION_MAPPINGS.containsKey(getPosition(pieceBitFlag))) {
            pieceConfiguration.removeCastlePosition(CASTLE_POSITION_MAPPINGS.get(getPosition(pieceBitFlag)));
        }
    }

    @Override
    public String getANCode() {
        return AN_CODE;
    }

    public static char getFENCode(int pieceBitFlag) {
        return (char) (82 + (getSide(pieceBitFlag) * 32));
    }

    public static int getValue() {
        return 5;
    }
}
