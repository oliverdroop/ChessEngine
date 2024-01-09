package chess.api.pieces;

import chess.api.BitUtil;
import chess.api.PieceConfiguration;
import chess.api.Position;
import chess.api.Side;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;

import static chess.api.PieceConfiguration.*;

public abstract class Piece {

    private static final Logger LOGGER = LoggerFactory.getLogger(Piece.class);

    private static final int[] pieceValues = {
            0,King.getValue(),Knight.getValue(),0,Bishop.getValue(),0,0,0,
            Queen.getValue(),0,0,0,0,0,0,0,
            Rook.getValue(),0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,
            Pawn.getValue()};

    /**
     * @return An array of size-3 int arrays, where the first two ints correspond to a direction
     * and the third int corresponds to the maximum number of times the piece can move in that direction
     */
    public static int[][] getDirectionalLimits(int pieceBitFlag) {
        int pieceFlag = getPieceTypeBitFlag(pieceBitFlag);
        switch(pieceFlag) {
            case PAWN_OCCUPIED:
                return Pawn.getDirectionalLimits(pieceBitFlag);
            case BISHOP_OCCUPIED:
                return Bishop.getDirectionalLimits();
            case ROOK_OCCUPIED:
                return Rook.getDirectionalLimits();
            case QUEEN_OCCUPIED:
                return Queen.getDirectionalLimits();
            case KNIGHT_OCCUPIED:
                return Knight.getDirectionalLimits();
            case KING_OCCUPIED:
                return King.getDirectionalLimits(pieceBitFlag);
            default:
                return new int[0][];
        }
    }

    public static List<PieceConfiguration> getPossibleMoves(int pieceBitFlag, int[] positionBitFlags,
                                                            PieceConfiguration currentConfiguration,
                                                            boolean linkOnwardConfigurations) {
        int pieceFlag = getPieceTypeBitFlag(pieceBitFlag);
        switch(pieceFlag) {
            case PAWN_OCCUPIED:
                return Pawn.getPossibleMoves(pieceBitFlag, positionBitFlags, currentConfiguration, linkOnwardConfigurations);
            case KING_OCCUPIED:
                return King.getPossibleMoves(pieceBitFlag, positionBitFlags, currentConfiguration, linkOnwardConfigurations);
            default:
                break;
        }
        List<PieceConfiguration> pieceConfigurations = new ArrayList<>();
        for(int[] directionalLimit : getMovableDirectionalLimits(pieceBitFlag, positionBitFlags)) {
            int directionX = directionalLimit[0];
            int directionY = directionalLimit[1];
            int limit = directionalLimit[2];
            int testPositionIndex = Position.getPosition(pieceBitFlag);

            while (limit > 0) {
                testPositionIndex = Position.applyTranslation(testPositionIndex, directionX, directionY);
                if (testPositionIndex < 0 || testPositionIndex >= 64) {
                    break;
                }

                // Is this player piece blocked by another player piece?
                if (BitUtil.hasBitFlag(positionBitFlags[testPositionIndex], PieceConfiguration.PLAYER_OCCUPIED)) {
                    break;
                }

                // Is there an opponent piece on the position?
                int takenPieceBitFlag = -1;
                if (BitUtil.hasBitFlag(positionBitFlags[testPositionIndex], PieceConfiguration.OPPONENT_OCCUPIED)) {
                    takenPieceBitFlag = positionBitFlags[testPositionIndex];
                }

                // Is this position a position which wouldn't block an existing checking direction?
                if (BitUtil.hasBitFlag(positionBitFlags[testPositionIndex], PieceConfiguration.DOES_NOT_BLOCK_CHECK)) {
                    if (takenPieceBitFlag == -1) {
                        limit--;
                        continue;
                    } else {
                        break;
                    }
                }

                addNewPieceConfigurations(pieceBitFlag, pieceConfigurations, currentConfiguration, testPositionIndex,
                        takenPieceBitFlag, linkOnwardConfigurations);

                if (takenPieceBitFlag >= 0) {
                    // Stop considering moves beyond this taken piece
                    break;
                }
                limit--;
            }
        }
        return pieceConfigurations;
    }

