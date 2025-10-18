package chess.api.pieces;

import chess.api.configuration.PieceConfiguration;
import chess.api.Position;
import org.apache.logging.log4j.util.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static chess.api.MoveDescriber.describeMove;
import static chess.api.configuration.PieceConfiguration.*;
import static chess.api.Position.isValidPosition;

public class Pawn extends Piece{

    private static final int[][][] DIRECTIONAL_LIMITS = {
                                                //  WHITE
                                                //      Non-starting position
        {{0, 1, 1}},                            //          Forward only
        {{-1, 1, 1}, {0, 1, 1}},                //          Forward and left diagonal
        {{0, 1, 1}, {1, 1, 1}},                 //          Forward and right diagonal
        {{-1, 1, 1}, {0, 1, 1}, {1, 1, 1}},     //          Forward and both diagonals
                                                //      Starting position
        {{0, 1, 2}},                            //          Forward only
        {{-1, 1, 1}, {0, 1, 2}},                //          Forward and left diagonal
        {{0, 1, 2}, {1, 1, 1}},                 //          Forward and right diagonal
        {{-1, 1, 1}, {0, 1, 2}, {1, 1, 1}},     //          Forward and both diagonals
                                                //  BLACK
                                                //      Non-starting position
        {{0, -1, 1}},                           //          Forward only
        {{0, -1, 1}, {1, -1, 1}},               //          Forward and left diagonal
        {{-1, -1, 1}, {0, -1, 1}},              //          Forward and right diagonal
        {{1, -1, 1}, {0, -1, 1}, {-1, -1, 1}},  //          Forward and both diagonals
                                                //      Starting position
        {{0, -1, 2}},                           //          Forward only
        {{0, -1, 2}, {1, -1, 1}},               //          Forward and left diagonal
        {{-1, -1, 1}, {0, -1, 2}},              //          Forward and right diagonal
        {{1, -1, 1}, {0, -1, 2}, {-1, -1, 1}}   //          Forward and both diagonals
    };

    public static final Map<Integer, String> PROMOTION_PIECE_TYPES = Map.of(KNIGHT_OCCUPIED, "N", BISHOP_OCCUPIED, "B", ROOK_OCCUPIED, "R", QUEEN_OCCUPIED, "Q");

    public static int[][] getUnrestrictedDirectionalLimits(int pieceBitFlag) {
        final int index = 7 | (getSide(pieceBitFlag) << 3);
        return DIRECTIONAL_LIMITS[index];
    }

    public static List<PieceConfiguration> getPossibleMoves(int pieceBitFlag, PieceConfiguration currentConfiguration) {
        List<PieceConfiguration> pieceConfigurations = new ArrayList<>();
        final int turnSide = currentConfiguration.getTurnSide();
        for(int[] directionalLimit : getMovableDirectionalLimits(pieceBitFlag, currentConfiguration, turnSide)) {
            int directionX = directionalLimit[0];
            int directionY = directionalLimit[1];
            int limit = directionalLimit[2];
            int testPositionIndex = getPosition(pieceBitFlag);

            while (limit > 0) {
                testPositionIndex = Position.applyTranslation(testPositionIndex, directionX, directionY);
                if (!isValidPosition(testPositionIndex)) {
                    break;
                }

                // Is this player piece blocked by another player piece?
                if (currentConfiguration.isPlayerOccupied(testPositionIndex)) {
                    break;
                }

                // Is there an opponent piece on the position?
                int takenPieceBitFlag = -1;
                if (currentConfiguration.isOpponentOccupiedOrEnPassantSquare(testPositionIndex)) {
                    if (directionX != 0) {
                        // This is a diagonal move so taking a piece is valid
                        takenPieceBitFlag = currentConfiguration.getPieceAtPosition(testPositionIndex);
                        if ((takenPieceBitFlag & ALL_PIECE_FLAGS_COMBINED) == 0) {
                            // This is an en-passant move
                            takenPieceBitFlag = currentConfiguration.getPieceAtPosition(testPositionIndex - 8 + (16 * currentConfiguration.getTurnSide()));
                        }
                    } else {
                        // This is a straight forward move so taking a piece is not possible
                        break;
                    }
                }

                // Is this position a position which wouldn't block an existing checking direction?
                if (currentConfiguration.isIneffectiveCheckBlockAttempt(testPositionIndex)) {
                    limit--;
                    continue;
                }

                addNewPieceConfigurations(pieceBitFlag, pieceConfigurations, currentConfiguration, testPositionIndex);

                if (takenPieceBitFlag >= 0) {
                    // Stop considering moves beyond this taken piece
                    break;
                }
                limit--;
            }
        }
        return pieceConfigurations;
    }

