package chess.api.configuration;

import chess.api.FENWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static chess.api.BitUtil.*;
import static chess.api.configuration.IntsPieceConfiguration.toNewIntsConfigurationFromMove;

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

    public abstract <T extends PieceConfiguration> List<T> getOnwardConfigurations();

    public abstract <T extends PieceConfiguration> List<T> getOnwardConfigurationsForPiece(int pieceBitFlag);

    public abstract int getValueDifferential();

    public abstract double getLesserScore();

    public abstract boolean isCheck();

    public abstract void addPiece(int pieceData);

    public abstract void removePiece(int pieceData);

    public abstract int getPieceAtPosition(int position);

    public abstract boolean isPlayerOccupied(int position);

    public abstract boolean isKingOccupied(int position);

    public abstract boolean isPlayerKingOccupied(int position);

    public abstract boolean isOpponentOccupied(int position);

    public abstract boolean isOpponentOccupiedOrEnPassantSquare(int position);

    public abstract boolean isThreatened(int position);

    public abstract void setThreatened(int position);

    public abstract void setDirectionalFlag(int position, int directionalFlag);

    public abstract boolean isCheckBlockingOrNoCheck(int position);

    public abstract boolean isCastleAvailable(int position);

    public abstract void setHigherBitFlags();

    public abstract <T extends PieceConfiguration> String getAlgebraicNotation(T previousConfiguration);

    public static PieceConfiguration toNewConfigurationFromMoves(PieceConfiguration originalConfiguration, short[] historicMoves) {
        if (originalConfiguration instanceof IntsPieceConfiguration originalConfigurationImpl) {
            return IntsPieceConfiguration.toNewIntsConfigurationFromMoves(originalConfigurationImpl, historicMoves);
        } else if (originalConfiguration instanceof LongsPieceConfiguration originalConfigurationImpl) {
            return LongsPieceConfiguration.toNewLongsConfigurationFromMoves(originalConfigurationImpl, historicMoves);
        }
        return null;
    }

    public static PieceConfiguration toNewConfigurationFromMove(PieceConfiguration previousConfiguration, short move) {
        if (previousConfiguration instanceof IntsPieceConfiguration previousConfigurationImpl) {
            return toNewIntsConfigurationFromMove(previousConfigurationImpl, move);
        } else if (previousConfiguration instanceof LongsPieceConfiguration previousConfigurationImpl) {
            return LongsPieceConfiguration.toNewLongsConfigurationFromMove(previousConfigurationImpl, move);
        }
        return null;
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
}