    protected static void addNewPieceConfigurations(int pieceBitFlag, List<PieceConfiguration> pieceConfigurations,
                                                    PieceConfiguration currentConfiguration, int newPiecePosition,
                                                    int takenPieceBitFlag, boolean linkOnwardConfigurations) {
        PieceConfiguration newConfiguration = new PieceConfiguration(currentConfiguration, false);
        for(int pieceBitFlag2 : currentConfiguration.getPieceBitFlags()) {
            if (pieceBitFlag2 != pieceBitFlag && pieceBitFlag2 != takenPieceBitFlag) {
                newConfiguration.addPiece(pieceBitFlag2);
            }
        }
        try {
            int movedPieceBitFlag = (pieceBitFlag ^ getPosition(pieceBitFlag)) | newPiecePosition;
            newConfiguration.addPiece(movedPieceBitFlag);
        } catch (Exception e) {
            LOGGER.error("Problem creating new piece configuration");
            return;
        }
        newConfiguration.setTurnSide(currentConfiguration.getTurnSide().getOpposingSide());

        if (takenPieceBitFlag >= 0 || BitUtil.hasBitFlag(pieceBitFlag, PieceConfiguration.PAWN_OCCUPIED)) {
            newConfiguration.setHalfMoveClock(0);
        } else {
            newConfiguration.setHalfMoveClock(currentConfiguration.getHalfMoveClock() + 1);
        }

        if (BitUtil.hasBitFlag(pieceBitFlag, PieceConfiguration.BLACK_OCCUPIED)) {
            newConfiguration.setFullMoveNumber(currentConfiguration.getFullMoveNumber() + 1);
        }
        pieceConfigurations.add(newConfiguration);

        if (linkOnwardConfigurations) {
            linkPieceConfigurations(pieceBitFlag, currentConfiguration, newConfiguration, newPiecePosition, takenPieceBitFlag);
        }

        // Remove castling options when rook moves
        if (BitUtil.hasBitFlag(pieceBitFlag, ROOK_OCCUPIED)) {
            Rook.removeCastlingOptions(pieceBitFlag, pieceConfigurations);
        }
    }

    protected static void linkPieceConfigurations(int pieceBitFlag, PieceConfiguration currentConfiguration, PieceConfiguration newConfiguration,
                                           int newPiecePosition, int takenPieceBitFlag) {
        currentConfiguration.addChildConfiguration(newConfiguration);
        newConfiguration.setParentConfiguration(currentConfiguration);
        newConfiguration.setAlgebraicNotation(getAlgebraicNotation(
                getPosition(pieceBitFlag), newPiecePosition, takenPieceBitFlag >= 0, null));
    }

    public static String getAlgebraicNotation(int currentPosition, int nextPosition, boolean capturing, String promotionTo) {
        StringBuilder sb = new StringBuilder()
//                .append(getANCode())
                .append(Position.getCoordinateString(currentPosition));
        if (capturing) {
            sb.append('x');
        }
        sb.append(Position.getCoordinateString(nextPosition));
        if (promotionTo != null) {
            sb.append(promotionTo);
        }
        return sb.toString();
    }

    public abstract String getANCode();

    public static int[] stampThreatFlags(int pieceBitFlag, int[] positionBitFlags) {
        int pieceTypeFlag = getPieceTypeBitFlag(pieceBitFlag);
        switch(pieceTypeFlag) {
            case PieceConfiguration.KING_OCCUPIED:
                return King.stampThreatFlags(pieceBitFlag, positionBitFlags);
            case PieceConfiguration.PAWN_OCCUPIED:
                return Pawn.stampThreatFlags(pieceBitFlag, positionBitFlags);
            case PieceConfiguration.KNIGHT_OCCUPIED:
            case PieceConfiguration.BISHOP_OCCUPIED:
            case PieceConfiguration.ROOK_OCCUPIED:
            case PieceConfiguration.QUEEN_OCCUPIED:
            default:
                return stampSimpleThreatFlags(pieceBitFlag, positionBitFlags);
        }
    }

