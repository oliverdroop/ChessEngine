package chess.api.pieces;

import chess.api.configuration.PieceConfiguration;
import chess.api.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;

import static chess.api.configuration.PieceConfiguration.getPieceTypeBitFlag;
import static chess.api.MoveDescriber.describeMove;
import static chess.api.configuration.PieceConfiguration.*;
import static chess.api.Position.isValidPosition;

public abstract class Piece {

    private static final Logger LOGGER = LoggerFactory.getLogger(Piece.class);

    public static final int[] FAST_VALUE_ARRAY = {0, 3, 3, 0, 5, 0, 0, 0, 9, 0, 0, 0, 0, 0, 0, 0, 1};

    /**
     * @return An array of size-3 int arrays, where the first two ints correspond to a direction
     * and the third int corresponds to the maximum number of times the piece can move in that direction
     */
    public static int[][] getDirectionalLimits(int pieceBitFlag) {
        final int pieceFlag = getPieceTypeBitFlag(pieceBitFlag);
        return switch (pieceFlag) {
            case PAWN_OCCUPIED -> Pawn.getUnrestrictedDirectionalLimits(pieceBitFlag);
            case BISHOP_OCCUPIED -> Bishop.getDirectionalLimits();
            case ROOK_OCCUPIED -> Rook.getDirectionalLimits();
            case QUEEN_OCCUPIED -> Queen.getDirectionalLimits();
            case KNIGHT_OCCUPIED -> Knight.getDirectionalLimits();
            case KING_OCCUPIED -> King.getDirectionalLimits(pieceBitFlag);
            default -> new int[0][];
        };
    }

    public static List<PieceConfiguration> getPossibleMoves(int pieceBitFlag,
                                                            PieceConfiguration currentConfiguration) {
        final int pieceTypeFlag = getPieceTypeBitFlag(pieceBitFlag);
        switch(pieceTypeFlag) {
            case PAWN_OCCUPIED:
                return Pawn.getPossibleMoves(pieceBitFlag, currentConfiguration);
            case KING_OCCUPIED:
                return King.getPossibleMoves(pieceBitFlag, currentConfiguration);
            default:
                break;
        }
        final List<PieceConfiguration> pieceConfigurations = new ArrayList<>();
        for(int[] directionalLimit : getMovableDirectionalLimits(pieceBitFlag, currentConfiguration)) {
            final int directionX = directionalLimit[0];
            final int directionY = directionalLimit[1];
            int limit = directionalLimit[2];
            int testPosition = Position.getPosition(pieceBitFlag);

            while (limit > 0) {
                testPosition = Position.applyTranslation(testPosition, directionX, directionY);
                if (!isValidPosition(testPosition)) {
                    break;
                }

                // Is this player piece blocked by another player piece?
                if (currentConfiguration.isPlayerOccupied(testPosition)) {
                    break;
                }

                // Is there an opponent piece on the position?
                int takenPieceBitFlag = -1;
                if (currentConfiguration.isOpponentOccupied(testPosition)) {
                    takenPieceBitFlag = currentConfiguration.getPieceAtPosition(testPosition);
                }

                // Is this position a position which wouldn't block an existing checking direction?
                if (currentConfiguration.isIneffectiveCheckBlockAttempt(testPosition)) {
                    if (takenPieceBitFlag == -1) {
                        limit--;
                        continue;
                    } else {
                        break;
                    }
                }

                final short move = describeMove(pieceBitFlag & 63, testPosition, 0);
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

    public static void stampThreatFlags(int pieceBitFlag, PieceConfiguration pieceConfiguration) {
        int pieceTypeFlag = getPieceTypeBitFlag(pieceBitFlag);
        switch (pieceTypeFlag) {
            case PieceConfiguration.KING_OCCUPIED:
                King.stampThreatFlags(pieceBitFlag, pieceConfiguration);
                break;
            case PieceConfiguration.PAWN_OCCUPIED:
                Pawn.stampThreatFlags(pieceBitFlag, pieceConfiguration);
                break;
            default: stampSimpleThreatFlags(pieceBitFlag, pieceConfiguration);
        }
    }

    public static void stampSimpleThreatFlags(int pieceBitFlag, PieceConfiguration pieceConfiguration) {
        for(int[] directionalLimit : getDirectionalLimits(pieceBitFlag)) {
            final int directionX = directionalLimit[0];
            final int directionY = directionalLimit[1];
            int limit = directionalLimit[2];
            int testPosition = getPosition(pieceBitFlag);
            int potentialKingProtectorPosition = -1;
            while (limit > 0) {
                testPosition = Position.applyTranslation(testPosition, directionX, directionY);
                if (!isValidPosition(testPosition)) {
                    break;
                }

                if (potentialKingProtectorPosition < 0) {
                    // Opponent piece threatens the position
                    pieceConfiguration.setThreatened(testPosition);
                    // Is this opponent piece blocked by another opponent piece?
                    if (pieceConfiguration.isOpponentOccupied(testPosition)) {
                        break;
                    }
                }

                // Is there a player piece on the position?
                if (pieceConfiguration.isPlayerOccupied(testPosition)) {
                    final boolean kingOccupied = pieceConfiguration.isKingOccupied(testPosition);
                    if (potentialKingProtectorPosition < 0) {
                        if (kingOccupied) {
                            // First player piece encountered in this direction is the player's king
                            // Player's king can't be a king protector
                            final int directionalFlag = getDirectionalFlag(directionX, directionY);
                            pieceConfiguration.setDirectionalFlag(testPosition, directionalFlag);
                        } else {
                            // First player piece encountered in this direction is not the player's king
                            potentialKingProtectorPosition = testPosition;
                        }
                    } else if (kingOccupied) {
                        // Second player piece encountered in this direction is the player's king
                        final int directionalFlag = getDirectionalFlag(directionX, directionY);
                        pieceConfiguration.setDirectionalFlag(potentialKingProtectorPosition, directionalFlag);
                        break;
                    } else {
                        // Second player piece encountered in this direction is not the player's king.
                        break;
                    }
                } else if (pieceConfiguration.isOpponentOccupied(testPosition)) {
                    break;
                }
                limit--;
            }
        }
    }

    protected static int getDirectionalFlag(int x, int y) {
        if ((x & 3) == 2 | (y & 3) == 2) {
            return PieceConfiguration.DIRECTION_ANY_KNIGHT;
        }
        return Position.DIRECTIONAL_BIT_FLAG_GRID[y + 1][x + 1];
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

    protected static int[][] getMovableDirectionalLimits(int pieceBitFlag, PieceConfiguration currentConfiguration) {
        int position = getPosition(pieceBitFlag);
        int number = currentConfiguration.getPieceAtPosition(position);
        if (hasDirectionalFlags(number)) {
            return restrictDirections(pieceBitFlag, getDirectionalFlags(number));
        }
        return getDirectionalLimits(pieceBitFlag);
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
            default -> throw new IllegalArgumentException("No piece type recognised from which to get PieceType enum");
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
            default -> throw new IllegalArgumentException("No piece type recognised from which to get FEN code");
        };
    }

    public static int getValue(int pieceBitFlag) {
        int pieceTypeFlag = getPieceTypeBitFlag(pieceBitFlag);
        return FAST_VALUE_ARRAY[pieceTypeFlag >> 11];
    }
}
