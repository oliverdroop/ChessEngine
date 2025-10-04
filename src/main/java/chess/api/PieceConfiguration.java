package chess.api;

import chess.api.pieces.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static chess.api.BitUtil.*;
import static chess.api.pieces.King.CASTLE_POSITION_MAPPINGS;
import static chess.api.pieces.Pawn.PROMOTION_PIECE_TYPES;

public class PieceConfiguration {

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

    private static final int COLOUR_FLAGS_COMBINED = WHITE_OCCUPIED | BLACK_OCCUPIED;

    public static final int NO_CAPTURE_OR_PAWN_MOVE_LIMIT = 99;

    private static final int[] ALL_DIRECTIONAL_FLAGS = {
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
    private int auxiliaryData = Integer.MIN_VALUE;

    private int[] positionBitFlags = Position.POSITIONS.clone();

    private short[] historicMoves;

    public PieceConfiguration() {}

    public PieceConfiguration(PieceConfiguration copiedConfiguration, boolean copyPieces) {
        auxiliaryData = copiedConfiguration.auxiliaryData;

        if (copyPieces) {
            Arrays.stream(Position.POSITIONS)
                    .forEach(pos -> positionBitFlags[pos] = BitUtil.applyBitFlag(positionBitFlags[pos],
                            copiedConfiguration.positionBitFlags[pos] & ALL_PIECE_AND_COLOUR_FLAGS_COMBINED));
        }
    }

    public static PieceConfiguration toNewConfigurationFromMoves(PieceConfiguration originalConfiguration, short[] historicMoves) {
        PieceConfiguration currentConfiguration = originalConfiguration;
        for (short historicMove : historicMoves) {
            currentConfiguration = toNewConfigurationFromMove(currentConfiguration, historicMove);
        }
        return currentConfiguration;
    }

    public static PieceConfiguration toNewConfigurationFromMove(PieceConfiguration previousConfiguration, short moveDescription) {
        final PieceConfiguration newConfiguration = new PieceConfiguration(previousConfiguration, true);
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

    public void setHigherBitFlags() {
        clearNonPieceFlags(); // Necessary for nested evaluations
        positionBitFlags = stampCheckNonBlockerFlags(stampThreatFlags(stampOccupationFlags(positionBitFlags)));
    }

    public List<PieceConfiguration> getOnwardConfigurations() {
        setHigherBitFlags();
        return Arrays.stream(getPieceBitFlags())
                .boxed()
                .filter(p -> BitUtil.hasBitFlag(p, PLAYER_OCCUPIED))
                .flatMap(p -> getOnwardConfigurationsForPiece(p).stream())
                .collect(Collectors.toList());
    }

    private void clearNonPieceFlags() {
        Arrays.stream(Position.POSITIONS).forEach(pos -> positionBitFlags[pos] = BitUtil.clearBits(positionBitFlags[pos], ~(63 | ALL_PIECE_AND_COLOUR_FLAGS_COMBINED)));
    }

    public List<PieceConfiguration> getOnwardConfigurationsForPiece(int pieceBitFlag) {
        if (Arrays.stream(positionBitFlags).noneMatch(pbf -> pbf > 65535)) {
            setHigherBitFlags();
        }
        return Piece.getPossibleMoves(pieceBitFlag, positionBitFlags, this);
    }

    private int[] stampOccupationFlags(int[] positions) {
        for(int pieceBitFlag : getPieceBitFlags()) {
            int occupationFlag = Piece.getSide(pieceBitFlag) == getTurnSide() ? PLAYER_OCCUPIED : OPPONENT_OCCUPIED;
            int position = Position.getPosition(pieceBitFlag);
            positions[position] = BitUtil.applyBitFlag(positions[position], occupationFlag);
        }
        int enPassantSquare = getEnPassantSquare();
        if (enPassantSquare >= 0) {
            positions[enPassantSquare] = BitUtil.applyBitFlag(positions[enPassantSquare], EN_PASSANT_SQUARE);
        }
        for(int castlePosition : getCastlePositions()) {
            positions[castlePosition] = BitUtil.applyBitFlag(positions[castlePosition], CASTLE_AVAILABLE);
        }
        return positions;
    }

    private int[] stampThreatFlags(int[] positionBitFlags) {
        for(int pieceBitFlag : getPieceBitFlags()) {
            if (Piece.getSide(pieceBitFlag) != getTurnSide()) {
                positionBitFlags = Piece.stampThreatFlags(pieceBitFlag, positionBitFlags);
            }
        }
        return positionBitFlags;
    }

    private static int[] stampCheckNonBlockerFlags(int[] positionBitFlags) {
        final int checkedPlayerKingBitFlag = isPlayerInCheck(positionBitFlags);
        if (checkedPlayerKingBitFlag >= 0) {
            return getCheckNonBlockerPositionBitFlags(checkedPlayerKingBitFlag, positionBitFlags);
        }
        return positionBitFlags;
    }

    private static int[] getCheckNonBlockerPositionBitFlags(int kingPositionBitFlag, int[] positionBitFlags) {
        final int kingPositionDirectionalFlags = Piece.getDirectionalFlags(kingPositionBitFlag);
        if (Arrays.stream(ALL_DIRECTIONAL_FLAGS).anyMatch(df -> df == kingPositionDirectionalFlags)) {
            // The king is only checked from one direction
            int currentPosition = kingPositionBitFlag;
            int nextPositionIndex = Position.applyTranslationTowardsThreat(kingPositionDirectionalFlags, currentPosition);

            while(!BitUtil.hasBitFlag(currentPosition, OPPONENT_OCCUPIED) && nextPositionIndex >= 0) {
                positionBitFlags[nextPositionIndex] = BitUtil.applyBitFlag(positionBitFlags[nextPositionIndex], DOES_NOT_BLOCK_CHECK);
                currentPosition = positionBitFlags[nextPositionIndex];
                nextPositionIndex = Position.applyTranslationTowardsThreat(kingPositionDirectionalFlags, currentPosition);
            }

            if (kingPositionDirectionalFlags == DIRECTION_ANY_KNIGHT) {
                // The king is checked only by a knight
                for(int[] directionalLimit : Knight.getDirectionalLimits()) {
                    int possibleCheckingKnightPosition = Position.applyTranslation(currentPosition, directionalLimit[0], directionalLimit[1]);
                    if (possibleCheckingKnightPosition >= 0
                            && BitUtil.hasBitFlag(positionBitFlags[possibleCheckingKnightPosition], KNIGHT_OCCUPIED | OPPONENT_OCCUPIED)) {
                        // Stamp the checking knight's position so it becomes a movable position
                        positionBitFlags[possibleCheckingKnightPosition] = BitUtil.applyBitFlag(
                                positionBitFlags[possibleCheckingKnightPosition], DOES_NOT_BLOCK_CHECK);
                    }
                }
            }
        }

        // Now reverse the bit DOES_NOT_BLOCK_CHECK bit flags because they're on the squares which can block check
        for(int position : Position.POSITIONS) {
            positionBitFlags[position] = BitUtil.reverseBitFlag(positionBitFlags[position], DOES_NOT_BLOCK_CHECK);
        }
        return positionBitFlags;
    }

    public int getValueDifferential() {
        int valueDifferential = 0;
        final int turnSide = getTurnSide();
        for (int positionBitFlag : positionBitFlags) {
            // Is it a piece?
            final int pieceBitFlag = positionBitFlag & PieceConfiguration.ALL_PIECE_FLAGS_COMBINED;
            if (pieceBitFlag == 0) {
                continue;
            }
            final int value = Piece.getValue(pieceBitFlag);
            // Is it a black piece?
            final int isBlackOccupied = (positionBitFlag & PieceConfiguration.BLACK_OCCUPIED) >> 9;
            // Is it a player or opposing piece?
            final int turnSideFactor = 1 - ((turnSide ^ isBlackOccupied) << 1);
            valueDifferential += value * turnSideFactor;
        }
        return valueDifferential;
    }

    public static int isPlayerInCheck(int[] positionBitFlags) {
        return Arrays.stream(positionBitFlags)
                .filter(pbf -> BitUtil.hasBitFlag(pbf, CHECK_FLAGS_COMBINED))
                .findFirst()
                .orElse(-1);
    }

    public boolean isCheck() {
        return Arrays.stream(this.positionBitFlags)
                .anyMatch(position -> BitUtil.hasBitFlag(position, CHECK_FLAGS_COMBINED));
    }

    public int[] getPieceBitFlags() {
        return Arrays.stream(positionBitFlags).filter(pbf -> getPieceTypeBitFlag(pbf) != 0).toArray();
    }

    public static int getPieceAndColourBitFlags(int positionBitFlag) {
        return positionBitFlag & (ALL_PIECE_AND_COLOUR_FLAGS_COMBINED);
    }

    public static int getPieceTypeBitFlag(int positionBitFlag) {
        return positionBitFlag & ALL_PIECE_FLAGS_COMBINED;
    }

    public void addPiece(int pieceBitFlag) {
        final int pieceFlag = getPieceAndColourBitFlags(pieceBitFlag);
        final int position = Position.getPosition(pieceBitFlag);
        positionBitFlags[position] = BitUtil.applyBitFlag(positionBitFlags[position], pieceFlag);
    }

    public void removePiece(int pieceBitFlag) {
        final int position = Position.getPosition(pieceBitFlag);
        positionBitFlags[position] = positionBitFlags[position] & (~ALL_PIECE_COLOUR_AND_OCCUPATION_FLAGS_COMBINED);
    }

    public int getPieceAtPosition(int positionBitFlag) {
        return positionBitFlags[Position.getPosition(positionBitFlag)];
    }

    public void promotePiece(int position, int pieceTypeFlag) {
        final int oldPieceBitFlag = getPieceAtPosition(position);
        final int newPieceBitFlag = oldPieceBitFlag & ~ALL_PIECE_FLAGS_COMBINED;
        positionBitFlags[position] = newPieceBitFlag | pieceTypeFlag;
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

    public String toString() {
        return FENWriter.write(this);
    }

    public String getAlgebraicNotation(PieceConfiguration previousConfiguration) {
        boolean capturing = false;
        int previousBitFlag = Integer.MIN_VALUE;
        int currentBitFlag = Integer.MIN_VALUE;
        for(int pos : Position.POSITIONS) {
            int currentPieceOnPosition = positionBitFlags[pos] & ALL_PIECE_AND_COLOUR_FLAGS_COMBINED;
            int previousPieceOnPosition = previousConfiguration.positionBitFlags[pos] & ALL_PIECE_AND_COLOUR_FLAGS_COMBINED;
            int currentColour = currentPieceOnPosition & COLOUR_FLAGS_COMBINED;
            int previousColour = previousPieceOnPosition & COLOUR_FLAGS_COMBINED;
            if (currentPieceOnPosition == previousPieceOnPosition) {
                continue;
            }
            if (currentPieceOnPosition == 0 && !BitUtil.hasBitFlag(previousBitFlag, KING_OCCUPIED)) {
                // Moving from this position
                previousBitFlag = previousConfiguration.positionBitFlags[pos];
            } else if (previousPieceOnPosition == 0 && !BitUtil.hasBitFlag(currentBitFlag, KING_OCCUPIED)) {
                // Moving to this position
                currentBitFlag = positionBitFlags[pos];
            } else if (currentColour != 0 && previousColour != 0 && currentColour != previousColour) {
                currentBitFlag = positionBitFlags[pos];
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

    public short[] getHistoricMoves() {
        return historicMoves;
    }

    public void setHistoricMoves(short[] historicMoves) {
        this.historicMoves = historicMoves;
    }

    public void addHistoricMove(PieceConfiguration previousConfiguration, short newMove) {
        if (previousConfiguration != null && previousConfiguration.getHistoricMoves() != null) {
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

    public double getLesserScore() {
        double lesserScore = 0;
        for (int positionBitFlag : positionBitFlags) {
            if (BitUtil.hasBitFlag(positionBitFlag, THREATENED)) {
                lesserScore -= 0.00625;
                lesserScore -= ((positionBitFlag >> 6) & 1) * 0.00625; // Count threatened player pieces
                lesserScore += ((positionBitFlag >> 7) & 1) * 0.00625; // Count threatened opponent pieces
            }
            if (BitUtil.hasBitFlag(positionBitFlag, 27)
                    | BitUtil.hasBitFlag(positionBitFlag, 28)
                    | BitUtil.hasBitFlag(positionBitFlag, 35)
                    | BitUtil.hasBitFlag(positionBitFlag, 36)) {
                lesserScore -= ((positionBitFlag >> 6) & 1) * 0.05; // Count player-occupied centre squares
                lesserScore += ((positionBitFlag >> 7) & 1) * 0.05; // Count opponent-occupied centre squares
            }
        }
        return lesserScore;
    }
}
