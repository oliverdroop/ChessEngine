package chess.api.pieces;

import chess.api.BitUtil;
import chess.api.PieceConfiguration;
import chess.api.Position;
import com.google.common.collect.ImmutableMap;
import org.apache.logging.log4j.util.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static chess.api.PieceConfiguration.*;
import static chess.api.Position.isValidPosition;

public class Pawn extends Piece{

    private static final int[][] WHITE_DIRECTIONAL_LIMITS = {{-1, 1, 1}, {0, 1, 2}, {1, 1, 1}};

    private static final int[][] BLACK_DIRECTIONAL_LIMITS = {{1, -1, 1}, {0, -1, 2}, {-1, -1, 1}};

    private static final Map<Integer, String> PROMOTION_PIECE_TYPES = ImmutableMap.of(KNIGHT_OCCUPIED, "N", BISHOP_OCCUPIED, "B", ROOK_OCCUPIED, "R", QUEEN_OCCUPIED, "Q");

    public static int[][] getDirectionalLimits(int pieceBitFlag) {
        return getSide(pieceBitFlag) == 0 ? WHITE_DIRECTIONAL_LIMITS : BLACK_DIRECTIONAL_LIMITS;
    }

    public static List<PieceConfiguration> getPossibleMoves(int pieceBitFlag, int[] positionBitFlags, PieceConfiguration currentConfiguration, boolean linkOnwardConfigurations) {
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

                // Is this player piece blocked by another player piece?
                if (BitUtil.hasBitFlag(positionBitFlags[testPositionIndex], PLAYER_OCCUPIED)) {
                    break;
                }

                // Is this position a position which wouldn't block an existing checking direction?
                if (BitUtil.hasBitFlag(positionBitFlags[testPositionIndex], DOES_NOT_BLOCK_CHECK)) {
                    limit--;
                    continue;
                }

                // Is there an opponent piece on the position?
                int takenPieceBitFlag = -1;
                if (BitUtil.hasBitFlag(positionBitFlags[testPositionIndex], OPPONENT_OCCUPIED)) {
                    if (directionX != 0) {
                        // This is a diagonal move so taking a piece is valid
                        takenPieceBitFlag = currentConfiguration.getPieceAtPosition(testPositionIndex);
                    } else {
                        // This is a straight forward move so taking a piece is not possible
                        break;
                    }
                }

                addNewPieceConfigurations(pieceBitFlag, pieceConfigurations, currentConfiguration, testPositionIndex, takenPieceBitFlag, linkOnwardConfigurations);

                if (takenPieceBitFlag >= 0) {
                    // Stop considering moves beyond this taken piece
                    break;
                }
                limit--;
            }
        }
        return pieceConfigurations;
    }

    public static int[] stampThreatFlags(int pieceBitFlag, int[] positionBitFlags) {
        int[] directionalLimitThreatIndexes = {0, 2};
        for(int i : directionalLimitThreatIndexes) {
            int[] directionalLimit = getDirectionalLimits(pieceBitFlag)[i];
            int directionX = directionalLimit[0];
            int directionY = directionalLimit[1];
            int testPositionIndex = getPosition(pieceBitFlag);
            testPositionIndex = Position.applyTranslation(testPositionIndex, directionX, directionY);
            if (!isValidPosition(testPositionIndex)) {
                continue;
            }

            positionBitFlags[testPositionIndex] = BitUtil.applyBitFlag(positionBitFlags[testPositionIndex], THREATENED);

            if (BitUtil.hasBitFlag(positionBitFlags[testPositionIndex], PLAYER_KING_OCCUPIED)) {
                // Player piece encountered in this direction is the player's king
                positionBitFlags[testPositionIndex] = setDirectionalFlags(
                        positionBitFlags[testPositionIndex], directionX, directionY);
            }
        }
        return positionBitFlags;
    }

    protected static int[][] getMovableDirectionalLimits(int pieceBitFlag, int[] positionBitFlags) {
        boolean leftDiagonalAvailable = isDiagonalMoveAvailable(pieceBitFlag, 0, positionBitFlags);
        boolean rightDiagonalAvailable = isDiagonalMoveAvailable(pieceBitFlag, 2, positionBitFlags);
        int startIndex = leftDiagonalAvailable ? 0 : 1;
        int endIndex = rightDiagonalAvailable ? 3 : 2;
        int[][] movableDirectionalLimits = arrayDeepCopy(getDirectionalLimits(pieceBitFlag), startIndex, endIndex);
        if (!isOnStartingRank(pieceBitFlag)) {
            movableDirectionalLimits[1 - startIndex][2] = 1;
        }

        int directionalBitFlags = getDirectionalFlags(positionBitFlags[getPosition(pieceBitFlag)]);
        if (directionalBitFlags != 0) {
            return restrictDirections(movableDirectionalLimits, directionalBitFlags);
        }
        return movableDirectionalLimits;
    }

    protected static void addNewPieceConfigurations(int pieceBitFlag, List<PieceConfiguration> pieceConfigurations,
            PieceConfiguration currentConfiguration, int newPiecePosition, int takenPieceBitFlag, boolean linkOnwardConfigurations) {
        Piece.addNewPieceConfigurations(pieceBitFlag, pieceConfigurations, currentConfiguration, newPiecePosition, takenPieceBitFlag, linkOnwardConfigurations);
        int translation = newPiecePosition - getPosition(pieceBitFlag);
        PieceConfiguration newPieceConfiguration = pieceConfigurations.get(pieceConfigurations.size() - 1);
        if (Math.abs(translation) == 16) {
            // Set the en passant square
            newPieceConfiguration.setEnPassantSquare(getPosition(pieceBitFlag) + (translation / 2));
        } else if (Position.getY(newPiecePosition) == 7 - (getSide(pieceBitFlag) * 7)) {
            // Promote pawn
            pieceConfigurations.remove(pieceConfigurations.size() - 1);
            currentConfiguration.getChildConfigurations().remove(newPieceConfiguration);
            for(int promotionPieceTypeFlag : PROMOTION_PIECE_TYPES.keySet()) {
                PieceConfiguration promotedPawnConfiguration = getPromotedPawnConfiguration(newPieceConfiguration,
                        newPiecePosition, promotionPieceTypeFlag);
                if (linkOnwardConfigurations) {
                    linkPromotedPieceConfigurations(pieceBitFlag, currentConfiguration, promotedPawnConfiguration, newPiecePosition,
                            takenPieceBitFlag, PROMOTION_PIECE_TYPES.get(promotionPieceTypeFlag).toLowerCase());
                }
                pieceConfigurations.add(promotedPawnConfiguration);
            }
        }
    }

    protected static void linkPromotedPieceConfigurations(int pieceBitFlag, PieceConfiguration currentConfiguration, PieceConfiguration newConfiguration,
                                           int newPiecePosition, int takenPieceBitFlag, String promotionTo) {
        currentConfiguration.addChildConfiguration(newConfiguration);
        newConfiguration.setParentConfiguration(currentConfiguration);
        newConfiguration.setAlgebraicNotation(getAlgebraicNotation(getPosition(pieceBitFlag), newPiecePosition, takenPieceBitFlag >= 0, promotionTo));
    }

    private static PieceConfiguration getPromotedPawnConfiguration(PieceConfiguration unpromotedPawnConfiguration,
            int newPiecePosition, int promotedPieceTypeFlag) {
        PieceConfiguration newPieceConfiguration = new PieceConfiguration(unpromotedPawnConfiguration, true);
        newPieceConfiguration.promotePiece(newPiecePosition, promotedPieceTypeFlag);
        return newPieceConfiguration;
    }

    private static boolean isOnStartingRank(int pieceBitFlag) {
        return Position.getY(getPosition(pieceBitFlag)) - (getSide(pieceBitFlag) * 5) == 1;
    }

    private static boolean isDiagonalMoveAvailable(int pieceBitFlag, int directionalLimitIndex, int[] positionBitFlags) {
        int[][] directionalLimits = getDirectionalLimits(pieceBitFlag);
        int testPosition = Position.applyTranslation(getPosition(pieceBitFlag),
                directionalLimits[directionalLimitIndex][0], directionalLimits[directionalLimitIndex][1]);
        if (testPosition < 0) {
            return false;
        }
        return BitUtil.hasBitFlag(positionBitFlags[testPosition], OPPONENT_OCCUPIED)
                || BitUtil.hasBitFlag(positionBitFlags[testPosition], EN_PASSANT_SQUARE);
    }

    public static char getFENCode(int pieceBitFlag) {
        return (char) (80 + (getSide(pieceBitFlag) * 32));
    }

    @Override
    public String getANCode() {
        return Strings.EMPTY;
    }

    public static int getValue() {
        return 1;
    }
}
