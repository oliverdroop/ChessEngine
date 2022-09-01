package chess.api;

import chess.api.pieces.King;
import chess.api.pieces.Knight;
import chess.api.pieces.Piece;
import com.google.common.collect.ImmutableSet;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public class PieceConfiguration {

    public static final int PLAYER_OCCUPIED = 64; // 6

    public static final int OPPONENT_OCCUPIED = 128; // 7

    public static final int THREATENED = 256; // 8

    public static final int KING_OCCUPIED = 512; // 9

    public static final int EN_PASSANT_SQUARE = 1024; // 10

    public static final int CASTLE_AVAILABLE = 2048; // 11

    public static final int DOES_NOT_BLOCK_CHECK = 4096; // 12

    public static final int DIRECTION_N = 8192; // 13

    public static final int DIRECTION_NE = 16384; // 14

    public static final int DIRECTION_E = 32768; // 15

    public static final int DIRECTION_SE = 65536; // 16

    public static final int DIRECTION_S = 131072; // 17

    public static final int DIRECTION_SW = 262144; // 18

    public static final int DIRECTION_W = 524288; // 19

    public static final int DIRECTION_NW = 1048576; // 20

    public static final int DIRECTION_NNE = 2097152; // 21

    public static final int DIRECTION_ENE = 4194304; // 22

    public static final int DIRECTION_ESE = 8388608; // 23

    public static final int DIRECTION_SSE = 16777216; // 24

    public static final int DIRECTION_SSW = 33554432; // 25

    public static final int DIRECTION_WSW = 67108864; // 26

    public static final int DIRECTION_WNW = 134217728; // 27

    public static final int DIRECTION_NNW = 268435456; // 28

    public static final ImmutableSet<Integer> ALL_DIRECTIONAL_FLAGS = ImmutableSet.of(DIRECTION_N, DIRECTION_NNE,
            DIRECTION_NE, DIRECTION_ENE, DIRECTION_E, DIRECTION_ESE, DIRECTION_SE, DIRECTION_SSE, DIRECTION_S,
            DIRECTION_SSW, DIRECTION_SW, DIRECTION_WSW, DIRECTION_W, DIRECTION_WNW, DIRECTION_NW, DIRECTION_NNW);

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
            return getCheckNonBlockerPositionBitFlags(checkedPlayerKingBitFlag, positionBitFlags);
        }
        return positionBitFlags;
    }

    public static int[] getCheckNonBlockerPositionBitFlags(int kingPositionBitFlag, int[] positionBitFlags) {
        int kingPositionDirectionalFlags = Piece.getDirectionalFlags(kingPositionBitFlag);
        if (ALL_DIRECTIONAL_FLAGS.contains(kingPositionDirectionalFlags)) {
            int currentPosition = kingPositionBitFlag;
            int nextPositionIndex = Position.applyTranslationTowardsThreat(kingPositionDirectionalFlags, currentPosition);

            while(!BitUtil.hasBitFlag(currentPosition, PieceConfiguration.OPPONENT_OCCUPIED) && nextPositionIndex >= 0) {
                positionBitFlags[nextPositionIndex] = BitUtil.applyBitFlag(positionBitFlags[nextPositionIndex], PieceConfiguration.DOES_NOT_BLOCK_CHECK);
                currentPosition = positionBitFlags[nextPositionIndex];
                nextPositionIndex = Position.applyTranslationTowardsThreat(kingPositionDirectionalFlags, currentPosition);
            }
        }

        // Now reverse the bit DOES_NOT_BLOCK_CHECK bit flags because they're on the squares which can block check
        for(int position : Position.POSITIONS) {
            positionBitFlags[position] = BitUtil.reverseBitFlag(positionBitFlags[position], PieceConfiguration.DOES_NOT_BLOCK_CHECK);
        }
        return positionBitFlags;
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
