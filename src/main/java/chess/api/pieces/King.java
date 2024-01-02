package chess.api.pieces;

import chess.api.BitUtil;
import chess.api.PieceConfiguration;
import chess.api.Position;
import chess.api.Side;
import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.List;

public class King extends Piece{

    private static final int[][] STANDARD_DIRECTIONAL_LIMITS = {{-1, -1, 1}, {0, -1, 1}, {1, -1, 1}, {-1, 0, 1}, {1, 0, 1}, {-1, 1, 1}, {0, 1, 1}, {1, 1, 1}};

    private static final int[][] STARTING_POSITION_DIRECTIONAL_LIMITS = {{-1, -1, 1}, {0, -1, 1}, {1, -1, 1}, {-1, 0, 2}, {1, 0, 2}, {-1, 1, 1}, {0, 1, 1}, {1, 1, 1}};

    private static final ImmutableMap<Integer, Integer> CASTLE_POSITION_MAPPINGS = ImmutableMap.of(2, 0, 6, 7, 58, 56, 62, 63);

    public static final String AN_CODE = "K";

    public King(Side side, int position) {
        super(side, PieceType.KING, position);
    }

    @Override
    public int[][] getDirectionalLimits() {
        return isOnStartingPosition() ? STARTING_POSITION_DIRECTIONAL_LIMITS : STANDARD_DIRECTIONAL_LIMITS;
    }

    @Override
    public List<PieceConfiguration> getPossibleMoves(int[] positionBitFlags, PieceConfiguration currentConfiguration, boolean linkOnwardConfigurations) {
        List<PieceConfiguration> pieceConfigurations = new ArrayList<>();
        for(int[] directionalLimit : getMovableDirectionalLimits(positionBitFlags)) {
            int directionX = directionalLimit[0];
            int directionY = directionalLimit[1];
            int limit = directionalLimit[2];
            int testPositionIndex = getPosition();

            while (limit > 0) {
                testPositionIndex = Position.applyTranslation(testPositionIndex, directionX, directionY);
                if (testPositionIndex < 0 || testPositionIndex >= 64) {
                    break;
                }

                // Is this position threatened?
                if (BitUtil.hasBitFlag(positionBitFlags[testPositionIndex], PieceConfiguration.THREATENED)) {
                    break;
                }

                // Is this player piece blocked by another player piece?
                if (BitUtil.hasBitFlag(positionBitFlags[testPositionIndex], PieceConfiguration.PLAYER_OCCUPIED)) {
                    break;
                }

                // Is this an attempted castling?
                if (directionalLimit[2] == 2 && limit == 1 && !isLegalCastle(positionBitFlags, testPositionIndex)) {
                    break;
                }

                // Is there an opponent piece on the position?
                Piece takenPiece = null;
                if (BitUtil.hasBitFlag(positionBitFlags[testPositionIndex], PieceConfiguration.OPPONENT_OCCUPIED)) {
                    takenPiece = currentConfiguration.getPieceAtPosition(testPositionIndex);
                }

                addNewPieceConfigurations(pieceConfigurations, currentConfiguration, testPositionIndex, takenPiece, linkOnwardConfigurations);

                if (takenPiece != null) {
                    // Stop considering moves beyond this taken piece
                    break;
                }
                limit--;
            }
        }
        return pieceConfigurations;
    }

    @Override
    protected int[][] getMovableDirectionalLimits(int[] positionBitFlags) {
        int number = positionBitFlags[getPosition()];
        if (hasDirectionalFlags(number)) {
            return restrictDirections(~getDirectionalFlags(number));
        }
        return getDirectionalLimits();
    }

    private boolean isLegalCastle(int[] positionBitFlags, int testPositionIndex) {
        if (!BitUtil.hasBitFlag(positionBitFlags[testPositionIndex], PieceConfiguration.CASTLE_AVAILABLE)
                || BitUtil.hasBitFlag(positionBitFlags[getPosition()], PieceConfiguration.THREATENED) // No castling out of check
                || BitUtil.hasBitFlag(positionBitFlags[testPositionIndex], PieceConfiguration.OPPONENT_OCCUPIED) // No taking by castle
                || (Position.getX(testPositionIndex) == 2
                        && (BitUtil.hasBitFlag(positionBitFlags[testPositionIndex - 1], PieceConfiguration.PLAYER_OCCUPIED)
                                || BitUtil.hasBitFlag(positionBitFlags[testPositionIndex - 1], PieceConfiguration.OPPONENT_OCCUPIED)))) {
            return false;
        }
        return true;
    }

    @Override
    public int[] stampThreatFlags(int[] positionBitFlags) {
        for(int[] directionalLimit : getDirectionalLimits()) {
            int direction = directionalLimit[0];
            // Limit is always 1 for the king (castling positions can't be threatened by the king)
            int testPositionIndex = Position.applyTranslation(getPosition(), directionalLimit[0], directionalLimit[1]);
            if (testPositionIndex < 0 || testPositionIndex >= 64) {
                continue;
            }

            positionBitFlags[testPositionIndex] = BitUtil.applyBitFlag(positionBitFlags[testPositionIndex], PieceConfiguration.THREATENED);
        }
        return positionBitFlags;
    }

    private boolean isOnStartingPosition() {
        return getPosition() == 4 + (getSide().ordinal() * 56);
    }

    @Override
    protected void addNewPieceConfigurations(List<PieceConfiguration> pieceConfigurations,
            PieceConfiguration currentConfiguration, int newPiecePosition, Piece takenPiece, boolean linkOnwardConfigurations) {
        super.addNewPieceConfigurations(pieceConfigurations, currentConfiguration, newPiecePosition, takenPiece, linkOnwardConfigurations);
        if (isOnStartingPosition()) {
            PieceConfiguration newPieceConfiguration = pieceConfigurations.get(pieceConfigurations.size() - 1);
            newPieceConfiguration.removeCastlePosition(getPosition() - 2);
            newPieceConfiguration.removeCastlePosition(getPosition() + 2);
            if (CASTLE_POSITION_MAPPINGS.containsKey(newPiecePosition)) {
                Piece castlingRook = currentConfiguration.getPieceAtPosition(CASTLE_POSITION_MAPPINGS.get(newPiecePosition));
                newPieceConfiguration.removePiece(castlingRook);
                newPieceConfiguration.addPiece(new Rook(getSide(), (getPosition() + newPiecePosition) / 2));
            }
        }
    }

    @Override
    public String getANCode() {
        return AN_CODE;
    }

    public char getFENCode() {
        return (char) (75 + (getSide().ordinal() * 32));
    }

    public int getValue() {
        return 0;
    }
}
