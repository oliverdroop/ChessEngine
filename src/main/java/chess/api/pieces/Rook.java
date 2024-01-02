package chess.api.pieces;

import chess.api.PieceConfiguration;
import chess.api.Side;
import com.google.common.collect.ImmutableMap;

import java.util.List;

public class Rook extends Piece {

    private static final int[][] DIRECTIONAL_LIMITS = {{0, -1, 7}, {-1, 0, 7}, {1, 0, 7}, {0, 1, 7}};

    private static final ImmutableMap<Integer, Integer> CASTLE_POSITION_MAPPINGS = ImmutableMap.of(0, 2, 7, 6, 56, 58, 63, 62);

    public static final String AN_CODE = "R";

    public Rook(Side side, int position) {
        super(side, PieceType.ROOK, position);
    }

    @Override
    public int[][] getDirectionalLimits() {
        return DIRECTIONAL_LIMITS;
    }

    @Override
    public int[] stampThreatFlags(int[] positionBitFlags) {
        return stampSimpleThreatFlags(positionBitFlags);
    }

    @Override
    protected void addNewPieceConfigurations(List<PieceConfiguration> pieceConfigurations,
            PieceConfiguration currentConfiguration, int newPiecePosition, Piece takenPiece, boolean linkOnwardConfigurations) {
        super.addNewPieceConfigurations(pieceConfigurations, currentConfiguration, newPiecePosition, takenPiece, linkOnwardConfigurations);
        if (CASTLE_POSITION_MAPPINGS.containsKey(getPosition())) {
            PieceConfiguration newPieceConfiguration = pieceConfigurations.get(pieceConfigurations.size() - 1);
            newPieceConfiguration.removeCastlePosition(CASTLE_POSITION_MAPPINGS.get(getPosition()));
        }
    }

    @Override
    public String getANCode() {
        return AN_CODE;
    }

    public char getFENCode() {
        return (char) (82 + (getSide().ordinal() * 32));
    }

    public int getValue() {
        return 5;
    }
}
