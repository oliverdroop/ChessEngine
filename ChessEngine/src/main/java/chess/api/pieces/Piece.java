package chess.api.pieces;

import chess.api.BitUtil;
import chess.api.PieceConfiguration;
import chess.api.Position;
import chess.api.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class Piece {

    private static final Logger LOGGER = LoggerFactory.getLogger(Piece.class);

    private Side side;
    private PieceType pieceType;
    private int position;

    public Piece(Side side, PieceType pieceType, int position) {
        this.side = side;
        this.pieceType = pieceType;
        this.position = position;
    }

    /**
     * @return An array of size-2 int arrays, where the first int corresponds to a direction
     * and the second int corresponds to the maximum number of times the piece can move in that direction
     */
    public abstract int[][] getDirectionalLimits();

    public List<PieceConfiguration> getPossibleMoves(int[] positionBitFlags, PieceConfiguration currentConfiguration) {
        List<PieceConfiguration> pieceConfigurations = new ArrayList<>();
        for(int[] directionalLimit : getMovableDirectionalLimits(positionBitFlags)) {
            int directionX = directionalLimit[0];
            int directionY = directionalLimit[1];
            int limit = directionalLimit[2];
            int testPositionIndex = position;

            while (limit > 0) {
                testPositionIndex = Position.applyTranslation(testPositionIndex, directionX, directionY);
                if (testPositionIndex < 0 || testPositionIndex >= 64) {
                    break;
                }

                // Is this position a position which wouldn't block an existing checking direction?
                if (BitUtil.hasBitFlag(positionBitFlags[testPositionIndex], PieceConfiguration.DOES_NOT_BLOCK_CHECK)) {
                    limit--;
                    continue;
                }

                // Is this player piece blocked by another player piece?
                if (BitUtil.hasBitFlag(positionBitFlags[testPositionIndex], PieceConfiguration.PLAYER_OCCUPIED)) {
                    break;
                }

                // Is there an opponent piece on the position?
                Piece takenPiece = null;
                if (BitUtil.hasBitFlag(positionBitFlags[testPositionIndex], PieceConfiguration.OPPONENT_OCCUPIED)) {
                    takenPiece = currentConfiguration.getPieceAtPosition(testPositionIndex);
                }

                addNewPieceConfigurations(pieceConfigurations, currentConfiguration, testPositionIndex, takenPiece);

                if (takenPiece != null) {
                    // Stop considering moves beyond this taken piece
                    break;
                }
                limit--;
            }
        }
        return pieceConfigurations;
    }

    protected void addNewPieceConfigurations(List<PieceConfiguration> pieceConfigurations, PieceConfiguration currentConfiguration, int newPiecePosition,
            Piece takenPiece) {
        PieceConfiguration newConfiguration = new PieceConfiguration(currentConfiguration, false);
        for(Piece piece : currentConfiguration.getPieces()) {
            if (!piece.equals(this) && !piece.equals(takenPiece)) {
                newConfiguration.addPiece(piece);
            }
        }
        try {
            Piece movedPiece = this.getClass().getConstructor(Side.class, int.class).newInstance(side, newPiecePosition);
            newConfiguration.addPiece(movedPiece);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            LOGGER.error("Problem creating new piece configuration");
            return;
        }
        newConfiguration.setTurnSide(currentConfiguration.getTurnSide().getOpposingSide());

        if (takenPiece != null || this instanceof Pawn) {
            newConfiguration.setHalfMoveClock(0);
        } else {
            newConfiguration.setHalfMoveClock(currentConfiguration.getHalfMoveClock() + 1);
        }

        if (side == Side.BLACK) {
            newConfiguration.setFullMoveNumber(currentConfiguration.getFullMoveNumber() + 1);
        }
        pieceConfigurations.add(newConfiguration);
    }

    public abstract int[] stampThreatFlags(int[] positionBitFlags);

    public int[] stampSimpleThreatFlags(int[] positionBitFlags) {
        for(int[] directionalLimit : getDirectionalLimits()) {
            int directionX = directionalLimit[0];
            int directionY = directionalLimit[1];
            int limit = directionalLimit[2];
            int testPositionIndex = getPosition();
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

    protected int setDirectionalFlags(int number, int x, int y) {

        if (Math.abs(x) > 1 || Math.abs(y) > 1) {
            return number | PieceConfiguration.DIRECTION_KNIGHT;
        } else if (x > 0) {
            if (y > 0) {
                return number | PieceConfiguration.DIRECTION_NE;
            } else if (y < 0) {
                return number | PieceConfiguration.DIRECTION_SE;
            } else {
                return number | PieceConfiguration.DIRECTION_E;
            }
        } else if (x < 0) {
            if (y > 0) {
                return number | PieceConfiguration.DIRECTION_NW;
            } else if (y < 0) {
                return number | PieceConfiguration.DIRECTION_SW;
            } else {
                return number | PieceConfiguration.DIRECTION_W;
            }
        } else if (y > 0) {
            return number | PieceConfiguration.DIRECTION_N;
        }
        return number | PieceConfiguration.DIRECTION_S;
    }

    public static int getDirectionalFlags(int number) {
        return number & (PieceConfiguration.DIRECTION_N | PieceConfiguration.DIRECTION_NE
                | PieceConfiguration.DIRECTION_E | PieceConfiguration.DIRECTION_SE | PieceConfiguration.DIRECTION_S
                | PieceConfiguration.DIRECTION_SW | PieceConfiguration.DIRECTION_W | PieceConfiguration.DIRECTION_NW);
    }

    protected boolean hasDirectionalFlags(int number) {
        return getDirectionalFlags(number) != 0;
    }

    protected int[][] restrictDirections(int directionalBitFlags) {
        return restrictDirections(getDirectionalLimits(), directionalBitFlags);
    }

    protected int[][] restrictDirections(int[][] directionalLimits, int directionalBitFlags) {
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
            case PieceConfiguration.DIRECTION_KNIGHT:
            default:
                return directionalLimits;
        }
    }

    private int[][] filterDirectionalLimitsByPredicate(int[][] directionalLimits, Predicate<int[]> predicate) {
        return Arrays.stream(directionalLimits)
                .filter(predicate)
                .toArray(size1 -> new int[size1][]);
    }

    protected int[][] getMovableDirectionalLimits(int[] positionBitFlags) {
        int number = positionBitFlags[position];
        if (hasDirectionalFlags(number)) {
            return restrictDirections(getDirectionalFlags(number));
        }
        return getDirectionalLimits();
    }

    protected int[][] arrayDeepCopy(int[][] arrayToCopy, int startIndex, int endIndex) {
        int[][] output = new int[endIndex - startIndex][3];
        int io = 0;
        for(int i = startIndex; i < endIndex; i++) {
            output[io] = arrayToCopy[i].clone();
            io++;
        }
        return output;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public PieceType getPieceType() {
        return pieceType;
    }

    public void setPieceType(PieceType pieceType) {
        this.pieceType = pieceType;
    }

    public Side getSide() {
        return side;
    }

    public void setSide(Side side) {
        this.side = side;
    }

    public String toString() {
        return new StringBuilder()
                .append(this.getClass().getSimpleName())
                .append(':')
                .append(Position.getCoordinateString(position))
                .toString();
    }

    public abstract char getFENCode();

    public abstract int getValue();
}
