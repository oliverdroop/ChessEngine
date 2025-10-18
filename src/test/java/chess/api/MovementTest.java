package chess.api;

import chess.api.configuration.IntsPieceConfiguration;
import chess.api.configuration.LongsPieceConfiguration;
import chess.api.configuration.PieceConfiguration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MovementTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MovementTest.class);

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testBishopMovement(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest("B7/8/8/8/8/8/8/8 w - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of forward piece configurations returned")
                .hasSize(7);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testBishopMovement_withOpponentPiecesBlocking(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest("8/8/2p1p3/3B4/2p1p3/8/8/8 w - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of forward piece configurations returned")
                .hasSize(4);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testBishopMovement_withPlayerPiecesBlocking(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest(
            "nNRRRRRR/NBRRRRRR/RRRRRRRR/RRRRRRRR/RRRRRRRR/RRRRRRRR/RRRRRRRR/RRRRRRRR w - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of forward piece configurations returned")
                .hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testKnightMovement(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest("N7/8/8/8/8/8/8/8 w - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of forward piece configurations returned")
                .hasSize(2);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testMovement_whenProtectingKingWithKnight(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest("KP6/PN6/8/8/8/8/8/7b w - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Expected no moves to be available because knight protects king")
                .isEmpty();
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testMovement_whenProtectingKingWithBishop(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest("KP6/PB6/8/8/8/8/8/7b w - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of moves available to bishop protecting king")
                .hasSize(6);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testMovement_whenKingDoublyProtected(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest("KP6/PN6/2N5/8/8/8/8/7b w - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of moves available when king is doubly protected")
                .hasSize(10);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testPawnMovement_fromStartingPosition(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest("8/8/8/8/8/8/1P6/8 w - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Expected two possible moves for pawn in starting position")
                .hasSize(2);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testPawnMovement_fromNonStartingPositionWithoutTargets(
            Class<? extends PieceConfiguration> configurationClass)
    {
        PieceConfiguration pieceConfiguration = setupTest("8/8/8/8/8/1P6/8/8 w - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Expected one possible move for pawn not in starting position")
                .hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testPawnMovement_fromStartingPositionWithDistantBlockingOpponent(
            Class<? extends PieceConfiguration> configurationClass)
    {
        PieceConfiguration pieceConfiguration = setupTest("8/8/8/8/1p6/8/1P6/8 w - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Expected one possible move for pawn with opponent blocking two squares forward")
                .hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testPawnMovement_fromStartingPositionWithCloseBlockingOpponent(
            Class<? extends PieceConfiguration> configurationClass)
    {
        PieceConfiguration pieceConfiguration = setupTest("8/8/8/8/8/1p6/1P6/8 w - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Expected no possible moves for pawn with opponent immediately blocking")
                .isEmpty();
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testPawnMovement_fromStartingPositionWithTwoTakeableOpponents(
            Class<? extends PieceConfiguration> configurationClass)
    {
        PieceConfiguration pieceConfiguration = setupTest("8/8/8/8/8/p1p5/1P6/8 w - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves for pawn on starting rank with two takeable pawns")
                .hasSize(4);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testPawnMovement_whenProtectingKingFromDistantOpponent(
            Class<? extends PieceConfiguration> configurationClass)
    {
        PieceConfiguration pieceConfiguration = setupTest("7b/8/8/8/8/8/1P6/KP6 w - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when pawn is blocking check")
                .hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testPawnMovement_whenProtectingKingFromCloseOpponent(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest("8/8/8/8/8/2b5/1P6/KP6 w - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when pawn is blocking check from takeable bishop")
                .hasSize(2);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testPawnMovement_withEnPassantAvailable(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest("8/8/8/3Pp3/8/8/8/8 w - e6 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when en passant is possible")
                .hasSize(2);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testKingMovement_withPawnPreventingTaking(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest("8/2p5/1r6/K7/8/8/8/8 w - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Only one move should be available because the rook is protected by a pawn")
                .hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testKingMovement_withTwoCastlesAvailable(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest("8/8/8/8/8/8/P6P/R3K2R w KQ - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Both castling positions should be available")
                .hasSize(16);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testKingMovement_withNoCastlesAvailable(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest("8/8/8/8/8/8/P6P/R3K2R w - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when neither castling position is available")
                .hasSize(14);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testKingMovement_withCastleThroughCheck(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest("3r1r2/8/8/8/8/8/P6P/R3K2R w KQ - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when attempting to castle through check")
                .hasSize(10);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testKingMovement_withCastleOutOfCheck(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest("4r3/8/8/8/8/8/P6P/R3K2R w KQ - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when attempting to castle out of check")
                .hasSize(4);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testKingMovement_withCastleIntoCheck(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest(
            "2r3r1/8/8/8/8/8/P6P/R3K2R w KQ - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when attempting to castle into check")
                .hasSize(14);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testKingMovement_withCastleBlockedByPlayerPiece(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest("8/8/8/8/8/8/P6P/RN2K1NR w KQ - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when queen's castle blocked by knight")
                .hasSize(15);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testKingMovement_withCastleBlockedByOpponentPiece(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest("8/8/8/8/8/8/P6P/Rn2K1nR w KQ - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when queen's castle blocked by knight")
                .hasSize(9);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testRookMovement_removesCastleOption(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest("8/8/8/8/8/8/P6P/R3K2R w KQ - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .extracting(PieceConfiguration::toString)
                .as("Unexpected castle marker when rook has moved")
                .contains("8/8/8/8/8/8/P6P/R3KR2 b Q - 1 1",
                        "8/8/8/8/8/8/P6P/3RK2R b K - 1 1");
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testStartingPosition(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest(
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves from starting position")
                .hasSize(20);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testPawnPromotion(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest("8/3P4/8/8/8/8/8/8 w - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves for promoted pawn")
                .hasSize(4);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testStalemate(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest("1r6/8/8/8/8/8/7r/K7 w - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves for cornered king")
                .isEmpty();
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testInCheck_whenOnlyKnightCanBlock(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest("8/8/8/8/8/N7/PPP5/K6r w - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when only the knight can block check")
                .hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testInCheck_whenOnlyBishopCanBlock(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest("qq6/q6B/8/8/8/8/8/7K w - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when only the bishop can block check")
                .hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testInCheck_whenPawnTakeCanBlock(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest("8/PP6/K5rr/PP5P/8/8/8/8 w - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when only taking the rook with the pawn can block")
                .hasSize(2);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testInCheck_whenOnlyPawnCanBlock(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest("8/PP6/K5rr/PP4P1/8/8/8/8 w - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when pawn cannot take checking rook")
                .hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testInCheck_whenPawnMovingBehindKingDoesNotBlock(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest("8/PP5r/1K5r/PP5r/8/8/8/8 w - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when a pawn moving behind the king does not block check")
                .isEmpty();
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testInCheck_whenBishopCannotBlockQueenAndRook(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest("qq6/q6B/8/8/8/8/8/r6K w - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when the king is pinned down and checked from 2 angles")
                .isEmpty();
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testInCheck_whenBishopCannotBlockKnight(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest("qqq4B/q7/8/8/8/5K2/8/6n1 w - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when the king is pinned down and checked by a knight and a queen")
                .isEmpty();
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testInCheck_whenBothDirectionsCheckedByBishops(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest("7b/b7/8/3P4/2PKP3/3P4/8/8 w - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when the king is pinned down by two bishops")
                .isEmpty();
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testInCheck_whenMovingPawnToProtectWouldExposeKingToRook(
            Class<? extends PieceConfiguration> configurationClass)
    {
        PieceConfiguration pieceConfiguration = setupTest(
            "8/b7/8/3PP3/r1PKP3/2PP4/8/8 w - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when moving the pawn to protect would expose to a rook")
                .isEmpty();
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testInCheck_tryingToMoveAwayFromCheckingRook(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest("8/8/8/8/8/8/PPP5/1K5r w - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when the king has been check-mated")
                .isEmpty();
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testInCheck_tryingToMoveAwayFromCheckingPawn(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest("8/8/5p2/4p3/3K4/8/8/8 w - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when the king has been checked by a pawn")
                .hasSize(7);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testInCheck_tryingToMoveAwayFromCheckingKnight(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest("8/8/8/8/8/1nn5/8/K2n4 w - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when the king has been checked and cornered by knights")
                .isEmpty();
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testInCheck_whenKingCannotMoveButPawnCanTakeChecker(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest("7k/6pp/6N1/8/8/8/B7/8 b - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when the king has been checked by a takeable knight")
                .hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testInCheck_whenOpposingPieceBlocksTakingCheckingPiece(
            Class<? extends PieceConfiguration> configurationClass)
    {
        PieceConfiguration pieceConfiguration = setupTest("4k3/8/8/q2BQ/8/8/8/7K b - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();
        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when the king has been checked by an untakeable queen")
                .hasSize(3);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testInCheck_whenOpposingPieceDoesNotBlockTakingCheckingPiece(
            Class<? extends PieceConfiguration> configurationClass)
    {
        PieceConfiguration pieceConfiguration = setupTest("4k3/8/8/q3Q/8/8/8/7K b - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();
        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when the king has been checked by an takeable queen")
                .hasSize(5);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testConfinedMovement_withOrthogonalDirectionalBitFlag(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest(
            "3rkr2/4q3/8/8/4P3/8/4K3/8 w - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();
        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when the king and pawn can only move in one plane")
                .hasSize(3);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testConfinedMovement_withDiagonalDirectionalBitFlag_pawnTakes(
            Class<? extends PieceConfiguration> configurationClass)
    {
        PieceConfiguration pieceConfiguration = setupTest(
            "4kbb1/7b/5b1b/4b3/3P4/8/1K6/8 w - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();
        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when the king and pawn can only move in one plane")
                .hasSize(3);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testConfinedMovement_withDiagonalDirectionalBitFlag_pawnPinned(
            Class<? extends PieceConfiguration> configurationClass)
    {
        PieceConfiguration pieceConfiguration = setupTest("4kbb1/7b/5b1b/8/3P4/8/1K6/8 w - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();
        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when the king can only move in one plane")
                .hasSize(2);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testKnightMovement_withFriendlyAndOpposingKnightsBlockingCheck(
            Class<? extends PieceConfiguration> configurationClass)
    {
        PieceConfiguration pieceConfiguration = setupTest(
            "r1b2rk1/ppp1qppp/2n5/3pN3/2P1nB2/5Q2/PPP2PPP/R3KB1R w KQ - 6 10", configurationClass);

        int pieceBitFlag = pieceConfiguration.getPieceAtPosition(36);
        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurationsForPiece(pieceBitFlag);

        assertThat(pieceConfigurations)
                .as("Expected moves to be available to knight at e5")
                .hasSize(6);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testPawnMovement_withFriendlyPawnBlockingCheck(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest("7k/8/8/K3P2r/8/8/8/8 w - - 0 1", configurationClass);

        int pieceBitFlag = pieceConfiguration.getPieceAtPosition(36);
        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurationsForPiece(pieceBitFlag);

        assertThat(pieceConfigurations)
                .as("Expected no moves to be available to pawn at e5 because it blocks check")
                .isEmpty();
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testPawnMovement_withFriendlyAndOpposingPawnsBlockingCheck(
            Class<? extends PieceConfiguration> configurationClass)
    {
        PieceConfiguration pieceConfiguration = setupTest("7k/8/8/K1p1P2r/8/8/8/8 w - - 0 1", configurationClass);

        int pieceBitFlag = pieceConfiguration.getPieceAtPosition(36);
        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurationsForPiece(pieceBitFlag);

        assertThat(pieceConfigurations)
                .as("Expected the pawn to be able to move because an opposing pawn also blocks check")
                .hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testPawnMovement_withFriendlyPawnAndOpposingKingBlockingCheck(
            Class<? extends PieceConfiguration> configurationClass)
    {
        PieceConfiguration pieceConfiguration = setupTest("8/8/8/K1k1P2r/8/8/8/8 w - - 0 1", configurationClass);

        int pieceBitFlag = pieceConfiguration.getPieceAtPosition(36);
        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurationsForPiece(pieceBitFlag);

        assertThat(pieceConfigurations)
                .as("Expected the pawn to be able to move because the opposing king also blocks check")
                .hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testPawnMovement_withKingCheckedByTakeableOpposingPawn(
            Class<? extends PieceConfiguration> configurationClass)
    {
        PieceConfiguration pieceConfiguration = setupTest("7k/8/8/3p4/2P1K3/8/8/3q4 w - - 0 1", configurationClass);

        int pieceBitFlag = pieceConfiguration.getPieceAtPosition(26);
        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurationsForPiece(pieceBitFlag);

        assertThat(pieceConfigurations)
                .as("Expected pawn to be able to end check")
                .hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testPawnMovement_withKingCheckedByUntakeableOpposingPawn(
            Class<? extends PieceConfiguration> configurationClass)
    {
        PieceConfiguration pieceConfiguration = setupTest("7k/8/8/3p4/r1P1K3/8/8/3q4 w - - 0 1", configurationClass);

        int pieceBitFlag = pieceConfiguration.getPieceAtPosition(26);
        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurationsForPiece(pieceBitFlag);

        assertThat(pieceConfigurations)
                .as("Expected pawn not to be able to end check because of opposing rook")
                .isEmpty();
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testEnPassantSquareIsCleared(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest(
            "rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq d3 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();
        List<String> fens = pieceConfigurations.stream().map(PieceConfiguration::toString).toList();
        assertThat(fens)
            .as("En-passant square should be cleared after every move")
            .noneMatch(fen -> fen.contains(" d3 "));
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testPromotionWhileTaking(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest("7k/8/8/8/8/8/p7/1Q5K b - - 0 1", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();
        List<String> fens = pieceConfigurations.stream().map(PieceConfiguration::toString).toList();

        assertThat(fens)
            .as("Promotion to black queen should be possible")
            .contains("7k/8/8/8/8/8/8/1q5K w - - 0 2");
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testPawnMovementWhenJumpingForwards(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest(
            "rnk5/pp3ppp/5P2/8/2P1n1B1/1Pr5/PK5P/3RR3 b - - 1 26", configurationClass);

        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurations();
        List<String> fens = pieceConfigurations.stream().map(PieceConfiguration::toString).toList();

        assertThat(fens)
            .as("Pawns should not be able to jump other pieces")
            .doesNotContain("rnk5/pp4pp/5P2/5p2/2P1n1B1/1Pr5/PK5P/3RR3 w - f6 0 27");
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testEnPassant_1(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest(
            "rnb1k2r/p1p2pp1/1b2p3/1p1pN1Pp/qP1P1P2/P1PK4/1B1N4/R7 w kq h6 0 24", configurationClass);

        int pieceBitFlag = pieceConfiguration.getPieceAtPosition(38);
        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurationsForPiece(pieceBitFlag);
        List<String> fens = pieceConfigurations.stream()
            .map(PieceConfiguration::toString)
            .toList();

        assertThat(fens)
            .as("En-passant should be available")
            .contains("rnb1k2r/p1p2pp1/1b2p2P/1p1pN3/qP1P1P2/P1PK4/1B1N4/R7 b kq - 0 24");
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testEnPassant_2(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest(
            "rnb1k2r/ppqpb3/2p1p2p/4Pppn/1PB5/P1N1QNPP/2PB1P2/3RK2R w Kkq f6 0 16", configurationClass);

        int pieceBitFlag = pieceConfiguration.getPieceAtPosition(36);
        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurationsForPiece(pieceBitFlag);
        List<String> fens = pieceConfigurations.stream()
            .map(PieceConfiguration::toString)
            .toList();

        assertThat(fens)
            .as("En-passant should be available")
            .contains("rnb1k2r/ppqpb3/2p1pP1p/6pn/1PB5/P1N1QNPP/2PB1P2/3RK2R b Kkq - 0 16");
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testEnPassant_3(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = setupTest(
            "r1b1k1n1/p1p2p2/2n1p3/1p1pP3/3P1P2/2P2B2/PP6/R1K5 w q d6 0 22", configurationClass);

        int pieceBitFlag = pieceConfiguration.getPieceAtPosition(36);
        List<PieceConfiguration> pieceConfigurations = pieceConfiguration.getOnwardConfigurationsForPiece(pieceBitFlag);
        List<String> fens = pieceConfigurations.stream()
            .map(PieceConfiguration::toString)
            .toList();

        assertThat(fens)
            .as("En-passant should be available")
            .contains("r1b1k1n1/p1p2p2/2nPp3/1p6/3P1P2/2P2B2/PP6/R1K5 b q - 0 22");
    }

    private PieceConfiguration setupTest(String fen, Class<? extends PieceConfiguration> configurationClass) {
        return FENReader.read(fen, configurationClass);
    }
}