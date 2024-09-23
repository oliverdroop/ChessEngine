package chess.api;

import chess.api.pieces.*;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ComparisonChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static chess.api.BitUtil.*;

public class PieceConfiguration implements Comparable<PieceConfiguration> {

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

    private static final int[] ALL_DIRECTIONAL_FLAGS = new int[]{DIRECTION_N, DIRECTION_NE,
            DIRECTION_E, DIRECTION_SE, DIRECTION_S, DIRECTION_SW, DIRECTION_W, DIRECTION_NW, DIRECTION_ANY_KNIGHT};
    
    public static final int ALL_DIRECTIONAL_FLAGS_COMBINED = DIRECTION_N | DIRECTION_NE | DIRECTION_E | DIRECTION_SE
            | DIRECTION_S | DIRECTION_SW | DIRECTION_W | DIRECTION_NW | DIRECTION_ANY_KNIGHT;

    public static final int ALL_PIECE_FLAGS_COMBINED = KING_OCCUPIED | KNIGHT_OCCUPIED | BISHOP_OCCUPIED
            | ROOK_OCCUPIED | QUEEN_OCCUPIED | PAWN_OCCUPIED;

    public static final int ALL_PIECE_AND_COLOUR_FLAGS_COMBINED = ALL_PIECE_FLAGS_COMBINED | WHITE_OCCUPIED | BLACK_OCCUPIED;

    public static final int ALL_PIECE_COLOUR_AND_OCCUPATION_FLAGS_COMBINED = PLAYER_OCCUPIED | OPPONENT_OCCUPIED
            | ALL_PIECE_FLAGS_COMBINED | WHITE_OCCUPIED | BLACK_OCCUPIED;

    public static final int CHECK_FLAGS_COMBINED = PLAYER_OCCUPIED | KING_OCCUPIED | THREATENED;

    public static final int PLAYER_KING_OCCUPIED = KING_OCCUPIED | PLAYER_OCCUPIED;

    public static final int[] CENTRE_POSITIONS = {27, 28, 35, 36};

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

    private PieceConfiguration parentConfiguration;

    private List<PieceConfiguration> childConfigurations;

    private String algebraicNotation;

    public PieceConfiguration() {

    }

    public PieceConfiguration(PieceConfiguration copiedConfiguration, boolean copyPieces) {
        auxiliaryData = copiedConfiguration.auxiliaryData;

        if (copyPieces) {
            Arrays.stream(Position.POSITIONS)
                    .forEach(pos -> positionBitFlags[pos] = BitUtil.applyBitFlag(positionBitFlags[pos],
                            copiedConfiguration.positionBitFlags[pos] & ALL_PIECE_AND_COLOUR_FLAGS_COMBINED));
        }
    }

    public List<PieceConfiguration> getPossiblePieceConfigurations() {
        clearNonPieceFlags(); // Necessary for nested evaluations
        positionBitFlags = stampCheckNonBlockerFlags(stampThreatFlags(stampOccupationFlags(positionBitFlags)));
        return Arrays.stream(getPieceBitFlags())
                .boxed()
                .filter(p -> BitUtil.hasBitFlag(p, PLAYER_OCCUPIED))
                .flatMap(p -> getPossiblePieceConfigurationsForPiece(p, false).stream())
                .collect(Collectors.toList());
    }

    private void clearNonPieceFlags() {
        Arrays.stream(Position.POSITIONS).forEach(pos -> positionBitFlags[pos] = BitUtil.clearBits(positionBitFlags[pos], ~(63 | ALL_PIECE_AND_COLOUR_FLAGS_COMBINED)));
    }

