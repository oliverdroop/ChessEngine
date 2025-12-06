package chess.api.configuration;

import chess.api.BitUtil;
import chess.api.Position;
import chess.api.pieces.Piece;

import java.util.Arrays;
import java.util.List;
import java.util.function.LongBinaryOperator;
import java.util.stream.Collectors;

import static chess.api.pieces.Piece.FAST_VALUE_ARRAY;

/**
 * This class holds board state using an array of 64-bit numbers.
 * Each bit of those 64-bit numbers corresponds to a board position, starting from a1 as the least-significant bit.
 * Each element in the data array corresponds to a separate property which squares can have,
 * such as piece types and threatened status.
 */
public class LongsPieceConfiguration extends PieceConfiguration {

    private static final int PLAYER_OCCUPATION_DATA_INDEX = 0;

    private static final int OPPONENT_OCCUPATION_DATA_INDEX = 1;

    private static final int WHITE_OCCUPATION_DATA_INDEX = 2;

    private static final int BLACK_OCCUPATION_DATA_INDEX = 3;

    private static final int KING_OCCUPATION_DATA_INDEX = 4;

    private static final int KNIGHT_OCCUPATION_DATA_INDEX = 5;

    private static final int BISHOP_OCCUPATION_DATA_INDEX = 6;

    private static final int ROOK_OCCUPATION_DATA_INDEX = 7;

    private static final int QUEEN_OCCUPATION_DATA_INDEX = 8;

    private static final int PAWN_OCCUPATION_DATA_INDEX = 9;

    public static final int THREATENED_DATA_INDEX = 10;

    public static final int DIRECTION_N_DATA_INDEX = 11;

    public static final int DIRECTION_NE_DATA_INDEX = 12;

    public static final int DIRECTION_E_DATA_INDEX = 13;

    public static final int DIRECTION_SE_DATA_INDEX = 14;

    public static final int DIRECTION_S_DATA_INDEX = 15;

    public static final int DIRECTION_SW_DATA_INDEX = 16;

    public static final int DIRECTION_W_DATA_INDEX = 17;

    public static final int DIRECTION_NW_DATA_INDEX = 18;

    public static final int DIRECTION_ANY_KNIGHT_DATA_INDEX = 19;

    public static final int DOES_NOT_BLOCK_CHECK_DATA_INDEX = 20;

    public static final int CASTLE_AVAILABLE_DATA_INDEX = 21;

    public static final int EN_PASSANT_SQUARE_DATA_INDEX = 22;

    private static final int DATA_LENGTH = 23;

    private static final int[] PLAYER_DATA_INDEXES = new int[]{
        PLAYER_OCCUPATION_DATA_INDEX,
        OPPONENT_OCCUPATION_DATA_INDEX
    };

    private static final int[] PIECE_DATA_INDEXES = new int[]{
        KING_OCCUPATION_DATA_INDEX,
        KNIGHT_OCCUPATION_DATA_INDEX,
        BISHOP_OCCUPATION_DATA_INDEX,
        ROOK_OCCUPATION_DATA_INDEX,
        QUEEN_OCCUPATION_DATA_INDEX,
        PAWN_OCCUPATION_DATA_INDEX
    };

    private static final int[] VALUABLE_PIECE_DATA_INDEXES = new int[]{
        KNIGHT_OCCUPATION_DATA_INDEX,
        BISHOP_OCCUPATION_DATA_INDEX,
        ROOK_OCCUPATION_DATA_INDEX,
        QUEEN_OCCUPATION_DATA_INDEX,
        PAWN_OCCUPATION_DATA_INDEX
    };

    private static final int[] PIECE_AND_COLOUR_DATA_INDEXES = new int[]{
        WHITE_OCCUPATION_DATA_INDEX,
        BLACK_OCCUPATION_DATA_INDEX,
        KING_OCCUPATION_DATA_INDEX,
        KNIGHT_OCCUPATION_DATA_INDEX,
        BISHOP_OCCUPATION_DATA_INDEX,
        ROOK_OCCUPATION_DATA_INDEX,
        QUEEN_OCCUPATION_DATA_INDEX,
        PAWN_OCCUPATION_DATA_INDEX
    };

    private static final int[] PLAYER_CHECK_DATA_INDEXES = new int[]{
        PLAYER_OCCUPATION_DATA_INDEX,
        KING_OCCUPATION_DATA_INDEX,
        THREATENED_DATA_INDEX
    };

