package chess.api;

import chess.api.pieces.King;
import chess.api.pieces.Piece;
import com.google.common.collect.ImmutableSet;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;

public class PieceConfiguration {

    public static final int PLAYER_OCCUPIED = 64;

    public static final int OPPONENT_OCCUPIED = 128;

    public static final int THREATENED = 256;

    public static final int KING_OCCUPIED = 512;

    public static final int EN_PASSANT_SQUARE = 1024;

    public static final int CASTLE_AVAILABLE = 2048;

    public static final int HORIZONTAL_DIRECTION = 4096;

    public static final int VERTICAL_DIRECTION = 8192;

    public static final int NW_SE_DIRECTION = 16384;

    public static final int NE_SW_DIRECTION = 32768;

    public static final int ANY_KNIGHT_DIRECTION = 65536;

    public static final int DOES_NOT_BLOCK_CHECK = 131072;

    public static final ImmutableSet<Integer> ALL_DIRECTIONAL_FLAGS = ImmutableSet.of(HORIZONTAL_DIRECTION,
            VERTICAL_DIRECTION, NW_SE_DIRECTION, NE_SW_DIRECTION, ANY_KNIGHT_DIRECTION);

    private Set<Piece> pieces = new HashSet<>();

    private Integer enPassantSquare;

    private List<Integer> castlePositions = new ArrayList<>();

    private Side turnSide;

    private int halfMoveClock = 0;

    private int fullMoveNumber = 0;

    public PieceConfiguration() {

    }

    public PieceConfiguration(PieceConfiguration copiedConfiguration, boolean copyPieces) {
        this.turnSide = copiedConfiguration.getTurnSide();
        this.halfMoveClock = copiedConfiguration.getHalfMoveClock();
        this.fullMoveNumber = copiedConfiguration.getFullMoveNumber();
        this.castlePositions = new ArrayList<>(copiedConfiguration.getCastlePositions());
        if (copyPieces) {
            this.pieces = new HashSet<>(copiedConfiguration.getPieces());
        }
    }

    public List<PieceConfiguration> getPossiblePieceConfigurations() {
        int[] positionBitFlags = stampCheckNonBlockerFlags(stampThreatFlags(stampOccupationFlags(Arrays.copyOf(Position.POSITIONS, 64))));
        return pieces.stream()
                .filter(p -> p.getSide() == turnSide)
                .flatMap(p -> getPossiblePieceConfigurationsForPiece(p, positionBitFlags).stream())
                .collect(Collectors.toList());
    }

    public List<PieceConfiguration> getPossiblePieceConfigurationsForPiece(Piece piece, int[] positionBitFlags) {
        return piece.getPossibleMoves(positionBitFlags, this);
    }

    private int[] stampOccupationFlags(int[] positions) {
        for(Piece piece : pieces) {
            int occupationFlag = piece.getSide() == turnSide ? PLAYER_OCCUPIED : OPPONENT_OCCUPIED;
            positions[piece.getPosition()] = BitUtil.applyBitFlag(positions[piece.getPosition()], occupationFlag);
            if (piece instanceof King) {
                positions[piece.getPosition()] = BitUtil.applyBitFlag(positions[piece.getPosition()], KING_OCCUPIED);
            }
        }
        if (enPassantSquare != null) {
            positions[enPassantSquare] = BitUtil.applyBitFlag(positions[enPassantSquare], EN_PASSANT_SQUARE);
        }
        for(Integer castlePosition : castlePositions) {
            positions[castlePosition] = BitUtil.applyBitFlag(positions[castlePosition], CASTLE_AVAILABLE);
        }
        return positions;
    }

    private int[] stampThreatFlags(int[] positionBitFlags) {
        for(Piece piece : pieces) {
            if (piece.getSide() != turnSide) {
                positionBitFlags = piece.stampThreatFlags(positionBitFlags);
            }
        }
        return positionBitFlags;
    }

