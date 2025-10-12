package chess.api;

import chess.api.pieces.Knight;
import chess.api.pieces.Piece;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static chess.api.BitUtil.hasBitFlag;
import static chess.api.pieces.King.CASTLE_POSITION_MAPPINGS;
import static chess.api.pieces.Pawn.PROMOTION_PIECE_TYPES;

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

    public static IntsPieceConfiguration toNewIntsConfigurationFromMoves(IntsPieceConfiguration originalConfiguration, short[] historicMoves) {
        IntsPieceConfiguration currentConfiguration = originalConfiguration;
        for (short historicMove : historicMoves) {
            currentConfiguration = toNewIntsConfigurationFromMove(currentConfiguration, historicMove);
        }
        return currentConfiguration;
    }

    public static IntsPieceConfiguration toNewIntsConfigurationFromMove(IntsPieceConfiguration previousConfiguration, short moveDescription) {
        final IntsPieceConfiguration newConfiguration = new IntsPieceConfiguration(previousConfiguration, true);
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

    @Override
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

    public List<PieceConfiguration> getOnwardConfigurationsForPiece(int pieceBitFlag) {
        if (Arrays.stream(positionBitFlags).noneMatch(pbf -> pbf > 65535)) {
            setHigherBitFlags();
        }
        return Piece.getPossibleMoves(pieceBitFlag, positionBitFlags, this);
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
    public String getAlgebraicNotation(PieceConfiguration previousConfiguration) {
        if (!(previousConfiguration instanceof IntsPieceConfiguration previousConfigurationImpl)) {
            return null;
        }
        boolean capturing = false;
        int previousBitFlag = Integer.MIN_VALUE;
        int currentBitFlag = Integer.MIN_VALUE;
        for(int pos : Position.POSITIONS) {
            int currentPieceOnPosition = positionBitFlags[pos] & ALL_PIECE_AND_COLOUR_FLAGS_COMBINED;
            int previousPieceOnPosition = previousConfigurationImpl.positionBitFlags[pos] & ALL_PIECE_AND_COLOUR_FLAGS_COMBINED;
            int currentColour = currentPieceOnPosition & COLOUR_FLAGS_COMBINED;
            int previousColour = previousPieceOnPosition & COLOUR_FLAGS_COMBINED;
            if (currentPieceOnPosition == previousPieceOnPosition) {
                continue;
            }
            if (currentPieceOnPosition == 0 && !BitUtil.hasBitFlag(previousBitFlag, KING_OCCUPIED)) {
                // Moving from this position
                previousBitFlag = previousConfigurationImpl.positionBitFlags[pos];
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
        final int checkedPlayerKingBitFlag = getCheckedPlayerKing(positionBitFlags);
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
