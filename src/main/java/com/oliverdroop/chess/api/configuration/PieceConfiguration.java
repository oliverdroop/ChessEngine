package com.oliverdroop.chess.api.configuration;

import com.oliverdroop.chess.api.BitUtil;
import com.oliverdroop.chess.api.FENWriter;
import com.oliverdroop.chess.api.Position;
import com.oliverdroop.chess.api.pieces.Knight;
import com.oliverdroop.chess.api.pieces.Piece;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.oliverdroop.chess.api.BitUtil.*;
import static com.oliverdroop.chess.api.pieces.King.CASTLE_POSITION_MAPPINGS;
import static com.oliverdroop.chess.api.pieces.Pawn.PROMOTION_PIECE_TYPES;

public abstract class PieceConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(PieceConfiguration.class);

    public static final int PLAYER_OCCUPIED = 64; // 6

    public static final int OPPONENT_OCCUPIED = 128; // 7

    public static final int WHITE_OCCUPIED = 256; // 8

    public static final int BLACK_OCCUPIED = 512; // 9

    public static final int KING_OCCUPIED = 1024; // 10

    public static final int KNIGHT_OCCUPIED = 2048; // 11

    public static final int BISHOP_OCCUPIED = 4096; // 12

    public static final int ROOK_OCCUPIED = 8192; // 13

    public static final int QUEEN_OCCUPIED = 16384; // 14

    public static final int PAWN_OCCUPIED = 32768; // 15

    public static final int THREATENED = 65536; // 16

    public static final int DIRECTION_N = 131072; // 17

    public static final int DIRECTION_NE = 262144; // 18

    public static final int DIRECTION_E = 524288; // 19

    public static final int DIRECTION_SE = 1048576; // 20

    public static final int DIRECTION_S = 2097152; // 21

    public static final int DIRECTION_SW = 4194304; // 22

    public static final int DIRECTION_W = 8388608; // 23

    public static final int DIRECTION_NW = 16777216; // 24

    public static final int DIRECTION_ANY_KNIGHT = 33554432; // 25

    public static final int DOES_NOT_BLOCK_CHECK = 67108864; // 26

    public static final int CASTLE_AVAILABLE = 134217728; // 27

    public static final int EN_PASSANT_SQUARE = 268435456; // 28
    
    public static final int ALL_DIRECTIONAL_FLAGS_COMBINED = DIRECTION_N | DIRECTION_NE | DIRECTION_E | DIRECTION_SE
            | DIRECTION_S | DIRECTION_SW | DIRECTION_W | DIRECTION_NW | DIRECTION_ANY_KNIGHT;

    public static final int ALL_PIECE_FLAGS_COMBINED = KING_OCCUPIED | KNIGHT_OCCUPIED | BISHOP_OCCUPIED
            | ROOK_OCCUPIED | QUEEN_OCCUPIED | PAWN_OCCUPIED;

    public static final int ALL_PIECE_AND_COLOUR_FLAGS_COMBINED = ALL_PIECE_FLAGS_COMBINED | WHITE_OCCUPIED | BLACK_OCCUPIED;

    public static final int ALL_PIECE_COLOUR_AND_OCCUPATION_FLAGS_COMBINED = PLAYER_OCCUPIED | OPPONENT_OCCUPIED
            | ALL_PIECE_FLAGS_COMBINED | WHITE_OCCUPIED | BLACK_OCCUPIED;

    public static final int CHECK_FLAGS_COMBINED = PLAYER_OCCUPIED | KING_OCCUPIED | THREATENED;

    public static final int PLAYER_KING_OCCUPIED = KING_OCCUPIED | PLAYER_OCCUPIED;

    protected static final int COLOUR_FLAGS_COMBINED = WHITE_OCCUPIED | BLACK_OCCUPIED;

    public static final int NO_CAPTURE_OR_PAWN_MOVE_LIMIT = 99;

    protected static final int[] ALL_DIRECTIONAL_FLAGS = {
            DIRECTION_N,
            DIRECTION_NE,
            DIRECTION_E,
            DIRECTION_SE,
            DIRECTION_S,
            DIRECTION_SW,
            DIRECTION_W,
            DIRECTION_NW,
            DIRECTION_ANY_KNIGHT
    };

    private static final int[][] CASTLE_POSITION_COMBINATIONS = {
            {},
            {2},
            {6},
            {2, 6},
            {58},
            {2, 58},
            {6, 58},
            {2, 6, 58},
            {62},
            {2, 62},
            {6, 62},
            {2, 6, 62},
            {58, 62},
            {2, 58, 62},
            {6, 58, 62},
            {2, 6, 58, 62}
    };

    // 0 turnSide, 1-4 castlePositions, 5-11 halfMoveClock, 12-24 fullMoveNumber, 25-30 enPassant position, 31 enPassantIsNotSet
    protected int auxiliaryData = Integer.MIN_VALUE;

    private short[] historicMoves;

    public abstract Class<? extends PieceConfiguration> getConfigurationClass();

    public abstract List<PieceConfiguration> getOnwardConfigurations();

    public abstract List<PieceConfiguration> getOnwardConfigurationsForPiece(int pieceBitFlag);

    public abstract int getValueDifferential();

    public abstract double getLesserScore();

    public abstract boolean isCheck();

    public abstract void addPiece(int pieceData);

    protected abstract void removePiece(int pieceData);

    public abstract int getPieceAtPosition(int position);

    protected abstract int getPieceAndColourFlags(int position);

    public abstract boolean isPlayerOccupied(int position);

    public abstract boolean isKingOccupied(int position);

    public abstract boolean isPlayerKingOccupied(int position);

    public abstract boolean isOpponentOccupied(int position);

    public abstract boolean isOpponentOccupiedOrEnPassantSquare(int position);

    protected abstract boolean isOpponentKnightOccupied(int position);

    public abstract boolean isThreatened(int position);

    public abstract void setThreatened(int position);

    public abstract void setDirectionalFlag(int position, int directionalFlag);

    public abstract boolean isIneffectiveCheckBlockAttempt(int position);

    protected abstract void setDoesNotBlockCheck(int position);

    public abstract boolean isCastleAvailable(int position);

    public abstract void setHigherBitFlags();

    public static PieceConfiguration toNewConfigurationFromMoves(PieceConfiguration originalConfiguration, short[] historicMoves) {
        PieceConfiguration currentConfiguration = originalConfiguration;
        for (short historicMove : historicMoves) {
            currentConfiguration = toNewConfigurationFromMove(currentConfiguration, historicMove);
        }
        return currentConfiguration;
    }

    public static PieceConfiguration toNewConfigurationFromMove(PieceConfiguration previousConfiguration, short moveDescription) {
        final PieceConfiguration newConfiguration = getPieceConfigurationImplementation(previousConfiguration);
        newConfiguration.addHistoricMove(previousConfiguration, moveDescription);
        final int fromPos = (moveDescription & 0b0000111111000000) >> 6;
        final int toPos = moveDescription & 0b0000000000111111;
        final int promotionBitFlag = (moveDescription & 0b1111000000000000) >>> 1;
        final int oldPieceBitFlag = previousConfiguration.getPieceAtPosition(fromPos);
        final int newPieceBitFlag = promotionBitFlag == 0
            ? (oldPieceBitFlag & ALL_PIECE_AND_COLOUR_FLAGS_COMBINED) | toPos
            : (oldPieceBitFlag & COLOUR_FLAGS_COMBINED) | promotionBitFlag | toPos;
        final int directlyTakenPieceBitFlag = previousConfiguration.getPieceAtPosition(toPos) & ALL_PIECE_FLAGS_COMBINED;
        final boolean isAnyPieceTaken = directlyTakenPieceBitFlag > 0
            || (previousConfiguration.getEnPassantSquare() == toPos && (newPieceBitFlag & PAWN_OCCUPIED) != 0);
        final int posDiff = toPos - fromPos;
        newConfiguration.removePiece(fromPos);
        newConfiguration.removePiece(toPos);
        newConfiguration.addPiece(newPieceBitFlag);
        if (isAnyPieceTaken && directlyTakenPieceBitFlag == 0) {
            // Remove pawn taken by en passant
            final int takenPiecePosition = toPos - (8 - (previousConfiguration.getTurnSide() * 16));
            newConfiguration.removePiece(takenPiecePosition);
        }
        if (hasBitFlag(oldPieceBitFlag, KING_OCCUPIED)) {
            if (Math.abs(posDiff) == 2) {
                // Castling
                final int rookFromPos = CASTLE_POSITION_MAPPINGS.get(toPos);
                final int rookToPos = posDiff > 0 ? fromPos + 1 : fromPos - 1;
                final int oldRookBitFlag = previousConfiguration.getPieceAtPosition(rookFromPos);
                final int newRookBitFlag = (oldRookBitFlag & ALL_PIECE_AND_COLOUR_FLAGS_COMBINED) | rookToPos;
                newConfiguration.removePiece(rookFromPos);
                newConfiguration.addPiece(newRookBitFlag);
            }
            // Remove castle positions for side because king has moved
            final int leftCastlePosition = 2 + (56 * ((oldPieceBitFlag & BLACK_OCCUPIED) >> 9));
            final int rightCastlePosition = leftCastlePosition + 4;
            newConfiguration.removeCastlePosition(leftCastlePosition);
            newConfiguration.removeCastlePosition(rightCastlePosition);
        }
        if (hasBitFlag(oldPieceBitFlag, ROOK_OCCUPIED) && CASTLE_POSITION_MAPPINGS.containsValue(fromPos)) {
            // Remove castle position for rook because rook has moved
            for(Map.Entry<Integer, Integer> entry : CASTLE_POSITION_MAPPINGS.entrySet()) {
                if (entry.getValue() == fromPos) {
                    newConfiguration.removeCastlePosition(entry.getKey());
                }
            }
        }
        if (hasBitFlag(directlyTakenPieceBitFlag, ROOK_OCCUPIED)) {
            // Remove castle position for rook because rook has been taken
            for(Map.Entry<Integer, Integer> entry : CASTLE_POSITION_MAPPINGS.entrySet()) {
                if (entry.getValue() == toPos) {
                    newConfiguration.removeCastlePosition(entry.getKey());
                }
            }
        }

        if (hasBitFlag(oldPieceBitFlag, PAWN_OCCUPIED) && Math.abs(posDiff) == 16) {
            // Set the en passant square
            newConfiguration.setEnPassantSquare((fromPos + toPos) >> 1);
        } else {
            // Clear the en passant square
            newConfiguration.setEnPassantSquare(-1);
        }

        if (isAnyPieceTaken || hasBitFlag(oldPieceBitFlag, PAWN_OCCUPIED)) {
            // Reset the half move clock to zero
            newConfiguration.setHalfMoveClock(0);
        } else {
            // Increment the half move clock
            newConfiguration.setHalfMoveClock(previousConfiguration.getHalfMoveClock() + 1);
        }

        // Increment the full move number
        newConfiguration.setFullMoveNumber(previousConfiguration.getFullMoveNumber() + previousConfiguration.getTurnSide());
        // Switch the turn side
        newConfiguration.setTurnSide(previousConfiguration.getOpposingSide());
        return newConfiguration;
    }

    public static int getPieceTypeBitFlag(int positionBitFlag) {
        return positionBitFlag & ALL_PIECE_FLAGS_COMBINED;
    }

    public String getAlgebraicNotation(PieceConfiguration previousConfiguration) {
        boolean capturing = false;
        int previousBitFlag = Integer.MIN_VALUE;
        int currentBitFlag = Integer.MIN_VALUE;
        for(int pos : Position.POSITIONS) {
            int currentPieceOnPosition = getPieceAndColourFlags(pos);
            int previousPieceOnPosition = previousConfiguration.getPieceAndColourFlags(pos);
            int currentColour = currentPieceOnPosition & COLOUR_FLAGS_COMBINED;
            int previousColour = previousPieceOnPosition & COLOUR_FLAGS_COMBINED;
            if (currentPieceOnPosition == previousPieceOnPosition) {
                continue;
            }
            if (currentPieceOnPosition == 0 && !BitUtil.hasBitFlag(previousBitFlag, KING_OCCUPIED)) {
                // Moving from this position
                previousBitFlag = previousConfiguration.getPieceAtPosition(pos);
            } else if (previousPieceOnPosition == 0 && !BitUtil.hasBitFlag(currentBitFlag, KING_OCCUPIED)) {
                // Moving to this position
                currentBitFlag = getPieceAtPosition(pos);
            } else if (currentColour != 0 && previousColour != 0 && currentColour != previousColour) {
                currentBitFlag = getPieceAtPosition(pos);
                capturing = true;
            }
        }
        String promotionTo = null;
        if ((previousBitFlag & ALL_PIECE_FLAGS_COMBINED) != (currentBitFlag & ALL_PIECE_FLAGS_COMBINED)) {
            promotionTo = PROMOTION_PIECE_TYPES.get(currentBitFlag & ALL_PIECE_FLAGS_COMBINED).toLowerCase();
        }
        return Piece.getAlgebraicNotation(
            Piece.getPosition(previousBitFlag), Piece.getPosition(currentBitFlag), capturing, promotionTo);
    }

    public boolean isDraw() {
        return getHalfMoveClock() > NO_CAPTURE_OR_PAWN_MOVE_LIMIT || isThreefoldRepetitionFailure();
    }

    public int adjustForDraw(int valueDifferential) {
        if (isDraw()) {
            if (Math.abs(valueDifferential) > 3) {
                valueDifferential -= Math.round(Math.signum(valueDifferential) * Short.MAX_VALUE);
            } else {
                valueDifferential += Short.MAX_VALUE;
            }
        }
        return valueDifferential;
    }

    boolean isThreefoldRepetitionFailure() {
        if (historicMoves == null || historicMoves.length < 8) {
            return false;
        }
        final int historyLength = historicMoves.length;
        final short[] lastFourMoves = Arrays.copyOfRange(historicMoves, historyLength - 4, historyLength);
        final short[] previousFourMoves = Arrays.copyOfRange(historicMoves, historyLength - 8, historyLength - 4);
        return Arrays.equals(lastFourMoves, previousFourMoves);
    }

    private static PieceConfiguration getPieceConfigurationImplementation(PieceConfiguration previousConfiguration) {
        final PieceConfiguration newConfiguration;
        if (previousConfiguration instanceof IntsPieceConfiguration previousConfigurationImpl) {
            newConfiguration = new IntsPieceConfiguration(previousConfigurationImpl, true);
        } else if (previousConfiguration instanceof LongsPieceConfiguration previousConfigurationImpl) {
            newConfiguration = new LongsPieceConfiguration(previousConfigurationImpl, true);
        } else {
            throw new IllegalArgumentException("Input PieceConfiguration was not a usable implementation");
        }
        return newConfiguration;
    }

    public int getTurnSide() {
        return auxiliaryData & 1;
    }

    public void setTurnSide(int turnSide) {
        auxiliaryData = overwriteBits(auxiliaryData, 1, turnSide);
    }

    public int getOpposingSide() {
        return (~auxiliaryData) & 1;
    }

    public int[] getCastlePositions() {
        final int castlePositionBits = (auxiliaryData >> 1) & 0b1111;
        return CASTLE_POSITION_COMBINATIONS[castlePositionBits];
    }

    public void addCastlePosition(int castlePosition) {
        final int index = Arrays.binarySearch(CASTLE_POSITION_COMBINATIONS[15], castlePosition);
        auxiliaryData = applyBitFlag(auxiliaryData, 0b10 << index);
    }

    public void removeCastlePosition(int castlePosition) {
        final int index = Arrays.binarySearch(CASTLE_POSITION_COMBINATIONS[15], castlePosition);
        auxiliaryData = clearBits(auxiliaryData, 0b10 << index);
    }

    public int getHalfMoveClock() {
        return (auxiliaryData >> 5) & 0b1111111;
    }

    public void setHalfMoveClock(int halfMoveClock) {
        auxiliaryData = overwriteBits(auxiliaryData, 0b111111100000, halfMoveClock << 5);
    }

    public int getFullMoveNumber() {
        return (auxiliaryData >> 12) & 0b1111111111111;
    }

    public void setFullMoveNumber(int fullMoveNumber) {
        auxiliaryData = overwriteBits(auxiliaryData, 0b1111111111111000000000000, fullMoveNumber << 12);
    }

    public int getEnPassantSquare() {
        final int pos = (auxiliaryData >> 25) & 0b111111;
        final int negativeBit = auxiliaryData & Integer.MIN_VALUE;
        return pos | negativeBit;
    }

    public void setEnPassantSquare(int enPassantSquare) {
        final int negativeBit = enPassantSquare & Integer.MIN_VALUE;
        final int pos = (enPassantSquare & 0b111111) << 25 ;
        auxiliaryData = overwriteBits(auxiliaryData, 0b11111110000000000000000000000000, pos | negativeBit);
    }

    protected void setCheckNonBlockerFlags(int kingPositionBitFlag, int kingPositionDirectionalFlags) {
        // The king is only checked from one direction
        int currentPosition = kingPositionBitFlag;
        int nextPositionIndex = Position.applyTranslationTowardsThreat(kingPositionDirectionalFlags, currentPosition);

        while(nextPositionIndex >= 0 && !BitUtil.hasBitFlag(getPieceAtPosition(currentPosition), OPPONENT_OCCUPIED)) {
            setDoesNotBlockCheck(nextPositionIndex);
            currentPosition = nextPositionIndex;
            nextPositionIndex = Position.applyTranslationTowardsThreat(kingPositionDirectionalFlags, currentPosition);
        }

        if (kingPositionDirectionalFlags == DIRECTION_ANY_KNIGHT) {
            // The king is checked only by a knight
            for(int[] directionalLimit : Knight.getDirectionalLimits()) {
                int possibleCheckingKnightPosition = Position.applyTranslation(currentPosition, directionalLimit[0], directionalLimit[1]);
                if (possibleCheckingKnightPosition >= 0
                    && isOpponentKnightOccupied(possibleCheckingKnightPosition)) {
                    // Stamp the checking knight's position so it becomes a movable position
                    setDoesNotBlockCheck(possibleCheckingKnightPosition);
                }
            }
        }
    }

    public String toString() {
        return FENWriter.write(this);
    }

    public short[] getHistoricMoves() {
        return historicMoves;
    }

    public void setHistoricMoves(short[] historicMoves) {
        this.historicMoves = historicMoves;
    }

    public void addHistoricMove(PieceConfiguration previousConfiguration, short newMove) {
        if (previousConfiguration.getHistoricMoves() != null) {
            historicMoves = new short[previousConfiguration.getHistoricMoves().length + 1];
            System.arraycopy(previousConfiguration.getHistoricMoves(), 0, historicMoves, 0, previousConfiguration.getHistoricMoves().length);
            historicMoves[historicMoves.length - 1] = newMove;
        }
    }

    int getAuxiliaryData() {
        return auxiliaryData;
    }

    void setAuxiliaryData(int auxiliaryData) {
        this.auxiliaryData = auxiliaryData;
    }
}
