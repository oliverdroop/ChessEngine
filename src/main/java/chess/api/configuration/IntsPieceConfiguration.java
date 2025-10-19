package chess.api.configuration;

import chess.api.BitUtil;
import chess.api.Position;
import chess.api.pieces.Piece;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class IntsPieceConfiguration extends PieceConfiguration {

    private int[] positionBitFlags = Position.POSITIONS.clone();

    public IntsPieceConfiguration() {}

    public IntsPieceConfiguration(IntsPieceConfiguration copiedConfiguration, boolean copyPieces) {
        auxiliaryData = copiedConfiguration.auxiliaryData;

        if (copyPieces) {
            Arrays.stream(Position.POSITIONS)
                .forEach(pos -> positionBitFlags[pos] = BitUtil.applyBitFlag(positionBitFlags[pos],
                    copiedConfiguration.positionBitFlags[pos] & ALL_PIECE_AND_COLOUR_FLAGS_COMBINED));
        }
    }

    @Override
    public Class<? extends PieceConfiguration> getConfigurationClass() {
        return IntsPieceConfiguration.class;
    }

    @Override
    public void setHigherBitFlags() {
        clearNonPieceFlags(); // Necessary for nested evaluations
        stampOccupationFlags();
        stampThreatFlags();
        stampCheckNonBlockerFlags();
    }

    @Override
    public boolean isPlayerOccupied(int position) {
        return BitUtil.hasBitFlag(positionBitFlags[position], PieceConfiguration.PLAYER_OCCUPIED);
    }

    @Override
    public boolean isKingOccupied(int position) {
        return BitUtil.hasBitFlag(positionBitFlags[position], PieceConfiguration.KING_OCCUPIED);
    }

    @Override
    public boolean isPlayerKingOccupied(int position) {
        return BitUtil.hasBitFlag(positionBitFlags[position], PLAYER_KING_OCCUPIED);
    }

    @Override
    public boolean isOpponentOccupied(int position) {
        return BitUtil.hasBitFlag(positionBitFlags[position], PieceConfiguration.OPPONENT_OCCUPIED);
    }

    @Override
    public boolean isOpponentOccupiedOrEnPassantSquare(int position) {
        return BitUtil.hasAnyBits(positionBitFlags[position], OPPONENT_OCCUPIED | EN_PASSANT_SQUARE);
    }

    @Override
    public boolean isOpponentKnightOccupied(int position) {
        return BitUtil.hasBitFlag(positionBitFlags[position], KNIGHT_OCCUPIED | OPPONENT_OCCUPIED);
    }

    @Override
    public boolean isThreatened(int position) {
        return BitUtil.hasBitFlag(positionBitFlags[position], PieceConfiguration.THREATENED);
    }

    @Override
    public void setThreatened(int position) {
        positionBitFlags[position] = BitUtil.applyBitFlag(positionBitFlags[position], PieceConfiguration.THREATENED);
    }

    @Override
    public void setDirectionalFlag(int position, int directionalFlag) {
        positionBitFlags[position] = positionBitFlags[position] | directionalFlag;
    }

    @Override
    public boolean isIneffectiveCheckBlockAttempt(int position) {
        return BitUtil.hasBitFlag(positionBitFlags[position], PieceConfiguration.DOES_NOT_BLOCK_CHECK);
    }

    @Override
    public void setDoesNotBlockCheck(int position) {
        positionBitFlags[position] = BitUtil.applyBitFlag(positionBitFlags[position], DOES_NOT_BLOCK_CHECK);
    }

    @Override
    public boolean isCastleAvailable(int position) {
        return BitUtil.hasBitFlag(positionBitFlags[position], PieceConfiguration.CASTLE_AVAILABLE);
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
        if (Arrays.stream(positionBitFlags).noneMatch(pbf -> pbf > 65535)) {
            setHigherBitFlags();
        }
        return Piece.getPossibleMoves(pieceBitFlag, this);
    }

    @Override
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

    @Override
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

    @Override
    public boolean isCheck() {
        return Arrays.stream(this.positionBitFlags)
            .anyMatch(position -> BitUtil.hasBitFlag(position, CHECK_FLAGS_COMBINED));
    }

    @Override
    public int getPieceAtPosition(int positionBitFlag) {
        return positionBitFlags[Position.getPosition(positionBitFlag)];
    }

    @Override
    protected int getPieceAndColourFlags(int position) {
        return getPieceAndColourBitFlags(positionBitFlags[position]);
    }

    @Override
    public void addPiece(int pieceBitFlag) {
        final int pieceFlag = getPieceAndColourBitFlags(pieceBitFlag);
        final int position = Position.getPosition(pieceBitFlag);
        positionBitFlags[position] = BitUtil.applyBitFlag(positionBitFlags[position], pieceFlag);
    }

    @Override
    public void removePiece(int pieceBitFlag) {
        final int position = Position.getPosition(pieceBitFlag);
        positionBitFlags[position] = positionBitFlags[position] & (~ALL_PIECE_COLOUR_AND_OCCUPATION_FLAGS_COMBINED);
    }

    private void clearNonPieceFlags() {
        Arrays.stream(Position.POSITIONS).forEach(pos -> positionBitFlags[pos] = BitUtil.clearBits(positionBitFlags[pos], ~(63 | ALL_PIECE_AND_COLOUR_FLAGS_COMBINED)));
    }

    private void stampOccupationFlags() {
        for(int pieceBitFlag : getPieceBitFlags()) {
            int occupationFlag = Piece.getSide(pieceBitFlag) == getTurnSide() ? PLAYER_OCCUPIED : OPPONENT_OCCUPIED;
            int position = Position.getPosition(pieceBitFlag);
            positionBitFlags[position] = BitUtil.applyBitFlag(positionBitFlags[position], occupationFlag);
        }
        int enPassantSquare = getEnPassantSquare();
        if (enPassantSquare >= 0) {
            positionBitFlags[enPassantSquare] = BitUtil.applyBitFlag(positionBitFlags[enPassantSquare], EN_PASSANT_SQUARE);
        }
        for(int castlePosition : getCastlePositions()) {
            positionBitFlags[castlePosition] = BitUtil.applyBitFlag(positionBitFlags[castlePosition], CASTLE_AVAILABLE);
        }
    }

    private void stampThreatFlags() {
        for(int pieceBitFlag : getPieceBitFlags()) {
            if (Piece.getSide(pieceBitFlag) != getTurnSide()) {
                Piece.stampThreatFlags(pieceBitFlag, this);
            }
        }
    }

    private void stampCheckNonBlockerFlags() {
        final int checkedPlayerKingBitFlag = getCheckedPlayerKing(positionBitFlags);
        if (checkedPlayerKingBitFlag >= 0) {
            stampCheckNonBlockerPositionBitFlags(checkedPlayerKingBitFlag);
        }
    }

    private void stampCheckNonBlockerPositionBitFlags(int kingPositionBitFlag) {
        final int kingPositionDirectionalFlags = Piece.getDirectionalFlags(kingPositionBitFlag);
        if (Arrays.stream(ALL_DIRECTIONAL_FLAGS).anyMatch(df -> df == kingPositionDirectionalFlags)) {
            setCheckNonBlockerFlags(kingPositionBitFlag, kingPositionDirectionalFlags);
        }

        // Now reverse the bit DOES_NOT_BLOCK_CHECK bit flags because they're on the squares which can block check
        for(int position : Position.POSITIONS) {
            positionBitFlags[position] = BitUtil.reverseBitFlag(positionBitFlags[position], DOES_NOT_BLOCK_CHECK);
        }
    }

    private static int getPieceAndColourBitFlags(int positionBitFlag) {
        return positionBitFlag & (ALL_PIECE_AND_COLOUR_FLAGS_COMBINED);
    }

    private static int getCheckedPlayerKing(int[] positionBitFlags) {
        return Arrays.stream(positionBitFlags)
            .filter(pbf -> BitUtil.hasBitFlag(pbf, CHECK_FLAGS_COMBINED))
            .findFirst()
            .orElse(-1);
    }

    private int[] getPieceBitFlags() {
        return Arrays.stream(positionBitFlags).filter(pbf -> getPieceTypeBitFlag(pbf) != 0).toArray();
    }
}