    private static final int[] HIGHER_FLAG_DATA_INDEXES = new int[] {
        THREATENED_DATA_INDEX,
        DIRECTION_N_DATA_INDEX,
        DIRECTION_NE_DATA_INDEX,
        DIRECTION_E_DATA_INDEX,
        DIRECTION_SE_DATA_INDEX,
        DIRECTION_S_DATA_INDEX,
        DIRECTION_SW_DATA_INDEX,
        DIRECTION_W_DATA_INDEX,
        DIRECTION_NW_DATA_INDEX,
        DIRECTION_ANY_KNIGHT_DATA_INDEX,
        DOES_NOT_BLOCK_CHECK_DATA_INDEX,
        CASTLE_AVAILABLE_DATA_INDEX,
        EN_PASSANT_SQUARE_DATA_INDEX
    };

    private static final LongBinaryOperator OR_BINARY_OPERATOR = ((l1, l2) -> l1 | l2);

    private static final LongBinaryOperator AND_BINARY_OPERATOR = ((l1, l2) -> l1 & l2);

    private static final long[] STARTING_POSITION_PIECE_DATA = new long[10];

    static {
        STARTING_POSITION_PIECE_DATA[2] = 0b0000000000000000000000000000000000000000000000001111111111111111L;
        STARTING_POSITION_PIECE_DATA[3] = 0b1111111111111111000000000000000000000000000000000000000000000000L;
        STARTING_POSITION_PIECE_DATA[4] = 0b0001000000000000000000000000000000000000000000000000000000010000L;
        STARTING_POSITION_PIECE_DATA[5] = 0b0100001000000000000000000000000000000000000000000000000001000010L;
        STARTING_POSITION_PIECE_DATA[6] = 0b0010010000000000000000000000000000000000000000000000000000100100L;
        STARTING_POSITION_PIECE_DATA[7] = 0b1000000100000000000000000000000000000000000000000000000010000001L;
        STARTING_POSITION_PIECE_DATA[8] = 0b0000100000000000000000000000000000000000000000000000000000001000L;
        STARTING_POSITION_PIECE_DATA[9] = 0b0000000011111111000000000000000000000000000000001111111100000000L;
    }

    private final long[] data = new long[23];

    public LongsPieceConfiguration(){}

    public LongsPieceConfiguration(LongsPieceConfiguration copiedConfiguration, boolean copyPieces) {
        auxiliaryData = copiedConfiguration.auxiliaryData;

        if (copyPieces) {
            for(int dataIndex : PIECE_AND_COLOUR_DATA_INDEXES) {
                data[dataIndex] = copiedConfiguration.data[dataIndex];
            }
        }
    }

    @Override
    public Class<? extends PieceConfiguration> getConfigurationClass() {
        return LongsPieceConfiguration.class;
    }

    @Override
    public List<PieceConfiguration> getOnwardConfigurations() {
        setHigherBitFlags();
        return Arrays.stream(getPieceBitFlags())
            .boxed()
            .filter(p -> BitUtil.hasBitFlag(p, PLAYER_OCCUPIED))
            .flatMap(p -> getOnwardConfigurationsForPiece(p).stream())
            .collect(Collectors.toList());
    }

    @Override
    public List<PieceConfiguration> getOnwardConfigurationsForPiece(int pieceBitFlag) {
        if (combineDataWithOr(HIGHER_FLAG_DATA_INDEXES) == 0) {
            setHigherBitFlags();
        }
        return Piece.getPossibleMoves(pieceBitFlag, this);
    }

    @Override
    public int getValueDifferential() {
        int valueDifferential = 0;
        final int turnSide = getTurnSide();
        final int playerColourDataIndex = turnSide + 2;
        final int opponentColourDataIndex = 3 - turnSide;
        for(int dataIndex : VALUABLE_PIECE_DATA_INDEXES) {
            final int pieceTypeFlag = 1 << (dataIndex - 5);
            final int pieceValue = FAST_VALUE_ARRAY[pieceTypeFlag];
            final long playerData = data[dataIndex] & data[playerColourDataIndex];
            final long opponentData = data[dataIndex] & data[opponentColourDataIndex];
            valueDifferential += (Long.bitCount(playerData) - Long.bitCount(opponentData)) * pieceValue;
        }
        return valueDifferential;
    }

    @Override
    public double getLesserScore() {
        final int threatenedCount = Long.bitCount(data[THREATENED_DATA_INDEX]);
        final int threatenedPlayerCount = Long.bitCount(data[THREATENED_DATA_INDEX] & data[PLAYER_OCCUPATION_DATA_INDEX]);
        final int threatenedOpponentCount = Long.bitCount(data[THREATENED_DATA_INDEX] & data[OPPONENT_OCCUPATION_DATA_INDEX]);
        final int playerOccupiedCentreCount = Long.bitCount(data[PLAYER_OCCUPATION_DATA_INDEX] & 103481868288L);
        final int opponentOccupiedCentreCount = Long.bitCount(data[OPPONENT_OCCUPATION_DATA_INDEX] & 103481868288L);
        final int undevelopedPlayerCount = countUndevelopedPiecesBySide(getTurnSide());
        final int undevelopedOpponentCount = countUndevelopedPiecesBySide(getOpposingSide());
        return (threatenedCount * -0.00625)
            + (threatenedPlayerCount * -0.00625)
            + (threatenedOpponentCount * 0.00625)
            + (undevelopedPlayerCount * -0.00625)
            + (undevelopedOpponentCount * 0.00625)
            + (playerOccupiedCentreCount * 0.025)
            + (opponentOccupiedCentreCount * -0.025);
    }

