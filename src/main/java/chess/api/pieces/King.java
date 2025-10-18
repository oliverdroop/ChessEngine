package chess.api.pieces;

import chess.api.configuration.PieceConfiguration;
import chess.api.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static chess.api.MoveDescriber.describeMove;
import static chess.api.configuration.PieceConfiguration.toNewConfigurationFromMove;
import static chess.api.Position.isValidPosition;

public class King extends Piece{

    private static final int[][][] DIRECTIONAL_LIMITS = {
        {{-1, -1, 1}, {0, -1, 1}, {1, -1, 1}, {-1, 0, 1}, {1, 0, 1}, {-1, 1, 1}, {0, 1, 1}, {1, 1, 1}},
        {{-1, -1, 1}, {0, -1, 1}, {1, -1, 1}, {-1, 0, 2}, {1, 0, 2}, {-1, 1, 1}, {0, 1, 1}, {1, 1, 1}}  // Starting position
    };

    public static final Map<Integer, Integer> CASTLE_POSITION_MAPPINGS = Map.of(2, 0, 6, 7, 58, 56, 62, 63);

    public static final String AN_CODE = "K";

    public static int[][] getDirectionalLimits(int pieceBitFlag) {
        final int isOnStartingPosition = Boolean.compare(isOnStartingPosition(pieceBitFlag), false);
        return DIRECTIONAL_LIMITS[isOnStartingPosition];
    }

    public static List<PieceConfiguration> getPossibleMoves(int pieceBitFlag, PieceConfiguration currentConfiguration) {
        List<PieceConfiguration> pieceConfigurations = new ArrayList<>();
        for(int[] directionalLimit : getMovableDirectionalLimits(pieceBitFlag, currentConfiguration)) {
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
                if (currentConfiguration.isThreatened(testPositionIndex)) {
                    break;
                }

                // Is this player piece blocked by another player piece?
                if (currentConfiguration.isPlayerOccupied(testPositionIndex)) {
                    break;
                }

                // Is this an attempted castling?
                if (directionalLimit[2] == 2 && limit == 1 && !isLegalCastle(pieceBitFlag, currentConfiguration, testPositionIndex)) {
                    break;
                }

                // Is there an opponent piece on the position?
                int takenPieceBitFlag = -1;
                if (currentConfiguration.isOpponentOccupied(testPositionIndex)) {
                    takenPieceBitFlag = currentConfiguration.getPieceAtPosition(testPositionIndex);
                }

                final short move = describeMove(pieceBitFlag & 63, testPositionIndex, 0);
                pieceConfigurations.add(toNewConfigurationFromMove(currentConfiguration, move));

                if (takenPieceBitFlag >= 0) {
                    // Stop considering moves beyond this taken piece
                    break;
                }
                limit--;
            }
        }
        return pieceConfigurations;
    }

    protected static int[][] getMovableDirectionalLimits(int pieceBitFlag, PieceConfiguration currentconfiguration) {
        int number = currentconfiguration.getPieceAtPosition(getPosition(pieceBitFlag));
        if (hasDirectionalFlags(number)) {
            return restrictDirections(pieceBitFlag, ~getDirectionalFlags(number));
        }
        return getDirectionalLimits(pieceBitFlag);
    }

    private static boolean isLegalCastle(int pieceBitFlag, PieceConfiguration currentConfiguration, int testPositionIndex) {
        return currentConfiguration.isCastleAvailable(testPositionIndex)
            && !currentConfiguration.isThreatened(getPosition(pieceBitFlag)) // No castling out of check
            && !currentConfiguration.isOpponentOccupied(testPositionIndex) // No taking by castle
            // Test if there is a piece in the B file when doing the long castle
            && (Position.getX(testPositionIndex) != 2
            || (!currentConfiguration.isPlayerOccupied(testPositionIndex - 1)
            && !currentConfiguration.isOpponentOccupied(testPositionIndex - 1)));
    }

    public static void stampThreatFlags(int pieceBitFlag, PieceConfiguration pieceConfiguration) {
        for(int[] directionalLimit : getDirectionalLimits(pieceBitFlag)) {
            // Limit is always 1 for the king (castling positions can't be threatened by the king)
            int testPositionIndex = Position.applyTranslation(Position.getPosition(pieceBitFlag),
                    directionalLimit[0], directionalLimit[1]);
            if (!isValidPosition(testPositionIndex)) {
                continue;
            }

            pieceConfiguration.setThreatened(testPositionIndex);
        }
    }

    private static boolean isOnStartingPosition(int pieceBitFlag) {
        return Position.getPosition(pieceBitFlag) == 4 + (getSide(pieceBitFlag) * 56);
    }

    @Override
    public String getANCode() {
        return AN_CODE;
    }

    public static char getFENCode(int pieceBitFlag) {
        return (char) (75 + (getSide(pieceBitFlag) * 32));
    }
}
