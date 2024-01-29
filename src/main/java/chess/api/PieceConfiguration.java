package chess.api;

import chess.api.pieces.*;
import com.google.common.collect.ComparisonChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

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

    private Integer enPassantSquare;

    private List<Integer> castlePositions = new ArrayList<>();

    private Side turnSide;

    private int halfMoveClock = 0;

    private int fullMoveNumber = 0;

    private int[] positionBitFlags = Position.POSITIONS.clone();

    private PieceConfiguration parentConfiguration;

    private final List<PieceConfiguration> childConfigurations = new ArrayList<>();

    private String algebraicNotation;

    public PieceConfiguration() {

    }

    public PieceConfiguration(PieceConfiguration copiedConfiguration, boolean copyPieces) {
        this.turnSide = copiedConfiguration.getTurnSide();
        this.halfMoveClock = copiedConfiguration.getHalfMoveClock();
        this.fullMoveNumber = copiedConfiguration.getFullMoveNumber();
        this.castlePositions = new ArrayList<>(copiedConfiguration.getCastlePositions());

        if (copyPieces) {
            Arrays.stream(Position.POSITIONS)
                    .forEach(pos -> positionBitFlags[pos] = BitUtil.applyBitFlag(positionBitFlags[pos],
                            copiedConfiguration.positionBitFlags[pos] & ALL_PIECE_AND_COLOUR_FLAGS_COMBINED));
        }
    }

    public List<PieceConfiguration> getPossiblePieceConfigurations() {
        positionBitFlags = stampCheckNonBlockerFlags(stampThreatFlags(stampOccupationFlags(positionBitFlags)));
        return Arrays.stream(getPieceBitFlags())
                .boxed()
                .filter(p -> BitUtil.hasBitFlag(p, PLAYER_OCCUPIED))
                .flatMap(p -> getPossiblePieceConfigurationsForPiece(p, false).stream())
                .collect(Collectors.toList());
    }

    public List<PieceConfiguration> getPossiblePieceConfigurationsForPiece(int pieceBitFlag, boolean linkOnwardConfigurations) {
        if (Arrays.stream(positionBitFlags).noneMatch(pbf -> pbf > 65535)) {
            positionBitFlags = stampCheckNonBlockerFlags(stampThreatFlags(stampOccupationFlags(positionBitFlags)));
        }
        return Piece.getPossibleMoves(pieceBitFlag, positionBitFlags, this, linkOnwardConfigurations);
    }

    private int[] stampOccupationFlags(int[] positions) {
        for(int pieceBitFlag : getPieceBitFlags()) {
            int occupationFlag = Piece.getSide(pieceBitFlag) == turnSide.ordinal() ? PLAYER_OCCUPIED : OPPONENT_OCCUPIED;
            int position = Position.getPosition(pieceBitFlag);
            positions[position] = BitUtil.applyBitFlag(positions[position], occupationFlag);
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
        for(int pieceBitFlag : getPieceBitFlags()) {
            if (Piece.getSide(pieceBitFlag) != turnSide.ordinal()) {
                positionBitFlags = Piece.stampThreatFlags(pieceBitFlag, positionBitFlags);
            }
        }
        return positionBitFlags;
    }

    private int[] stampCheckNonBlockerFlags(int[] positionBitFlags) {
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

            while(!BitUtil.hasBitFlag(currentPosition, PieceConfiguration.OPPONENT_OCCUPIED) && nextPositionIndex >= 0) {
                positionBitFlags[nextPositionIndex] = BitUtil.applyBitFlag(positionBitFlags[nextPositionIndex], PieceConfiguration.DOES_NOT_BLOCK_CHECK);
                currentPosition = positionBitFlags[nextPositionIndex];
                nextPositionIndex = Position.applyTranslationTowardsThreat(kingPositionDirectionalFlags, currentPosition);
            }

            if (kingPositionDirectionalFlags == PieceConfiguration.DIRECTION_ANY_KNIGHT) {
                // The king is checked only by a knight
                for(int[] directionalLimit : Knight.getKnightDirectionalLimits()) {
                    int possibleCheckingKnightPosition = Position.applyTranslation(currentPosition, directionalLimit[0], directionalLimit[1]);
                    if (possibleCheckingKnightPosition >= 0
                            && BitUtil.hasBitFlag(positionBitFlags[possibleCheckingKnightPosition],
                            PieceConfiguration.KNIGHT_OCCUPIED | PieceConfiguration.OPPONENT_OCCUPIED)) {
                        positionBitFlags[possibleCheckingKnightPosition] = BitUtil.applyBitFlag(
                                positionBitFlags[possibleCheckingKnightPosition], PieceConfiguration.DOES_NOT_BLOCK_CHECK);
                    }
                }
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
                .filter(pbf -> BitUtil.hasBitFlag(pbf, CHECK_FLAGS_COMBINED))
                .findFirst()
                .orElse(-1);
    }

    public int[] getPositionBitFlags() {
        return positionBitFlags;
    }

    public int countThreatFlags() {
        return (int) Arrays.stream(positionBitFlags)
                .filter(pbf -> BitUtil.hasBitFlag(pbf, PieceConfiguration.THREATENED))
                .count();
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
        int pieceFlag = getPieceAndColourBitFlags(pieceBitFlag);
        int position = Position.getPosition(pieceBitFlag);
        positionBitFlags[position] = BitUtil.applyBitFlag(positionBitFlags[position], pieceFlag);
    }

    public void removePiece(int pieceBitFlag) {
        int position = Position.getPosition(pieceBitFlag);
        positionBitFlags[position] = positionBitFlags[position] & (~ALL_PIECE_COLOUR_AND_OCCUPATION_FLAGS_COMBINED);
    }

    public int getPieceAtPosition(int positionBitFlag) {
        return positionBitFlags[Position.getPosition(positionBitFlag)];
    }

    public void promotePiece(int position, Class<? extends Piece> clazz) {
        int oldPieceBitFlag = getPieceAtPosition(position);
        int newPieceBitFlag = oldPieceBitFlag & ~ALL_PIECE_COLOUR_AND_OCCUPATION_FLAGS_COMBINED;
        if (clazz.equals(Queen.class)) {
            positionBitFlags[position] = newPieceBitFlag | QUEEN_OCCUPIED;
        } else if (clazz.equals(Knight.class)) {
            positionBitFlags[position] = newPieceBitFlag | KNIGHT_OCCUPIED;
        } else if (clazz.equals(Bishop.class)) {
            positionBitFlags[position] = newPieceBitFlag | BISHOP_OCCUPIED;
        } else if (clazz.equals(Rook.class)) {
            positionBitFlags[position] = newPieceBitFlag | ROOK_OCCUPIED;
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

    public Side getOpposingSide() {
        return turnSide == Side.WHITE ? Side.BLACK : Side.WHITE;
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
        childConfigurations.add(childConfiguration);
    }

    public String getAlgebraicNotation() {
        return algebraicNotation;
    }

    public void setAlgebraicNotation(String algebraicNotation) {
        this.algebraicNotation = algebraicNotation;
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

    public boolean isCheck() {
        return Arrays.stream(this.positionBitFlags)
                .anyMatch(position -> BitUtil.hasBitFlag(position, PieceConfiguration.PLAYER_OCCUPIED)
                        && BitUtil.hasBitFlag(position, PieceConfiguration.KING_OCCUPIED)
                        && BitUtil.hasBitFlag(position, PieceConfiguration.THREATENED));
    }

    public void logGameHistory() {
        StringBuilder sb = new StringBuilder();
        List<PieceConfiguration> gameHistory = getGameHistory();
        for (int i = 0; i < gameHistory.size(); i++) {
            PieceConfiguration pc = gameHistory.get(i);
            boolean whiteMoved = pc.getTurnSide() == Side.BLACK;
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
        return Arrays.stream(positionBitFlags)
                .filter(pbf -> BitUtil.hasBitFlag(pbf, PieceConfiguration.THREATENED))
                .count() / (double) 64;
    }

    public double getOpponentThreatenedScore() {
        return Arrays.stream(positionBitFlags)
                .filter(pbf -> BitUtil.hasBitFlag(pbf, PieceConfiguration.THREATENED)
                        && BitUtil.hasBitFlag(pbf, PieceConfiguration.OPPONENT_OCCUPIED))
                .count() / (double) 64;
    }

    public double getOpponentCentrePositionScore() {
        return Arrays.stream(CENTRE_POSITIONS)
                .filter(cp -> BitUtil.hasBitFlag(positionBitFlags[cp], PieceConfiguration.OPPONENT_OCCUPIED))
                .count() / (double) 4;
    }

    @Override
    public int compareTo(PieceConfiguration pieceConfiguration) {
        return ComparisonChain.start()
                .compare(PositionEvaluator.getValueDifferential(this),
                        PositionEvaluator.getValueDifferential(pieceConfiguration))
//                .compare(PositionEvaluator.getCentrePositionDifferential(this),
//                        PositionEvaluator.getCentrePositionDifferential(pieceConfiguration))
                .result();
    }
}
