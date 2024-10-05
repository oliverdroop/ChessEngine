package chess.api.pieces;

import chess.api.BitUtil;
import chess.api.PieceConfiguration;
import chess.api.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;

import static chess.api.PieceConfiguration.*;
import static chess.api.Position.isValidPosition;

public abstract class Piece {

    private static final Logger LOGGER = LoggerFactory.getLogger(Piece.class);

    private static final int[] FAST_VALUE_ARRAY = {0, 3, 3, 0, 5, 0, 0, 0, 9, 0, 0, 0, 0, 0, 0, 0, 1};

    /**
     * @return An array of size-3 int arrays, where the first two ints correspond to a direction
     * and the third int corresponds to the maximum number of times the piece can move in that direction
     */
    public static int[][] getDirectionalLimits(int pieceBitFlag) {
        int pieceFlag = getPieceTypeBitFlag(pieceBitFlag);
        return switch (pieceFlag) {
            case PAWN_OCCUPIED -> Pawn.getDirectionalLimits(pieceBitFlag);
            case BISHOP_OCCUPIED -> Bishop.getDirectionalLimits();
            case ROOK_OCCUPIED -> Rook.getDirectionalLimits();
            case QUEEN_OCCUPIED -> Queen.getDirectionalLimits();
            case KNIGHT_OCCUPIED -> Knight.getDirectionalLimits();
            case KING_OCCUPIED -> King.getDirectionalLimits(pieceBitFlag);
            default -> new int[0][];
        };
    }

