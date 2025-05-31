package chess.api.pieces;

import chess.api.BitUtil;
import chess.api.PieceConfiguration;
import chess.api.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static chess.api.Position.isValidPosition;

public class King extends Piece{

    private static final int[][] STANDARD_DIRECTIONAL_LIMITS = {{-1, -1, 1}, {0, -1, 1}, {1, -1, 1}, {-1, 0, 1}, {1, 0, 1}, {-1, 1, 1}, {0, 1, 1}, {1, 1, 1}};

    private static final int[][] STARTING_POSITION_DIRECTIONAL_LIMITS = {{-1, -1, 1}, {0, -1, 1}, {1, -1, 1}, {-1, 0, 2}, {1, 0, 2}, {-1, 1, 1}, {0, 1, 1}, {1, 1, 1}};

    private static final Map<Integer, Integer> CASTLE_POSITION_MAPPINGS = Map.of(2, 0, 6, 7, 58, 56, 62, 63);

    public static final String AN_CODE = "K";

    public static int[][] getDirectionalLimits(int pieceBitFlag) {
        return isOnStartingPosition(pieceBitFlag) ? STARTING_POSITION_DIRECTIONAL_LIMITS : STANDARD_DIRECTIONAL_LIMITS;
    }

    public static List<PieceConfiguration> getPossibleMoves(int pieceBitFlag, int[] positionBitFlags, PieceConfiguration currentConfiguration) {
        List<PieceConfiguration> pieceConfigurations = new ArrayList<>();
        for(int[] directionalLimit : getMovableDirectionalLimits(pieceBitFlag, positionBitFlags)) {
            int directionX = directionalLimit[0];
            int directionY = directionalLimit[1];
            int limit = directionalLimit[2];
            int testPositionIndex = getPosition(pieceBitFlag);

            while (limit > 0) {
                testPositionIndex = Position.applyTranslation(testPositionIndex, directionX, directionY);
                if (!isValidPosition(testPositionIndex)) {
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
                if (directionalLimit[2] == 2 && limit == 1 && !isLegalCastle(pieceBitFlag, positionBitFlags, testPositionIndex)) {
                    break;
                }

                // Is there an opponent piece on the position?
                int takenPieceBitFlag = -1;
                if (BitUtil.hasBitFlag(positionBitFlags[testPositionIndex], PieceConfiguration.OPPONENT_OCCUPIED)) {
                    takenPieceBitFlag = currentConfiguration.getPieceAtPosition(testPositionIndex);
                }

                addNewPieceConfigurations(pieceBitFlag, pieceConfigurations, currentConfiguration, testPositionIndex, takenPieceBitFlag);

                if (takenPieceBitFlag >= 0) {
                    // Stop considering moves beyond this taken piece
                    break;
                }
                limit--;
            }
        }
        return pieceConfigurations;
    }

    protected static int[][] getMovableDirectionalLimits(int pieceBitFlag, int[] positionBitFlags) {
        int number = positionBitFlags[getPosition(pieceBitFlag)];
        if (hasDirectionalFlags(number)) {
            return restrictDirections(pieceBitFlag, ~getDirectionalFlags(number));
        }
        return getDirectionalLimits(pieceBitFlag);
    }

    private static boolean isLegalCastle(int pieceBitFlag, int[] positionBitFlags, int testPositionIndex) {
        if (!BitUtil.hasBitFlag(positionBitFlags[testPositionIndex], PieceConfiguration.CASTLE_AVAILABLE)
                || BitUtil.hasBitFlag(positionBitFlags[getPosition(pieceBitFlag)], PieceConfiguration.THREATENED) // No castling out of check
                || BitUtil.hasBitFlag(positionBitFlags[testPositionIndex], PieceConfiguration.OPPONENT_OCCUPIED) // No taking by castle
                    // Test if there is a piece in the B file when doing the long castle
                || (Position.getX(testPositionIndex) == 2
                        && (BitUtil.hasBitFlag(positionBitFlags[testPositionIndex - 1], PieceConfiguration.PLAYER_OCCUPIED)
                                || BitUtil.hasBitFlag(positionBitFlags[testPositionIndex - 1], PieceConfiguration.OPPONENT_OCCUPIED)))) {
            return false;
        }
        return true;
    }

    public static int[] stampThreatFlags(int pieceBitFlag, int[] positionBitFlags) {
        for(int[] directionalLimit : getDirectionalLimits(pieceBitFlag)) {
            // Limit is always 1 for the king (castling positions can't be threatened by the king)
            int testPositionIndex = Position.applyTranslation(Position.getPosition(pieceBitFlag),
                    directionalLimit[0], directionalLimit[1]);
            if (!isValidPosition(testPositionIndex)) {
                continue;
            }

            positionBitFlags[testPositionIndex] = BitUtil.applyBitFlag(positionBitFlags[testPositionIndex], PieceConfiguration.THREATENED);
        }
        return positionBitFlags;
    }

    private static boolean isOnStartingPosition(int pieceBitFlag) {
        return Position.getPosition(pieceBitFlag) == 4 + (getSide(pieceBitFlag) * 56);
    }

    protected static void addNewPieceConfigurations(int pieceBitFlag, List<PieceConfiguration> pieceConfigurations,
            PieceConfiguration currentConfiguration, int newPiecePosition, int takenPieceBitFlag) {
        Piece.addNewPieceConfigurations(pieceBitFlag, pieceConfigurations, currentConfiguration, newPiecePosition, takenPieceBitFlag);
        if (isOnStartingPosition(pieceBitFlag)) {
            PieceConfiguration newPieceConfiguration = pieceConfigurations.get(pieceConfigurations.size() - 1);
            newPieceConfiguration.removeCastlePosition(getPosition(pieceBitFlag) - 2);
            newPieceConfiguration.removeCastlePosition(getPosition(pieceBitFlag) + 2);
            if (CASTLE_POSITION_MAPPINGS.containsKey(newPiecePosition)) {
                final int oldRookPosition = CASTLE_POSITION_MAPPINGS.get(newPiecePosition);
                final int oldCastlingRookBitFlag = currentConfiguration.getPieceAtPosition(oldRookPosition);
                final int newRookPosition = (getPosition(pieceBitFlag) + newPiecePosition) >> 1;
                newPieceConfiguration.removePiece(oldCastlingRookBitFlag);
                final int newCastlingRookBitFlag = oldCastlingRookBitFlag - oldRookPosition + newRookPosition;
                newPieceConfiguration.addPiece(newCastlingRookBitFlag);
            }
        }
    }

    @Override
    public String getANCode() {
        return AN_CODE;
    }

    public static char getFENCode(int pieceBitFlag) {
        return (char) (75 + (getSide(pieceBitFlag) * 32));
    }
}