    private int[] stampCheckNonBlockerFlags(int[] positionBitFlags) {
        int checkedPlayerKingBitFlag = isPlayerInCheck(positionBitFlags);
        if (checkedPlayerKingBitFlag >= 0) {
            for(int checkNonBlockerBitFlag : getCheckNonBlockerPositionBitFlags(checkedPlayerKingBitFlag, positionBitFlags)) {
                positionBitFlags[checkNonBlockerBitFlag & 63] = BitUtil.applyBitFlag(
                        positionBitFlags[checkNonBlockerBitFlag & 63], DOES_NOT_BLOCK_CHECK);
            }
        }
        return positionBitFlags;
    }

    public static int[] getCheckNonBlockerPositionBitFlags(int kingPositionBitFlag, int[] positionBitFlags) {
        int kingPositionDirectionalFlags = Piece.getDirectionalFlags(kingPositionBitFlag);
        if (!ALL_DIRECTIONAL_FLAGS.contains(kingPositionDirectionalFlags)) {
            return positionBitFlags;
        }
        return Arrays.stream(positionBitFlags)
                .filter(getCheckBlockerPredicate(kingPositionBitFlag).negate())
                .toArray();
    }

    private static IntPredicate getCheckBlockerPredicate(int kingPositionBitFlag) {
        switch(Piece.getDirectionalFlags(kingPositionBitFlag)) {
            case PieceConfiguration.HORIZONTAL_DIRECTION:
                return i -> Position.getY(i) == Position.getY(kingPositionBitFlag);
            case PieceConfiguration.VERTICAL_DIRECTION:
                return i -> Position.getX(i) == Position.getX(kingPositionBitFlag);
            case PieceConfiguration.NE_SW_DIRECTION:
                return i -> ((i & 63) - (kingPositionBitFlag & 63)) % 9 == 0;
            case PieceConfiguration.NW_SE_DIRECTION:
                return i -> ((i & 63) - (kingPositionBitFlag & 63)) % 7 == 0;
            case PieceConfiguration.ANY_KNIGHT_DIRECTION:
            default:
                return i -> true;
        }
    }

    public static int isPlayerInCheck(int[] positionBitFlags) {
        return Arrays.stream(positionBitFlags)
                .filter(pbf -> BitUtil.hasBitFlag(pbf, PieceConfiguration.PLAYER_OCCUPIED
                        | PieceConfiguration.KING_OCCUPIED | PieceConfiguration.THREATENED))
                .findFirst()
                .orElse(-1);
    }

    public Set<Piece> getPieces() {
        return pieces;
    }

    public void addPiece(Piece piece) {
        pieces.add(piece);
    }

    public void removePiece(Piece piece) {
        pieces.remove(piece);
    }

    public Piece getPieceAtPosition(int positionBitFlag) {
        for(Piece piece : pieces) {
            if ((positionBitFlag & 63) == piece.getPosition()) {
                return piece;
            }
        }
        return null;
    }

    public void promotePiece(int position, Class<? extends Piece> clazz) {
        Piece oldPiece = getPieceAtPosition(position);
        try {
            Piece newPiece = clazz.getConstructor(Side.class, int.class).newInstance(oldPiece.getSide(), position);
            removePiece(oldPiece);
            addPiece(newPiece);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public Integer getEnPassantSquare() {
        return enPassantSquare;
    }

    public void setEnPassantSquare(Integer enPassantSquare) {
        this.enPassantSquare = enPassantSquare;
    }

    public Side getTurnSide() {
        return turnSide;
    }

    public List<Integer> getCastlePositions() {
        return castlePositions;
    }

    public void setCastlePositions(List<Integer> castlePositions) {
        this.castlePositions = castlePositions;
    }

    public void addCastlePosition(Integer castlePosition) {
        this.castlePositions.add(castlePosition);
    }

    public void removeCastlePosition(Integer castlePosition) {
        this.castlePositions.remove(castlePosition);
    }

    public void setTurnSide(Side turnSide) {
        this.turnSide = turnSide;
    }

    public int getHalfMoveClock() {
        return halfMoveClock;
    }

    public void setHalfMoveClock(int halfMoveClock) {
        this.halfMoveClock = halfMoveClock;
    }

    public int getFullMoveNumber() {
        return fullMoveNumber;
    }

    public void setFullMoveNumber(int fullMoveNumber) {
        this.fullMoveNumber = fullMoveNumber;
    }

    public String toString() {
        return FENWriter.write(this);
    }
}
