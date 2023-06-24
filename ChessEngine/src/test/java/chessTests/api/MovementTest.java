package chessTests.api;

import chess.api.FENReader;
import chess.api.PieceConfiguration;
import chess.api.Position;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.function.IntPredicate;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(BlockJUnit4ClassRunner.class)
public class MovementTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MovementTest.class);

    @Test
    public void testBishopMovement() {
        PieceConfiguration pieceConfiguration = FENReader.read("B7/8/8/8/8/8/8/8 w - - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of forward piece configurations returned")
                .hasSize(7);
    }

    @Test
    public void testBishopMovement_withOpponentPiecesBlocking() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/8/2p1p3/3B4/2p1p3/8/8/8 w - - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of forward piece configurations returned")
                .hasSize(4);
    }

    @Test
    public void testBishopMovement_withPlayerPiecesBlocking() {
        PieceConfiguration pieceConfiguration = FENReader.read("nNRRRRRR/NBRRRRRR/RRRRRRRR/RRRRRRRR/RRRRRRRR/RRRRRRRR/RRRRRRRR/RRRRRRRR w - - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of forward piece configurations returned")
                .hasSize(1);
    }

    @Test
    public void testKnightMovement() {
        PieceConfiguration pieceConfiguration = FENReader.read("N7/8/8/8/8/8/8/8 w - - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of forward piece configurations returned")
                .hasSize(2);
    }

    @Test
    public void testApplyTranslation_withIllegalNegXNegYTranslation() {
        int position = 8;

        int newPosition = Position.applyTranslation(position, -1, -1);

        assertThat(newPosition).as("Unexpected value for translation which should be illegal").isEqualTo(-1);
    }

    @Test
    public void testApplyTranslation_withLegalNegYTranslation() {
        int position = 8;

        int newPosition = Position.applyTranslation(position, 0, -1);

        assertThat(newPosition).as("Unexpected value for translation which should be legal").isEqualTo(0);
    }

    @Test
    public void testApplyTranslation_withIllegalPosXPosYTranslation() {
        int position = 55;

        int newPosition = Position.applyTranslation(position, 1, 1);

        assertThat(newPosition).as("Unexpected value for translation which should be illegal").isEqualTo(-1);
    }

    @Test
    public void testApplyTranslation_withLegalPosXTranslation() {
        int position = 62;

        int newPosition = Position.applyTranslation(position, 1, 0);

        assertThat(newPosition).as("Unexpected value for translation which should be illegal").isEqualTo(63);
    }

    @Test
    public void testApplyTranslation_withLegalPosYTranslation() {
        int position = 7;

        int newPosition = Position.applyTranslation(position, 0, 7);

        assertThat(newPosition).as("Unexpected value for translation which should be legal").isEqualTo(63);
    }

    @Test
    public void testMovement_whenProtectingKingWithKnight() {
        PieceConfiguration pieceConfiguration = FENReader.read("KP6/PN6/8/8/8/8/8/7b w - - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Expected no moves to be available because knight protects king")
                .isEmpty();
    }

    @Test
    public void testMovement_whenProtectingKingWithBishop() {
        PieceConfiguration pieceConfiguration = FENReader.read("KP6/PB6/8/8/8/8/8/7b w - - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of moves available to bishop protecting king")
                .hasSize(6);
    }

    @Test
    public void testMovement_whenKingDoublyProtected() {
        PieceConfiguration pieceConfiguration = FENReader.read("KP6/PN6/2N5/8/8/8/8/7b w - - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of moves available when king is doubly protected")
                .hasSize(10);
    }

    @Test
    public void testPawnMovement_fromStartingPosition() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/8/8/8/8/8/1P6/8 w - - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Expected two possible moves for pawn in starting position")
                .hasSize(2);
    }

    @Test
    public void testPawnMovement_fromNonStartingPositionWithoutTargets() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/8/8/8/8/1P6/8/8 w - - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Expected one possible move for pawn not in starting position")
                .hasSize(1);
    }

    @Test
    public void testPawnMovement_fromStartingPositionWithDistantBlockingOpponent() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/8/8/8/1p6/8/1P6/8 w - - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Expected one possible move for pawn with opponent blocking two squares forward")
                .hasSize(1);
    }

    @Test
    public void testPawnMovement_fromStartingPositionWithCloseBlockingOpponent() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/8/8/8/8/1p6/1P6/8 w - - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Expected no possible moves for pawn with opponent immediately blocking")
                .isEmpty();
    }

    @Test
    public void testPawnMovement_fromStartingPositionWithTwoTakeableOpponents() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/8/8/8/8/p1p5/1P6/8 w - - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves for pawn on starting rank with two takeable pawns")
                .hasSize(4);
    }

    @Test
    public void testPawnMovement_whenProtectingKingFromDistantOpponent() {
        PieceConfiguration pieceConfiguration = FENReader.read("7b/8/8/8/8/8/1P6/KP6 w - - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when pawn is blocking check")
                .hasSize(1);
    }

    @Test
    public void testPawnMovement_whenProtectingKingFromCloseOpponent() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/8/8/8/8/2b5/1P6/KP6 w - - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when pawn is blocking check from takeable bishop")
                .hasSize(2);
    }

    @Test
    public void testPawnMovement_withEnPassantAvailable() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/8/8/3Pp3/8/8/8/8 w - e6 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when en passant is possible")
                .hasSize(2);
    }

    @Test
    public void testKingMovement_withPawnPreventingTaking() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/2p5/1r6/K7/8/8/8/8 w - - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Only one move should be available because the rook is protected by a pawn")
                .hasSize(1);
    }

    @Test
    public void testKingMovement_withTwoCastlesAvailable() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/8/8/8/8/8/P6P/R3K2R w KQ - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Both castling positions should be available")
                .hasSize(16);
    }

    @Test
    public void testKingMovement_withNoCastlesAvailable() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/8/8/8/8/8/P6P/R3K2R w - - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when neither castling position is available")
                .hasSize(14);
    }

    @Test
    public void testKingMovement_withCastleThroughCheck() {
        PieceConfiguration pieceConfiguration = FENReader.read("3r1r2/8/8/8/8/8/P6P/R3K2R w KQ - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when attempting to castle through check")
                .hasSize(10);
    }

    @Test
    public void testKingMovement_withCastleOutOfCheck() {
        PieceConfiguration pieceConfiguration = FENReader.read("4r3/8/8/8/8/8/P6P/R3K2R w KQ - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when attempting to castle out of check")
                .hasSize(4);
    }

    @Test
    public void testKingMovement_withCastleIntoCheck() {
        PieceConfiguration pieceConfiguration = FENReader.read("2r3r1/8/8/8/8/8/P6P/R3K2R w KQ - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when attempting to castle into check")
                .hasSize(14);
    }

    @Test
    public void testKingMovement_withCastleBlockedByPlayerPiece() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/8/8/8/8/8/P6P/RN2K1NR w KQ - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when queen's castle blocked by knight")
                .hasSize(15);
    }

    @Test
    public void testKingMovement_withCastleBlockedByOpponentPiece() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/8/8/8/8/8/P6P/Rn2K1nR w KQ - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when queen's castle blocked by knight")
                .hasSize(9);
    }

    @Test
    public void testStartingPosition() {
        PieceConfiguration pieceConfiguration = FENReader.read("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves from starting position")
                .hasSize(20);
    }

    @Test
    public void testPawnPromotion() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/3P4/8/8/8/8/8/8 w - - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves for promoted pawn")
                .hasSize(4);
    }

    @Test
    public void testStalemate() {
        PieceConfiguration pieceConfiguration = FENReader.read("1r6/8/8/8/8/8/7r/K7 w - - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves for cornered king")
                .isEmpty();
    }

    @Test
    public void testInCheck_whenOnlyKnightCanBlock() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/8/8/8/8/N7/PPP5/K6r w - - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when only the knight can block check")
                .hasSize(1);
    }

    @Test
    public void testInCheck_whenOnlyBishopCanBlock() {
        PieceConfiguration pieceConfiguration = FENReader.read("qq6/q6B/8/8/8/8/8/7K w - - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when only the bishop can block check")
                .hasSize(1);
    }

    @Test
    public void testInCheck_whenPawnTakeCanBlock() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/PP6/K5rr/PP5P/8/8/8/8 w - - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when only taking the rook with the pawn can block")
                .hasSize(2);
    }

    @Test
    public void testInCheck_whenOnlyPawnCanBlock() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/PP6/K5rr/PP4P1/8/8/8/8 w - - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when pawn cannot take checking rook")
                .hasSize(1);
    }

    @Test
    public void testInCheck_whenPawnMovingBehindKingDoesNotBlock() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/PP5r/1K5r/PP5r/8/8/8/8 w - - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when a pawn moving behind the king does not block check")
                .isEmpty();
    }

    @Test
    public void testInCheck_whenBishopCannotBlockQueenAndRook() {
        PieceConfiguration pieceConfiguration = FENReader.read("qq6/q6B/8/8/8/8/8/r6K w - - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when the king is pinned down and checked from 2 angles")
                .isEmpty();
    }

    @Test
    public void testInCheck_whenBishopCannotBlockKnight() {
        PieceConfiguration pieceConfiguration = FENReader.read("qqq4B/q7/8/8/8/5K2/8/6n1 w - - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when the king is pinned down and checked by a knight and a queen")
                .isEmpty();
    }

    @Test
    public void testInCheck_whenBothDirectionsCheckedByBishops() {
        PieceConfiguration pieceConfiguration = FENReader.read("7b/b7/8/3P4/2PKP3/3P4/8/8 w - - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when the king is pinned down by two bishops")
                .isEmpty();
    }

    @Test
    public void testInCheck_whenMovingPawnToProtectWouldExposeKingToRook() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/b7/8/3PP3/r1PKP3/2PP4/8/8 w - - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when moving the pawn to protect would expose to a rook")
                .isEmpty();
    }

    @Test
    public void testInCheck_tryingToMoveAwayFromCheckingRook() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/8/8/8/8/8/PPP5/1K5r w - - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when the king has been check-mated")
                .isEmpty();
    }

    @Test
    public void testInCheck_tryingToMoveAwayFromCheckingPawn() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/8/5p2/4p3/3K4/8/8/8 w - - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when the king has been checked by a pawn")
                .hasSize(7);
    }

    @Test
    public void testInCheck_tryingToMoveAwayFromCheckingKnight() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/8/8/8/8/1nn5/8/K2n4 w - - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when the king has been checked and cornered by knights")
                .isEmpty();
    }

    @Test
    public void testInCheck_whenKingCannotMoveButPawnCanTakeChecker() {
        PieceConfiguration pieceConfiguration = FENReader.read("7k/6pp/6N1/8/8/8/B7/8 b - - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when the king has been checked by a takeable knight")
                .hasSize(1);
    }

    @Test
    public void testInCheck_whenOpposingPieceBlocksTakingCheckingPiece() {
        PieceConfiguration pieceConfiguration = FENReader.read("4k3/8/8/q2BQ/8/8/8/7K b - - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();
        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when the king has been checked by an untakeable queen")
                .hasSize(3);
    }

    @Test
    public void testInCheck_whenOpposingPieceDoesNotBlockTakingCheckingPiece() {
        PieceConfiguration pieceConfiguration = FENReader.read("4k3/8/8/q3Q/8/8/8/7K b - - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();
        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when the king has been checked by an takeable queen")
                .hasSize(5);
    }

    @Test
    public void testConfinedMovement_withOrthogonalDirectionalBitFlag() {
        PieceConfiguration pieceConfiguration = FENReader.read("3rkr2/4q3/8/8/4P3/8/4K3/8 w - - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();
        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when the king and pawn can only move in one plane")
                .hasSize(3);
    }

    @Test
    public void testConfinedMovement_withDiagonalDirectionalBitFlag_pawnTakes() {
        PieceConfiguration pieceConfiguration = FENReader.read("4kbb1/7b/5b1b/4b3/3P4/8/1K6/8 w - - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();
        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when the king and pawn can only move in one plane")
                .hasSize(3);
    }

    @Test
    public void testConfinedMovement_withDiagonalDirectionalBitFlag_pawnPinned() {
        PieceConfiguration pieceConfiguration = FENReader.read("4kbb1/7b/5b1b/8/3P4/8/1K6/8 w - - 0 1");

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getPossiblePieceConfigurations();
        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when the king can only move in one plane")
                .hasSize(2);
    }
}