    public List<PieceConfiguration> getPossiblePieceConfigurationsForPiece(int pieceBitFlag, boolean linkOnwardConfigurations) {
        if (Arrays.stream(positionBitFlags).noneMatch(pbf -> pbf > 65535)) {
            positionBitFlags = stampCheckNonBlockerFlags(stampThreatFlags(stampOccupationFlags(positionBitFlags)));
        }
        return Piece.getPossibleMoves(pieceBitFlag, positionBitFlags, this, linkOnwardConfigurations);
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
                for(int[] directionalLimit : Knight.getKnightDirectionalLimits()) {
                    int possibleCheckingKnightPosition = Position.applyTranslation(currentPosition, directionalLimit[0], directionalLimit[1]);
                    if (possibleCheckingKnightPosition >= 0
                            && BitUtil.hasBitFlag(positionBitFlags[possibleCheckingKnightPosition],
                            PieceConfiguration.KNIGHT_OCCUPIED | OPPONENT_OCCUPIED)) {
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

    public int[] getPositionBitFlags() {
        return positionBitFlags;
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
        final int newPieceBitFlag = oldPieceBitFlag & ~ALL_PIECE_COLOUR_AND_OCCUPATION_FLAGS_COMBINED;
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
        auxiliaryData = clearBits(auxiliaryData, (0b10 << index));
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

    public PieceConfiguration getParentConfiguration() {
        return parentConfiguration;
    }

    public void setParentConfiguration(PieceConfiguration parentConfiguration) {
        this.parentConfiguration = parentConfiguration;
    }

    public List<PieceConfiguration> getChildConfigurations() {
        return childConfigurations;
    }

    public void addChildConfiguration(PieceConfiguration childConfiguration) {
        if (childConfigurations == null) {
            childConfigurations = new ArrayList<>();
        }
        childConfigurations.add(childConfiguration);
    }

    public String toString() {
        return FENWriter.write(this);
    }

    public String getAlgebraicNotation() {
        return algebraicNotation;
    }

    public void setAlgebraicNotation(String algebraicNotation) {
        this.algebraicNotation = algebraicNotation;
    }

    @VisibleForTesting
    int getAuxiliaryData() {
        return auxiliaryData;
    }

    @VisibleForTesting
    void setAuxiliaryData(int auxiliaryData) {
        this.auxiliaryData = auxiliaryData;
    }

    public List<PieceConfiguration> getGameHistory() {
        List<PieceConfiguration> pcs = new ArrayList<>();
        PieceConfiguration pc = this;
        while(pc.getParentConfiguration() != null) {
            pcs.add(pc);
            pc = pc.getParentConfiguration();
        }

        List<PieceConfiguration> outputPcs = new ArrayList<>();
        for(int i = pcs.size() - 1; i >= 0; i--) {
            outputPcs.add(pcs.get(i));
        }
        return outputPcs;
    }

    public void logGameHistory() {
        StringBuilder sb = new StringBuilder();
        List<PieceConfiguration> gameHistory = getGameHistory();
        for (int i = 0; i < gameHistory.size(); i++) {
            PieceConfiguration pc = gameHistory.get(i);
            boolean whiteMoved = pc.getTurnSide() == 1;
            if (whiteMoved) {
                sb = new StringBuilder()
                        .append(pc.getFullMoveNumber())
                        .append(". ");
            }
            sb.append(pc.getAlgebraicNotation());
            if (whiteMoved && i < gameHistory.size() - 1) {
                sb.append(" ");
            } else {
                LOGGER.info(sb.toString());
            }
        }
    }

    public double getThreatScore() {
        return countThreatFlags() / (double) 64;
    }

    public int countThreatFlags() {
        return (int) Arrays.stream(positionBitFlags)
                .filter(pbf -> BitUtil.hasBitFlag(pbf, THREATENED))
                .count();
    }

    public double getLesserScore() {
        int squaresThreatened = 0;
        int playerPiecesThreatened = 0;
        int opponentPiecesCovered = 0;
        int midSquares = 0;
        for (int positionBitFlag : positionBitFlags) {
            if (BitUtil.hasBitFlag(positionBitFlag, THREATENED)) {
                squaresThreatened++;
                if (BitUtil.hasBitFlag(positionBitFlag, PLAYER_OCCUPIED)) {
                    playerPiecesThreatened++;
                } else if (BitUtil.hasBitFlag(positionBitFlag, OPPONENT_OCCUPIED)) {
                    opponentPiecesCovered++;
                }
            }
            if (BitUtil.hasBitFlag(positionBitFlag, 27)
                    || BitUtil.hasBitFlag(positionBitFlag, 28)
                    || BitUtil.hasBitFlag(positionBitFlag, 35)
                    || BitUtil.hasBitFlag(positionBitFlag, 36)) {
                if (BitUtil.hasBitFlag(positionBitFlag, PLAYER_OCCUPIED)) {
                    midSquares++;
                } else if (BitUtil.hasBitFlag(positionBitFlag, OPPONENT_OCCUPIED)) {
                    midSquares--;
                }
            }
        }
        return - (squaresThreatened / (double) 160)
                - (playerPiecesThreatened / (double) 80)
                - (opponentPiecesCovered / (double) 80)
                + (midSquares / (double) 20);
    }

    @Override
    public int compareTo(PieceConfiguration pieceConfiguration) {
        return ComparisonChain.start()
                .compare(PositionEvaluator.getValueDifferential(this),
                        PositionEvaluator.getValueDifferential(pieceConfiguration))
                .result();
    }
}
