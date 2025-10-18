package chess.api.configuration;

import chess.api.BitUtil;
import chess.api.Position;
import chess.api.pieces.Knight;
import chess.api.pieces.Piece;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.LongBinaryOperator;
import java.util.stream.Collectors;

import static chess.api.BitUtil.hasBitFlag;
import static chess.api.pieces.King.CASTLE_POSITION_MAPPINGS;

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

    private static final int[] PLAYER_DATA_INDEXES = new int[]{
        PLAYER_OCCUPATION_DATA_INDEX,
        OPPONENT_OCCUPATION_DATA_INDEX
    };

    private static final int[] COLOUR_DATA_INDEXES = new int[]{
        WHITE_OCCUPATION_DATA_INDEX,
        BLACK_OCCUPATION_DATA_INDEX
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

    private static final int[] NON_PIECE_OR_COLOUR_DATA_INDEXES = new int[]{
        PLAYER_OCCUPATION_DATA_INDEX,
        OPPONENT_OCCUPATION_DATA_INDEX,
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

    private static final int[][] PIECE_COLOUR_AND_OCCUPATION_DATA_INDEXES = new int[][]{
        PLAYER_DATA_INDEXES, COLOUR_DATA_INDEXES, PIECE_DATA_INDEXES
    };

    private static final LongBinaryOperator OR_BINARY_OPERATOR = ((l1, l2) -> l1 | l2);

    private static final LongBinaryOperator AND_BINARY_OPERATOR = ((l1, l2) -> l1 & l2);

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

    public static LongsPieceConfiguration toNewLongsConfigurationFromMoves(LongsPieceConfiguration originalConfiguration, short[] historicMoves) {
        LongsPieceConfiguration currentConfiguration = originalConfiguration;
        for (short historicMove : historicMoves) {
            currentConfiguration = toNewLongsConfigurationFromMove(currentConfiguration, historicMove);
        }
        return currentConfiguration;
    }

    public static LongsPieceConfiguration toNewLongsConfigurationFromMove(LongsPieceConfiguration previousConfiguration, short moveDescription) {
        final LongsPieceConfiguration newConfiguration = new LongsPieceConfiguration(previousConfiguration, true);
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
        int diff = 0;
        final int turnSide = getTurnSide();
        final int playerColourDataIndex = turnSide + 2;
        final int opponentColourDataIndex = 3 - turnSide;
        for(int dataIndex : VALUABLE_PIECE_DATA_INDEXES) {
            final int pieceTypeFlag = 1 << (dataIndex - 5);
            final int pieceValue = Piece.FAST_VALUE_ARRAY[pieceTypeFlag];
            final long playerData = data[dataIndex] & data[playerColourDataIndex];
            final long opponentData = data[dataIndex] & data[opponentColourDataIndex];
            diff += (Long.bitCount(playerData) - Long.bitCount(opponentData)) * pieceValue;
        }
        return diff;
    }

    @Override
    public double getLesserScore() {
        final int threatenedCount = Long.bitCount(data[THREATENED_DATA_INDEX]);
        final int threatenedPlayerCount = Long.bitCount(data[THREATENED_DATA_INDEX] & data[PLAYER_OCCUPATION_DATA_INDEX]);
        final int threatenedOpponentCount = Long.bitCount(data[THREATENED_DATA_INDEX] & data[OPPONENT_OCCUPATION_DATA_INDEX]);
        final int playerOccupiedCentreCount = Long.bitCount(data[PLAYER_OCCUPATION_DATA_INDEX] & 103481868288L);
        final int opponentOccupiedCentreCount = Long.bitCount(data[OPPONENT_OCCUPATION_DATA_INDEX] & 103481868288L);
        return (threatenedCount * -0.00625)
            + (threatenedPlayerCount * -0.00625)
            + (threatenedOpponentCount * 0.00625)
            + (playerOccupiedCentreCount * -0.05)
            + (opponentOccupiedCentreCount * 0.05);
    }

    @Override
    public boolean isCheck() {
        return combineDataWithAnd(PLAYER_CHECK_DATA_INDEXES) != 0;
    }

    @Override
    public boolean isPlayerOccupied(int position) {
        return (data[PLAYER_OCCUPATION_DATA_INDEX] & (1L << position)) != 0;
    }

    @Override
    public boolean isKingOccupied(int position) {
        return (data[KING_OCCUPATION_DATA_INDEX] & (1L << position)) != 0;
    }

    @Override
    public boolean isPlayerKingOccupied(int position) {
        return ((data[PLAYER_OCCUPATION_DATA_INDEX] & data[KING_OCCUPATION_DATA_INDEX]) & (1L << position)) != 0;
    }

    @Override
    public boolean isOpponentOccupied(int position) {
        return (data[OPPONENT_OCCUPATION_DATA_INDEX] & (1L << position)) != 0;
    }

    @Override
    public boolean isOpponentOccupiedOrEnPassantSquare(int position) {
        return ((data[OPPONENT_OCCUPATION_DATA_INDEX] | data[EN_PASSANT_SQUARE_DATA_INDEX]) & (1L << position)) != 0;
    }

    @Override
    public boolean isThreatened(int position) {
        return (data[THREATENED_DATA_INDEX] & (1L << position)) != 0;
    }

    @Override
    public void setThreatened(int position) {
        data[THREATENED_DATA_INDEX] = data[THREATENED_DATA_INDEX] | (1L << position);
    }

    @Override
    public void setDirectionalFlag(int position, int directionalFlag) {
        final int dataIndex = Integer.numberOfTrailingZeros(directionalFlag) - 6;
        data[dataIndex] = data[dataIndex] | (1L << position);
    }

    @Override
    public boolean isCheckBlockingOrNoCheck(int position) {
        return (data[DOES_NOT_BLOCK_CHECK_DATA_INDEX] & (1L << position)) == 0;
    }

    @Override
    public boolean isCastleAvailable(int position) {
        return (data[CASTLE_AVAILABLE_DATA_INDEX] & (1L << position)) != 0;
    }

    @Override
    public void addPiece(int pieceData) {
        final int position = pieceData & 63;
        for(int[] possibleDataIndexes : PIECE_COLOUR_AND_OCCUPATION_DATA_INDEXES) {
            final int dataIndex = getDataIndex(pieceData, possibleDataIndexes);
            if (dataIndex >= 0) {
                data[dataIndex] = data[dataIndex] | (1L << position);
            }
        }
    }

    @Override
    public void removePiece(int pieceData) {
        final int position = pieceData & 63;
        for(int dataIndex = 0; dataIndex < data.length; dataIndex++) {
            data[dataIndex] = data[dataIndex] & ~(1L << position);
        }
    }

    @Override
    public int getPieceAtPosition(int position) {
        int pieceData = 0;
        for(int dataIndex = 0; dataIndex < data.length; dataIndex++) {
            final long dataLong = data[dataIndex];
            pieceData |= (int) ((dataLong >>> position) & 1L) << (dataIndex + 6);
        }
        return pieceData | position;
    }

    @Override
    public void setHigherBitFlags() {
        clearNonPieceFlags();
        stampOccupationData();
        stampThreatData();
        stampCheckNonBlockerData();
    }

    @Override
    public <T extends PieceConfiguration> String getAlgebraicNotation(T previousConfiguration) {
        return "";
    }

    private int[] getPieceBitFlags() {
        final long combined = combineDataWithOr(PIECE_DATA_INDEXES);
        return Arrays.stream(Position.POSITIONS)
            .filter(pos -> ((1L << pos) & combined) != 0)
            .map(this::getPieceAtPosition)
            .toArray();
    }

    private void stampOccupationData() {
        final int turnSide = getTurnSide();
        final long pieceTypeData = combineDataWithOr(PIECE_DATA_INDEXES);
        for(int playerIndex : PLAYER_DATA_INDEXES) {
            final int colourIndex = (turnSide ^ playerIndex) + 2;
            final long colourData = data[colourIndex];
            data[playerIndex] = pieceTypeData & colourData;
        }
        final int enPassantSquare = getEnPassantSquare();
        if (enPassantSquare >= 0) {
            data[EN_PASSANT_SQUARE_DATA_INDEX] = 1L << enPassantSquare;
        }
        for(int castlePosition : getCastlePositions()) {
            data[CASTLE_AVAILABLE_DATA_INDEX] = data[CASTLE_AVAILABLE_DATA_INDEX] | (1L << castlePosition);
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
            // The king is only checked from one direction
            int currentPosition = kingPositionBitFlag;
            int nextPositionIndex = Position.applyTranslationTowardsThreat(kingPositionDirectionalFlags, currentPosition);

            while(!BitUtil.hasBitFlag(getPieceAtPosition(currentPosition), OPPONENT_OCCUPIED) && nextPositionIndex >= 0) {
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

        // Now reverse the bit DOES_NOT_BLOCK_CHECK data because it's on the squares which can block check
        data[DOES_NOT_BLOCK_CHECK_DATA_INDEX] = ~data[DOES_NOT_BLOCK_CHECK_DATA_INDEX];
    }

    private void setDoesNotBlockCheck(int position) {
        data[DOES_NOT_BLOCK_CHECK_DATA_INDEX] = data[DOES_NOT_BLOCK_CHECK_DATA_INDEX] | (1L << position);
    }

    private boolean isOpponentKnightOccupied(int position) {
        return ((data[OPPONENT_OCCUPATION_DATA_INDEX] & data[KNIGHT_OCCUPATION_DATA_INDEX]) & (1L << position)) != 0;
    }

    private long combineDataWithOr(int[] dataIndexes) {
        return combineData(dataIndexes, OR_BINARY_OPERATOR);
    }

    private long combineDataWithAnd(int[] dataIndexes) {
        return combineData(dataIndexes, AND_BINARY_OPERATOR);
    }

    private long combineData(int[] dataIndexes, LongBinaryOperator binaryOperator) {
        return Arrays.stream(dataIndexes)
            .mapToLong(i -> data[i])
            .reduce(binaryOperator)
            .orElse(0);
    }

    private int getDataIndex(int pieceData, int[] possibleIndexes) {
        for(int dataIndex : possibleIndexes) {
            if ((getMask(dataIndex) & pieceData) != 0) {
                return dataIndex;
            }
        }
        return -1;
    }

    private int getMask(int dataIndex) {
        return (1 << dataIndex) << 6;
    }

    private void clearNonPieceFlags() {
        for(int dataIndex : NON_PIECE_OR_COLOUR_DATA_INDEXES) {
            data[dataIndex] = 0;
        }
    }
}