    public static void stampThreatFlags(int pieceBitFlag, PieceConfiguration pieceConfiguration) {
        final int[] directionalLimitThreatIndexes = {0, 2}; // Pawns can only threaten diagonally
        for(int i : directionalLimitThreatIndexes) {
            final int[] directionalLimit = getUnrestrictedDirectionalLimits(pieceBitFlag)[i];
            final int directionX = directionalLimit[0];
            final int directionY = directionalLimit[1];
            int testPositionIndex = getPosition(pieceBitFlag);
            testPositionIndex = Position.applyTranslation(testPositionIndex, directionX, directionY);
            if (!isValidPosition(testPositionIndex)) {
                continue;
            }

            pieceConfiguration.setThreatened(testPositionIndex);

            if (pieceConfiguration.isPlayerKingOccupied(testPositionIndex)) {
                // Player piece encountered in this direction is the player's king
                final int directionalFlag = getDirectionalFlag(directionX, directionY);
                pieceConfiguration.setDirectionalFlag(testPositionIndex, directionalFlag);
            }
        }
    }

    protected static int[][] getMovableDirectionalLimits(int pieceBitFlag, PieceConfiguration currentConfiguration, int turnSide) {
        final int isOnStartingPosition = Boolean.compare(isOnStartingRank(pieceBitFlag), false);
        final int leftDiagonalAvailable = isDiagonalMoveAvailable(pieceBitFlag, 0, currentConfiguration);
        final int rightDiagonalAvailable = isDiagonalMoveAvailable(pieceBitFlag, 2, currentConfiguration);
        final int availability = (turnSide << 3)
            | (isOnStartingPosition << 2)
            | (rightDiagonalAvailable << 1)
            | leftDiagonalAvailable;
        final int[][] moveableDirectionalLimits = DIRECTIONAL_LIMITS[availability];

        int directionalBitFlags = getDirectionalFlags(currentConfiguration.getPieceAtPosition(getPosition(pieceBitFlag)));
        if (directionalBitFlags != 0) {
            return restrictDirections(moveableDirectionalLimits, directionalBitFlags);
        }
        return moveableDirectionalLimits;
    }

    private static void addNewPieceConfigurations(int pieceBitFlag, List<PieceConfiguration> pieceConfigurations,
            PieceConfiguration currentConfiguration, int newPiecePosition) {
        final int newY = Position.getY(newPiecePosition);
        short move = describeMove(pieceBitFlag & 63, newPiecePosition, 0);
        if (newY == 7 | newY == 0) {
            for(int i = KNIGHT_OCCUPIED; i <= QUEEN_OCCUPIED; i = i << 1) {
                move = describeMove(pieceBitFlag & 63, newPiecePosition, i);
                pieceConfigurations.add(toNewConfigurationFromMove(currentConfiguration, move));
            }
        } else {
            pieceConfigurations.add(toNewConfigurationFromMove(currentConfiguration, move));
        }
    }

    private static boolean isOnStartingRank(int pieceBitFlag) {
        return Position.getY(getPosition(pieceBitFlag)) - (getSide(pieceBitFlag) * 5) == 1;
    }

    private static int isDiagonalMoveAvailable(int pieceBitFlag, int directionalLimitIndex, PieceConfiguration currentConfiguration) {
        int[][] directionalLimits = getUnrestrictedDirectionalLimits(pieceBitFlag);
        int testPosition = Position.applyTranslation(getPosition(pieceBitFlag),
                directionalLimits[directionalLimitIndex][0], directionalLimits[directionalLimitIndex][1]);
        if (testPosition < 0) {
            return 0;
        }
        return Boolean.compare(currentConfiguration.isOpponentOccupiedOrEnPassantSquare(testPosition), false);
    }

    public static char getFENCode(int pieceBitFlag) {
        return (char) (80 + (getSide(pieceBitFlag) * 32));
    }

    @Override
    public String getANCode() {
        return Strings.EMPTY;
    }
}