    @Override
    public boolean isDeadPosition() {
        if (data[PAWN_OCCUPATION_DATA_INDEX] != 0L) {
            return false;
        }
        int totalMaterial = 0;
        for(int dataIndex : VALUABLE_PIECE_DATA_INDEXES) {
            totalMaterial += Long.bitCount(data[dataIndex]) * FAST_VALUE_ARRAY[1 << (dataIndex - 5)];
        }
        return totalMaterial <= 3;
    }

    @Override
    public boolean isCheck() {
        return combineDataWithAnd(PLAYER_CHECK_DATA_INDEXES) != 0;
    }

    @Override
    public boolean isPlayerOccupied(int position) {
        return isBitSetAtPosition(PLAYER_OCCUPATION_DATA_INDEX, position);
    }

    @Override
    public boolean isKingOccupied(int position) {
        return isBitSetAtPosition(KING_OCCUPATION_DATA_INDEX, position);
    }

    @Override
    public boolean isPlayerKingOccupied(int position) {
        return isBitSetAtPosition(data[PLAYER_OCCUPATION_DATA_INDEX] & data[KING_OCCUPATION_DATA_INDEX], position);
    }

    @Override
    public boolean isOpponentOccupied(int position) {
        return isBitSetAtPosition(OPPONENT_OCCUPATION_DATA_INDEX, position);
    }

    @Override
    public boolean isOpponentOccupiedOrEnPassantSquare(int position) {
        return isBitSetAtPosition(data[OPPONENT_OCCUPATION_DATA_INDEX] | data[EN_PASSANT_SQUARE_DATA_INDEX], position);
    }

    @Override
    public boolean isOpponentKnightOccupied(int position) {
        return isBitSetAtPosition(data[OPPONENT_OCCUPATION_DATA_INDEX] & data[KNIGHT_OCCUPATION_DATA_INDEX], position);
    }

    @Override
    public boolean isThreatened(int position) {
        return isBitSetAtPosition(THREATENED_DATA_INDEX, position);
    }

    @Override
    public void setThreatened(int position) {
        data[THREATENED_DATA_INDEX] |= (1L << position);
    }

    @Override
    public void setDirectionalFlag(int position, int directionalFlag) {
        final int dataIndex = Integer.numberOfTrailingZeros(directionalFlag) - 6;
        data[dataIndex] |= (1L << position);
    }

    @Override
    public boolean isIneffectiveCheckBlockAttempt(int position) {
        return isBitSetAtPosition(DOES_NOT_BLOCK_CHECK_DATA_INDEX, position);
    }

    @Override
    public boolean isCastleAvailable(int position) {
        return isBitSetAtPosition(CASTLE_AVAILABLE_DATA_INDEX, position);
    }

    @Override
    public void addPiece(int pieceData) {
        final int position = pieceData & 63;
        final int shiftedPieceData = pieceData >>> 6;
        for(int dataIndex = 0; dataIndex < THREATENED_DATA_INDEX; dataIndex++) {
            final long bitFlag = ((shiftedPieceData >>> dataIndex) & 1L) << position;
            data[dataIndex] |= bitFlag;
        }
    }

    @Override
    public void removePiece(int pieceData) {
        final int position = pieceData & 63;
        final long mask = ~(1L << position);
        for(int dataIndex = 0; dataIndex < DATA_LENGTH; dataIndex++) {
            data[dataIndex] &= mask;
        }
    }

    @Override
    public int getPieceAtPosition(int position) {
        return getDataAtPosition(position, 0, DATA_LENGTH) | position;
    }

    @Override
    protected int getPieceAndColourFlags(int position) {
        return getDataAtPosition(position, WHITE_OCCUPATION_DATA_INDEX, THREATENED_DATA_INDEX);
    }

    @Override
    public void setHigherBitFlags() {
        clearNonPieceData();
        stampOccupationData();
        stampThreatData();
        stampCheckNonBlockerData();
    }

    protected void setDoesNotBlockCheck(int position) {
        data[DOES_NOT_BLOCK_CHECK_DATA_INDEX] |= 1L << position;
    }