    public static List<PieceConfiguration> getPossibleMoves(int pieceBitFlag, int[] positionBitFlags,
                                                            PieceConfiguration currentConfiguration) {
        int pieceFlag = getPieceTypeBitFlag(pieceBitFlag);
        switch(pieceFlag) {
            case PAWN_OCCUPIED:
                return Pawn.getPossibleMoves(pieceBitFlag, positionBitFlags, currentConfiguration);
            case KING_OCCUPIED:
                return King.getPossibleMoves(pieceBitFlag, positionBitFlags, currentConfiguration);
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
                if (!isValidPosition(testPositionIndex)) {
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

                addNewPieceConfigurations(
                        pieceBitFlag, pieceConfigurations, currentConfiguration, testPositionIndex, takenPieceBitFlag);

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
                                                    int takenPieceBitFlag) {
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
        newConfiguration.setTurnSide(currentConfiguration.getOpposingSide());
        newConfiguration.setEnPassantSquare(-1);

        if (takenPieceBitFlag >= 0 || BitUtil.hasBitFlag(pieceBitFlag, PieceConfiguration.PAWN_OCCUPIED)) {
            newConfiguration.setHalfMoveClock(0);
        } else {
            newConfiguration.setHalfMoveClock(currentConfiguration.getHalfMoveClock() + 1);
        }

        if (BitUtil.hasBitFlag(pieceBitFlag, PieceConfiguration.BLACK_OCCUPIED)) {
            newConfiguration.setFullMoveNumber(currentConfiguration.getFullMoveNumber() + 1);
        }
        pieceConfigurations.add(newConfiguration);

        // Remove castling options when rook moves
        if (BitUtil.hasBitFlag(pieceBitFlag, ROOK_OCCUPIED)) {
            Rook.removeCastlingOptions(pieceBitFlag, newConfiguration);
        }
    }

    public static String getAlgebraicNotation(
            int parentPosition, int childPosition, boolean capturing, String promotionTo) {
        StringBuilder sb = new StringBuilder()
                .append(Position.getCoordinateString(parentPosition));
        if (capturing) {
            sb.append('x');
        }
        sb.append(Position.getCoordinateString(childPosition));
        if (promotionTo != null) {
            sb.append(promotionTo);
        }
        return sb.toString();
    }

    public abstract String getANCode();

    public static int[] stampThreatFlags(int pieceBitFlag, int[] positionBitFlags) {
        int pieceTypeFlag = getPieceTypeBitFlag(pieceBitFlag);
        return switch (pieceTypeFlag) {
            case PieceConfiguration.KING_OCCUPIED -> King.stampThreatFlags(pieceBitFlag, positionBitFlags);
            case PieceConfiguration.PAWN_OCCUPIED -> Pawn.stampThreatFlags(pieceBitFlag, positionBitFlags);
            default -> stampSimpleThreatFlags(pieceBitFlag, positionBitFlags);
        };
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
                if (!isValidPosition(testPositionIndex)) {
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
                if (BitUtil.hasBitFlag(positionBitFlags[testPositionIndex], PLAYER_OCCUPIED)) {
                    boolean kingOccupied = BitUtil.hasBitFlag(positionBitFlags[testPositionIndex], KING_OCCUPIED);
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
                } else if (BitUtil.hasBitFlag(positionBitFlags[testPositionIndex], OPPONENT_OCCUPIED)) {
                    break;
                }
                limit--;
            }
        }
        return positionBitFlags;
    }

    protected static int setDirectionalFlags(int number, int x, int y) {
        if (Math.abs(x) == 2 | Math.abs(y) == 2) {
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
        return switch (directionalBitFlags) {
            case PieceConfiguration.DIRECTION_N, PieceConfiguration.DIRECTION_S ->
                    filterDirectionalLimitsByPredicate(directionalLimits, dl -> dl[0] == 0);
            case PieceConfiguration.DIRECTION_NE, PieceConfiguration.DIRECTION_SW ->
                    filterDirectionalLimitsByPredicate(directionalLimits, dl -> dl[0] == dl[1]);
            case PieceConfiguration.DIRECTION_E, PieceConfiguration.DIRECTION_W ->
                    filterDirectionalLimitsByPredicate(directionalLimits, dl -> dl[1] == 0);
            case PieceConfiguration.DIRECTION_SE, PieceConfiguration.DIRECTION_NW ->
                    filterDirectionalLimitsByPredicate(directionalLimits, dl -> dl[0] == -dl[1]);
            default -> directionalLimits;
        };
    }

    private static int[][] filterDirectionalLimitsByPredicate(int[][] directionalLimits, Predicate<int[]> predicate) {
        return Arrays.stream(directionalLimits)
                .filter(predicate)
                .toArray(int[][]::new);
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
        return switch (pieceTypeFlag) {
            case KING_OCCUPIED -> PieceType.KING;
            case KNIGHT_OCCUPIED -> PieceType.KNIGHT;
            case BISHOP_OCCUPIED -> PieceType.BISHOP;
            case ROOK_OCCUPIED -> PieceType.ROOK;
            case QUEEN_OCCUPIED -> PieceType.QUEEN;
            case PAWN_OCCUPIED -> PieceType.PAWN;
            default -> throw new RuntimeException("No piece type recognised from which to get PieceType enum");
        };
    }

    public static int getSide(int pieceBitFlag) {
        return (pieceBitFlag & PieceConfiguration.BLACK_OCCUPIED) >> 9;
    }

    public static char getFENCode(int pieceBitFlag) {
        int pieceTypeFlag = getPieceTypeBitFlag(pieceBitFlag);
        return switch (pieceTypeFlag) {
            case KING_OCCUPIED -> King.getFENCode(pieceBitFlag);
            case KNIGHT_OCCUPIED -> Knight.getFENCode(pieceBitFlag);
            case BISHOP_OCCUPIED -> Bishop.getFENCode(pieceBitFlag);
            case ROOK_OCCUPIED -> Rook.getFENCode(pieceBitFlag);
            case QUEEN_OCCUPIED -> Queen.getFENCode(pieceBitFlag);
            case PAWN_OCCUPIED -> Pawn.getFENCode(pieceBitFlag);
            default -> throw new RuntimeException("No piece type recognised from which to get FEN code");
        };
    }

    public static int getValue(int pieceBitFlag) {
        int pieceTypeFlag = getPieceTypeBitFlag(pieceBitFlag);
        return FAST_VALUE_ARRAY[pieceTypeFlag >> 11];
    }
}
