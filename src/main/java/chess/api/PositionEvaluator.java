package chess.api;

import chess.api.pieces.Piece;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class PositionEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PositionEvaluator.class);

    private static final int NO_CAPTURE_OR_PAWN_MOVE_LIMIT = 99;

    public static int getValueDifferential(PieceConfiguration pieceConfiguration) {
        int valueDifferential = 0;
        final int turnSide = pieceConfiguration.getTurnSide();
        for (int positionBitFlag : pieceConfiguration.getPositionBitFlags()) {
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

    public static PieceConfiguration getBestMoveRecursively(PieceConfiguration pieceConfiguration, int depth) {
        Optional<Object[]> optionalBestEntry = getBestPieceConfigurationToScoreEntryRecursively(pieceConfiguration, depth);
        return (PieceConfiguration) optionalBestEntry.map(obj -> obj[0]).orElse(null);
    }

    static double getBestScoreDifferentialRecursively(PieceConfiguration pieceConfiguration, int depth) {
        // The entry object below consists of a PieceConfiguration and a Double representing the score
        Optional<Object[]> optionalBestEntry = getBestPieceConfigurationToScoreEntryRecursively(pieceConfiguration, depth);
        if (optionalBestEntry.isPresent()) {
            Object[] bestEntry = optionalBestEntry.get();
            return ((double) bestEntry[1]);
        } else if (pieceConfiguration.isCheck()) {
            // Checkmate
            return Float.MAX_VALUE;
        }
        // Stalemate
        return -Float.MAX_VALUE;
    }

    private static boolean isFiftyMoveRuleFailure(PieceConfiguration pieceConfiguration) {
        return pieceConfiguration.getHalfMoveClock() > NO_CAPTURE_OR_PAWN_MOVE_LIMIT;
    }

    static Optional<Object[]> getBestPieceConfigurationToScoreEntryRecursively(PieceConfiguration pieceConfiguration, int depth) {
        final double currentDiff = getValueDifferential(pieceConfiguration);

        depth--;
        final List<PieceConfiguration> onwardPieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();
        final int onwardConfigurationCount = onwardPieceConfigurations.size();
        final double[] onwardConfigurationScores = new double[onwardConfigurationCount];
        final boolean[] fiftyMoveRuleChecks = new boolean[onwardConfigurationCount];
        for (int i = 0; i < onwardConfigurationCount; i++) {
            PieceConfiguration onwardPieceConfiguration = onwardPieceConfigurations.get(i);

            fiftyMoveRuleChecks[i] = isFiftyMoveRuleFailure(onwardPieceConfiguration);

            double nextDiff = getValueDifferential(onwardPieceConfiguration);
            double comparison = currentDiff - nextDiff;
            if (depth > 0) {
                comparison += getBestScoreDifferentialRecursively(onwardPieceConfiguration, depth) * 0.99; // This modifier adjusts for uncertainty at depth
                // Below is where the position can be evaluated for more than just the value differential (because the position bit flags have been calculated)
            }
            onwardConfigurationScores[i] = comparison;
        }

        final double threatValue = pieceConfiguration.getLesserScore();
        int bestOnwardConfigurationIndex = -1;
        double bestOnwardConfigurationScore = -Double.MAX_VALUE;
        for(int i = 0; i < onwardConfigurationCount; i++) {
            double onwardConfigurationScore = onwardConfigurationScores[i] + threatValue;
            if (onwardConfigurationScore > bestOnwardConfigurationScore && !fiftyMoveRuleChecks[i]) {
                bestOnwardConfigurationScore = onwardConfigurationScore;
                bestOnwardConfigurationIndex = i;
            }
        }

        if (bestOnwardConfigurationIndex >= 0) {
            PieceConfiguration bestOnwardConfiguration = onwardPieceConfigurations.get(bestOnwardConfigurationIndex);
            return Optional.of(new Object[]{bestOnwardConfiguration, -bestOnwardConfigurationScore});
        }
        return Optional.empty();
    }

    public static GameEndType deriveGameEndType(PieceConfiguration finalConfiguration) {
        if (finalConfiguration.isCheck() || finalConfiguration.getHalfMoveClock() == NO_CAPTURE_OR_PAWN_MOVE_LIMIT) {
            return GameEndType.values()[1 - finalConfiguration.getTurnSide()];
        } else {
            return GameEndType.STALEMATE;
        }
    }
}