    int countUndevelopedPiecesBySide(int turnSide) {
        final int colourDataIndex = turnSide + 2;
        final long colourData = data[colourDataIndex];
        int count = 0;
        for(int dataIndex : PIECE_DATA_INDEXES) {
            count += Long.bitCount(
                data[dataIndex]
                    & colourData
                    & STARTING_POSITION_PIECE_DATA[dataIndex]
                    & STARTING_POSITION_PIECE_DATA[colourDataIndex]
            );
        }
        return count;
    }

    private int[] getPieceBitFlags() {
        final long combined = combineDataWithOr(PIECE_DATA_INDEXES);
        final int[] piecesData = new int[32];
        int pieceIndex = 0;
        for(int position = 0; position < 64; position++) {
            if (((1L << position) & combined) != 0) {
                piecesData[pieceIndex] = getPieceAtPosition(position);
                pieceIndex++;
            }
        }
        return Arrays.copyOfRange(piecesData, 0, pieceIndex);
    }

    private void stampOccupationData() {
        final int turnSide = getTurnSide();
        for(int playerIndex : PLAYER_DATA_INDEXES) {
            final int colourIndex = (turnSide ^ playerIndex) + 2;
            data[playerIndex] = data[colourIndex];
        }
        final int enPassantSquare = getEnPassantSquare();
        if (enPassantSquare >= 0) {
            data[EN_PASSANT_SQUARE_DATA_INDEX] = 1L << enPassantSquare;
        }
        for(int castlePosition : getCastlePositions()) {
            data[CASTLE_AVAILABLE_DATA_INDEX] |= 1L << castlePosition;
        }
    }

    private void stampThreatData() {
        for(int pieceBitFlag : getPieceBitFlags()) {
            if (Piece.getSide(pieceBitFlag) != getTurnSide()) {
                Piece.stampThreatFlags(pieceBitFlag, this);
            }
        }
    }

    private void stampCheckNonBlockerData() {
        final int checkedPlayerKingBitFlag = getCheckedPlayerKing();
        if (checkedPlayerKingBitFlag >= 0) {
            setCheckNonBlockerData(checkedPlayerKingBitFlag);
        }
    }

    private int getCheckedPlayerKing() {
        final long playerCheckedKingData = combineDataWithAnd(PLAYER_CHECK_DATA_INDEXES);
        if (playerCheckedKingData == 0) {
            return -1;
        }
        final int position = Long.numberOfTrailingZeros(playerCheckedKingData);
        return getPieceAtPosition(position);
    }

    private void setCheckNonBlockerData(int kingPositionBitFlag) {
        final int kingPositionDirectionalFlags = Piece.getDirectionalFlags(kingPositionBitFlag);
        if (Arrays.stream(ALL_DIRECTIONAL_FLAGS).anyMatch(df -> df == kingPositionDirectionalFlags)) {
            setCheckNonBlockerFlags(kingPositionBitFlag, kingPositionDirectionalFlags);
        }

        // Now reverse the bit DOES_NOT_BLOCK_CHECK data because it's on the squares which can block check
        data[DOES_NOT_BLOCK_CHECK_DATA_INDEX] = ~data[DOES_NOT_BLOCK_CHECK_DATA_INDEX];
    }

    private long combineDataWithOr(int... dataIndexes) {
        return combineData(OR_BINARY_OPERATOR, dataIndexes);
    }

    private long combineDataWithAnd(int... dataIndexes) {
        return combineData(AND_BINARY_OPERATOR, dataIndexes);
    }

    private long combineData(LongBinaryOperator binaryOperator, int... dataIndexes) {
        long accumulation = data[dataIndexes[0]];
        for(int dataIndexIndex = 1; dataIndexIndex < dataIndexes.length; dataIndexIndex++) {
            final int dataIndex = dataIndexes[dataIndexIndex];
            accumulation = binaryOperator.applyAsLong(accumulation, data[dataIndex]);
        }
        return accumulation;
    }

    private int getDataAtPosition(int position, int fromDataIndex, int toDataIndexExclusive) {
        int pieceData = 0;
        for(int dataIndex = fromDataIndex; dataIndex < toDataIndexExclusive; dataIndex++) {
            final long dataLong = data[dataIndex];
            pieceData |= (int) ((dataLong >>> position) & 1L) << dataIndex;
        }
        return pieceData << 6;
    }

    private void clearNonPieceData() {
        for(int dataIndex : HIGHER_FLAG_DATA_INDEXES) {
            data[dataIndex] = 0;
        }
    }

    private boolean isBitSetAtPosition(int dataIndex, int position) {
        return isBitSetAtPosition(data[dataIndex], position);
    }

    private boolean isBitSetAtPosition(long data, int position) {
        return (data & (1L << position)) != 0;
    }
}