    public static int[] stampSimpleThreatFlags(int pieceBitFlag, int[] positionBitFlags) {
        for(int[] directionalLimit : getDirectionalLimits(pieceBitFlag)) {
            int directionX = directionalLimit[0];
            int directionY = directionalLimit[1];
            int limit = directionalLimit[2];
            int testPositionIndex = getPosition(pieceBitFlag);
            int potentialKingProtectorPosition = -1;
            while (limit > 0) {
                testPositionIndex = Position.applyTranslation(testPositionIndex, directionX, directionY);
                if (testPositionIndex < 0 || testPositionIndex >= 64) {
                    break;
                }

                if (potentialKingProtectorPosition < 0) {
                    // Opponent piece threatens the position
                    positionBitFlags[testPositionIndex] = BitUtil.applyBitFlag(positionBitFlags[testPositionIndex], PieceConfiguration.THREATENED);
                    // Is this opponent piece blocked by another opponent piece?
                    if (BitUtil.hasBitFlag(positionBitFlags[testPositionIndex], PieceConfiguration.OPPONENT_OCCUPIED)) {
                        break;
                    }
                }

                // Is there a player piece on the position?
                if (BitUtil.hasBitFlag(positionBitFlags[testPositionIndex], PieceConfiguration.PLAYER_OCCUPIED)) {
                    boolean kingOccupied = BitUtil.hasBitFlag(positionBitFlags[testPositionIndex], PieceConfiguration.KING_OCCUPIED);
                    if (potentialKingProtectorPosition < 0) {
                        if (kingOccupied) {
                            // First player piece encountered in this direction is the player's king
                            // Player's king can't be a king protector
                            positionBitFlags[testPositionIndex] = setDirectionalFlags(
                                    positionBitFlags[testPositionIndex], directionX, directionY);
                        } else {
                            // First player piece encountered in this direction is not the player's king
                            potentialKingProtectorPosition = testPositionIndex;
                        }
                    } else if (kingOccupied) {
                        // Second player piece encountered in this direction is the player's king
                        positionBitFlags[potentialKingProtectorPosition] = setDirectionalFlags(
                                positionBitFlags[potentialKingProtectorPosition], directionX, directionY);
                        break;
                    } else {
                        // Second player piece encountered in this direction is not the player's king.
                        break;
                    }
                }
                limit--;
            }
        }
        return positionBitFlags;
    }

    protected static int setDirectionalFlags(int number, int x, int y) {
        if (Math.abs(x) == 2 || Math.abs(y) == 2) {
            return number | PieceConfiguration.DIRECTION_ANY_KNIGHT;
        }
        return number | Position.DIRECTIONAL_BIT_FLAG_GRID[y + 1][x + 1];
    }

    public static int getDirectionalFlags(int number) {
        return number & ALL_DIRECTIONAL_FLAGS_COMBINED;
    }

    protected static boolean hasDirectionalFlags(int number) {
        return getDirectionalFlags(number) != 0;
    }

    protected static int[][] restrictDirections(int pieceBitFlag, int directionalBitFlags) {
        return restrictDirections(getDirectionalLimits(pieceBitFlag), directionalBitFlags);
    }

    protected static int[][] restrictDirections(int[][] directionalLimits, int directionalBitFlags) {
        switch(directionalBitFlags) {
            case PieceConfiguration.DIRECTION_N:
            case PieceConfiguration.DIRECTION_S:
                return filterDirectionalLimitsByPredicate(directionalLimits, dl -> dl[0] == 0);
            case PieceConfiguration.DIRECTION_NE:
            case PieceConfiguration.DIRECTION_SW:
                return filterDirectionalLimitsByPredicate(directionalLimits, dl -> dl[0] == dl[1]);
            case PieceConfiguration.DIRECTION_E:
            case PieceConfiguration.DIRECTION_W:
                return filterDirectionalLimitsByPredicate(directionalLimits, dl -> dl[1] == 0);
            case PieceConfiguration.DIRECTION_SE:
            case PieceConfiguration.DIRECTION_NW:
                return filterDirectionalLimitsByPredicate(directionalLimits, dl -> dl[0] == -dl[1]);
            case PieceConfiguration.DIRECTION_ANY_KNIGHT:
            default:
                return directionalLimits;
        }
    }

