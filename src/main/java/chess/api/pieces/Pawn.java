package chess.api.pieces;

import chess.api.BitUtil;
import chess.api.PieceConfiguration;
import chess.api.Position;
import chess.api.Side;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.logging.log4j.util.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Pawn extends Piece{

    private static final int[][] WHITE_DIRECTIONAL_LIMITS = {{-1, 1, 1}, {0, 1, 2}, {1, 1, 1}};

    private static final int[][] BLACK_DIRECTIONAL_LIMITS = {{1, -1, 1}, {0, -1, 2}, {-1, -1, 1}};

    private static final Map<Class<? extends Piece>, String> PROMOTION_CLASSES = ImmutableMap.of(Knight.class, "N", Bishop.class, "B", Rook.class, "R", Queen.class, "Q");

    public Pawn(Side side, int position) {
        super(side, PieceType.PAWN, position);
    }

    @Override
    public int[][] getDirectionalLimits() {
        return getSide() == Side.WHITE ? WHITE_DIRECTIONAL_LIMITS : BLACK_DIRECTIONAL_LIMITS;
    }

    @Override
    public List<PieceConfiguration> getPossibleMoves(int[] positionBitFlags, PieceConfiguration currentConfiguration, boolean linkOnwardConfigurations) {
        List<PieceConfiguration> pieceConfigurations = new ArrayList<>();
        for(int[] directionalLimit : getMovableDirectionalLimits(positionBitFlags)) {
            int directionX = directionalLimit[0];
            int directionY = directionalLimit[1];
            int limit = directionalLimit[2];
            int testPositionIndex = getPosition();

            while (limit > 0) {
                testPositionIndex = Position.applyTranslation(testPositionIndex, directionX, directionY);
                if (testPositionIndex < 0 || testPositionIndex >= 64) {
                    break;
                }

                // Is this player piece blocked by another player piece?
                if (BitUtil.hasBitFlag(positionBitFlags[testPositionIndex], PieceConfiguration.PLAYER_OCCUPIED)) {
                    break;
                }

                // Is this position a position which wouldn't block an existing checking direction?
                if (BitUtil.hasBitFlag(positionBitFlags[testPositionIndex], PieceConfiguration.DOES_NOT_BLOCK_CHECK)) {
                    limit--;
                    continue;
                }

                // Is there an opponent piece on the position?
                Piece takenPiece = null;
                if (BitUtil.hasBitFlag(positionBitFlags[testPositionIndex], PieceConfiguration.OPPONENT_OCCUPIED)) {
                    if (directionX != 0) {
                        // This is a diagonal move so taking a piece is valid
                        takenPiece = currentConfiguration.getPieceAtPosition(testPositionIndex);
                    } else {
                        // This is a straight forward move so taking a piece is not possible
                        break;
                    }
                }

                addNewPieceConfigurations(pieceConfigurations, currentConfiguration, testPositionIndex, takenPiece, linkOnwardConfigurations);

                if (takenPiece != null) {
                    // Stop considering moves beyond this taken piece
                    break;
                }
                limit--;
            }
        }
        return pieceConfigurations;
    }

    @Override
    public int[] stampThreatFlags(int[] positionBitFlags) {
        int[] directionalLimitThreatIndexes = {0, 2};
        for(int i : directionalLimitThreatIndexes) {
            int[] directionalLimit = getDirectionalLimits()[i];
            int directionX = directionalLimit[0];
            int directionY = directionalLimit[1];
            int testPositionIndex = getPosition();
            testPositionIndex = Position.applyTranslation(testPositionIndex, directionX, directionY);
            if (testPositionIndex < 0 || testPositionIndex >= 64) {
                continue;
            }

            positionBitFlags[testPositionIndex] = BitUtil.applyBitFlag(positionBitFlags[testPositionIndex], PieceConfiguration.THREATENED);
        }
        return positionBitFlags;
    }

    @Override
    protected int[][] getMovableDirectionalLimits(int[] positionBitFlags) {
        boolean leftDiagonalAvailable = isDiagonalMoveAvailable(0, positionBitFlags);
        boolean rightDiagonalAvailable = isDiagonalMoveAvailable(2, positionBitFlags);
        int startIndex = leftDiagonalAvailable ? 0 : 1;
        int endIndex = rightDiagonalAvailable ? 3 : 2;
        int[][] movableDirectionalLimits = arrayDeepCopy(getDirectionalLimits(), startIndex, endIndex);
        if (!isOnStartingRank()) {
            movableDirectionalLimits[1 - startIndex][2] = 1;
        }

        int directionalBitFlags = getDirectionalFlags(positionBitFlags[getPosition()]);
        if (directionalBitFlags != 0) {
            return restrictDirections(movableDirectionalLimits, directionalBitFlags);
        }
        return movableDirectionalLimits;
    }

    @Override
    protected void addNewPieceConfigurations(List<PieceConfiguration> pieceConfigurations,
            PieceConfiguration currentConfiguration, int newPiecePosition, Piece takenPiece, boolean linkOnwardConfigurations) {
        super.addNewPieceConfigurations(pieceConfigurations, currentConfiguration, newPiecePosition, takenPiece, linkOnwardConfigurations);
        int translation = newPiecePosition - getPosition();
        PieceConfiguration newPieceConfiguration = pieceConfigurations.get(pieceConfigurations.size() - 1);
        if (Math.abs(translation) == 16) {
            // Set the en passant square
            newPieceConfiguration.setEnPassantSquare(getPosition() + (translation / 2));
        } else if (Position.getY(newPiecePosition) == 7 - (getSide().ordinal() * 7)) {
            // Promote pawn
            pieceConfigurations.remove(pieceConfigurations.size() - 1);
            currentConfiguration.getChildConfigurations().remove(newPieceConfiguration);
            for(Class<? extends Piece> promotionClass : PROMOTION_CLASSES.keySet()) {
                PieceConfiguration promotedPawnConfiguration = getPromotedPawnConfiguration(newPieceConfiguration,
                        newPiecePosition, promotionClass);
                if (linkOnwardConfigurations) {
                    linkPromotedPieceConfigurations(currentConfiguration, promotedPawnConfiguration, newPiecePosition,
                            takenPiece, PROMOTION_CLASSES.get(promotionClass).toLowerCase());
                }
                pieceConfigurations.add(promotedPawnConfiguration);
            }
        }
    }

    protected void linkPromotedPieceConfigurations(PieceConfiguration currentConfiguration, PieceConfiguration newConfiguration,
                                           int newPiecePosition, Piece takenPiece, String promotionTo) {
        currentConfiguration.addChildConfiguration(newConfiguration);
        newConfiguration.setParentConfiguration(currentConfiguration);
        newConfiguration.setAlgebraicNotation(getAlgebraicNotation(getPosition(), newPiecePosition, takenPiece != null, promotionTo));
    }

    private PieceConfiguration getPromotedPawnConfiguration(PieceConfiguration unpromotedPawnConfiguration,
            int newPiecePosition, Class<? extends Piece> clazz) {
        PieceConfiguration newPieceConfiguration = new PieceConfiguration(unpromotedPawnConfiguration, true);
        newPieceConfiguration.promotePiece(newPiecePosition, clazz);
        return newPieceConfiguration;
    }

    private boolean isOnStartingRank() {
        return Position.getY(getPosition()) - (getSide().ordinal() * 5) == 1;
    }

    private boolean isDiagonalMoveAvailable(int directionalLimitIndex, int[] positionBitFlags) {
        int testPosition = Position.applyTranslation(getPosition(),
                getDirectionalLimits()[directionalLimitIndex][0], getDirectionalLimits()[directionalLimitIndex][1]);
        if (testPosition < 0) {
            return false;
        }
        return BitUtil.hasBitFlag(positionBitFlags[testPosition], PieceConfiguration.OPPONENT_OCCUPIED)
                || BitUtil.hasBitFlag(positionBitFlags[testPosition], PieceConfiguration.EN_PASSANT_SQUARE);
    }

    public char getFENCode() {
        return (char) (80 + (getSide().ordinal() * 32));
    }

    @Override
    public String getANCode() {
        return Strings.EMPTY;
    }

    public int getValue() {
        return 1;
    }
}