    private static int[][] filterDirectionalLimitsByPredicate(int[][] directionalLimits, Predicate<int[]> predicate) {
        return Arrays.stream(directionalLimits)
                .filter(predicate)
                .toArray(size1 -> new int[size1][]);
    }

    protected static int[][] getMovableDirectionalLimits(int pieceBitFlag, int[] positionBitFlags) {
        int position = getPosition(pieceBitFlag);
        int number = positionBitFlags[position];
        if (hasDirectionalFlags(number)) {
            return restrictDirections(pieceBitFlag, getDirectionalFlags(number));
        }
        return getDirectionalLimits(pieceBitFlag);
    }

    protected static int[][] arrayDeepCopy(int[][] arrayToCopy, int startIndex, int endIndex) {
        int[][] output = new int[endIndex - startIndex][3];
        int io = 0;
        for(int i = startIndex; i < endIndex; i++) {
            output[io] = arrayToCopy[i].clone();
            io++;
        }
        return output;
    }

    public static int getPosition(int pieceBitFlag) {
        return Position.getPosition(pieceBitFlag);
    }

    public static PieceType getPieceType(int pieceBitFlag) {
        int pieceTypeFlag = getPieceTypeBitFlag(pieceBitFlag);
        switch (pieceTypeFlag) {
            case KING_OCCUPIED:
                return PieceType.KING;
            case KNIGHT_OCCUPIED:
                return PieceType.KNIGHT;
            case BISHOP_OCCUPIED:
                return PieceType.BISHOP;
            case ROOK_OCCUPIED:
                return PieceType.ROOK;
            case QUEEN_OCCUPIED:
                return PieceType.QUEEN;
            case PAWN_OCCUPIED:
                return PieceType.PAWN;
            default:
                throw new RuntimeException("No piece type recognised from which to get PieceType enum");
        }
    }

    public static Side getSide(int pieceBitFlag) {
        return Side.values()[(pieceBitFlag & PieceConfiguration.BLACK_OCCUPIED) >> 9];
    }

    public String toString(int pieceBitFlag) {
        return new StringBuilder()
                .append(getPieceType(pieceBitFlag).toString())
                .append(':')
                .append(Position.getCoordinateString(getPosition(pieceBitFlag)))
                .toString();
    }

    public static char getFENCode(int pieceBitFlag) {
        int pieceTypeFlag = getPieceTypeBitFlag(pieceBitFlag);
        switch (pieceTypeFlag) {
            case KING_OCCUPIED:
                return King.getFENCode(pieceBitFlag);
            case KNIGHT_OCCUPIED:
                return Knight.getFENCode(pieceBitFlag);
            case BISHOP_OCCUPIED:
                return Bishop.getFENCode(pieceBitFlag);
            case ROOK_OCCUPIED:
                return Rook.getFENCode(pieceBitFlag);
            case QUEEN_OCCUPIED:
                return Queen.getFENCode(pieceBitFlag);
            case PAWN_OCCUPIED:
                return Pawn.getFENCode(pieceBitFlag);
            default:
                throw new RuntimeException("No piece type recognised from which to get FEN code");
        }
    }

    public static int getValue(int pieceBitFlag) {
        return pieceValues[getPieceTypeBitFlag(pieceBitFlag) >> 10];
//        int pieceTypeFlag = getPieceTypeBitFlag(pieceBitFlag);
//        switch (pieceTypeFlag) {
//            case KING_OCCUPIED:
//                return King.getValue();
//            case KNIGHT_OCCUPIED:
//                return Knight.getValue();
//            case BISHOP_OCCUPIED:
//                return Bishop.getValue();
//            case ROOK_OCCUPIED:
//                return Rook.getValue();
//            case QUEEN_OCCUPIED:
//                return Queen.getValue();
//            case PAWN_OCCUPIED:
//                return Pawn.getValue();
//            default:
//                return 0;
//        }
    }
}
