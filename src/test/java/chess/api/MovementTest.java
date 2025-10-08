package chess.api;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static chess.api.ai.util.StreamUtil.readObjectsFromStream;
import static org.assertj.core.api.Assertions.assertThat;

public class MovementTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MovementTest.class);

    @Test
    void testBishopMovement() {
        PieceConfiguration pieceConfiguration = FENReader.read("B7/8/8/8/8/8/8/8 w - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of forward piece configurations returned")
                .hasSize(7);
    }

    @Test
    void testBishopMovement_withOpponentPiecesBlocking() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/8/2p1p3/3B4/2p1p3/8/8/8 w - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of forward piece configurations returned")
                .hasSize(4);
    }

    @Test
    void testBishopMovement_withPlayerPiecesBlocking() {
        PieceConfiguration pieceConfiguration = FENReader.read("nNRRRRRR/NBRRRRRR/RRRRRRRR/RRRRRRRR/RRRRRRRR/RRRRRRRR/RRRRRRRR/RRRRRRRR w - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of forward piece configurations returned")
                .hasSize(1);
    }

    @Test
    void testKnightMovement() {
        PieceConfiguration pieceConfiguration = FENReader.read("N7/8/8/8/8/8/8/8 w - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of forward piece configurations returned")
                .hasSize(2);
    }

    @Test
    void testApplyTranslation_withIllegalNegXNegYTranslation() {
        int position = 8;

        int newPosition = Position.applyTranslation(position, -1, -1);

        assertThat(newPosition).as("Unexpected value for translation which should be illegal").isEqualTo(-1);
    }

    @Test
    void testApplyTranslation_withLegalNegYTranslation() {
        int position = 8;

        int newPosition = Position.applyTranslation(position, 0, -1);

        assertThat(newPosition).as("Unexpected value for translation which should be legal").isEqualTo(0);
    }

    @Test
    void testApplyTranslation_withIllegalPosXPosYTranslation() {
        int position = 55;

        int newPosition = Position.applyTranslation(position, 1, 1);

        assertThat(newPosition).as("Unexpected value for translation which should be illegal").isEqualTo(-1);
    }

    @Test
    void testApplyTranslation_withLegalPosXTranslation() {
        int position = 62;

        int newPosition = Position.applyTranslation(position, 1, 0);

        assertThat(newPosition).as("Unexpected value for translation which should be illegal").isEqualTo(63);
    }

    @Test
    void testApplyTranslation_withLegalPosYTranslation() {
        int position = 7;

        int newPosition = Position.applyTranslation(position, 0, 7);

        assertThat(newPosition).as("Unexpected value for translation which should be legal").isEqualTo(63);
    }

    @Test
    void testMovement_whenProtectingKingWithKnight() {
        PieceConfiguration pieceConfiguration = FENReader.read("KP6/PN6/8/8/8/8/8/7b w - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Expected no moves to be available because knight protects king")
                .isEmpty();
    }

    @Test
    void testMovement_whenProtectingKingWithBishop() {
        PieceConfiguration pieceConfiguration = FENReader.read("KP6/PB6/8/8/8/8/8/7b w - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of moves available to bishop protecting king")
                .hasSize(6);
    }

    @Test
    void testMovement_whenKingDoublyProtected() {
        PieceConfiguration pieceConfiguration = FENReader.read("KP6/PN6/2N5/8/8/8/8/7b w - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of moves available when king is doubly protected")
                .hasSize(10);
    }

    @Test
    void testPawnMovement_fromStartingPosition() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/8/8/8/8/8/1P6/8 w - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Expected two possible moves for pawn in starting position")
                .hasSize(2);
    }

    @Test
    void testPawnMovement_fromNonStartingPositionWithoutTargets() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/8/8/8/8/1P6/8/8 w - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Expected one possible move for pawn not in starting position")
                .hasSize(1);
    }

    @Test
    void testPawnMovement_fromStartingPositionWithDistantBlockingOpponent() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/8/8/8/1p6/8/1P6/8 w - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Expected one possible move for pawn with opponent blocking two squares forward")
                .hasSize(1);
    }

    @Test
    void testPawnMovement_fromStartingPositionWithCloseBlockingOpponent() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/8/8/8/8/1p6/1P6/8 w - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Expected no possible moves for pawn with opponent immediately blocking")
                .isEmpty();
    }

    @Test
    void testPawnMovement_fromStartingPositionWithTwoTakeableOpponents() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/8/8/8/8/p1p5/1P6/8 w - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves for pawn on starting rank with two takeable pawns")
                .hasSize(4);
    }

    @Test
    void testPawnMovement_whenProtectingKingFromDistantOpponent() {
        PieceConfiguration pieceConfiguration = FENReader.read("7b/8/8/8/8/8/1P6/KP6 w - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when pawn is blocking check")
                .hasSize(1);
    }

    @Test
    void testPawnMovement_whenProtectingKingFromCloseOpponent() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/8/8/8/8/2b5/1P6/KP6 w - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when pawn is blocking check from takeable bishop")
                .hasSize(2);
    }

    @Test
    void testPawnMovement_withEnPassantAvailable() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/8/8/3Pp3/8/8/8/8 w - e6 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when en passant is possible")
                .hasSize(2);
    }

    @Test
    void testKingMovement_withPawnPreventingTaking() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/2p5/1r6/K7/8/8/8/8 w - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Only one move should be available because the rook is protected by a pawn")
                .hasSize(1);
    }

    @Test
    void testKingMovement_withTwoCastlesAvailable() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/8/8/8/8/8/P6P/R3K2R w KQ - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Both castling positions should be available")
                .hasSize(16);
    }

    @Test
    void testKingMovement_withNoCastlesAvailable() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/8/8/8/8/8/P6P/R3K2R w - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when neither castling position is available")
                .hasSize(14);
    }

    @Test
    void testKingMovement_withCastleThroughCheck() {
        PieceConfiguration pieceConfiguration = FENReader.read("3r1r2/8/8/8/8/8/P6P/R3K2R w KQ - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when attempting to castle through check")
                .hasSize(10);
    }

    @Test
    void testKingMovement_withCastleOutOfCheck() {
        PieceConfiguration pieceConfiguration = FENReader.read("4r3/8/8/8/8/8/P6P/R3K2R w KQ - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when attempting to castle out of check")
                .hasSize(4);
    }

    @Test
    void testKingMovement_withCastleIntoCheck() {
        PieceConfiguration pieceConfiguration = FENReader.read("2r3r1/8/8/8/8/8/P6P/R3K2R w KQ - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when attempting to castle into check")
                .hasSize(14);
    }

    @Test
    void testKingMovement_withCastleBlockedByPlayerPiece() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/8/8/8/8/8/P6P/RN2K1NR w KQ - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when queen's castle blocked by knight")
                .hasSize(15);
    }

    @Test
    void testKingMovement_withCastleBlockedByOpponentPiece() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/8/8/8/8/8/P6P/Rn2K1nR w KQ - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when queen's castle blocked by knight")
                .hasSize(9);
    }

    @Test
    void testRookMovement_removesCastleOption() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/8/8/8/8/8/P6P/R3K2R w KQ - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .extracting(PieceConfiguration::toString)
                .as("Unexpected castle marker when rook has moved")
                .contains("8/8/8/8/8/8/P6P/R3KR2 b Q - 1 1",
                        "8/8/8/8/8/8/P6P/3RK2R b K - 1 1");
    }

    @Test
    void testStartingPosition() {
        PieceConfiguration pieceConfiguration = FENReader.read("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves from starting position")
                .hasSize(20);
    }

    @Test
    void testPawnPromotion() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/3P4/8/8/8/8/8/8 w - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves for promoted pawn")
                .hasSize(4);
    }

    @Test
    void testStalemate() {
        PieceConfiguration pieceConfiguration = FENReader.read("1r6/8/8/8/8/8/7r/K7 w - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves for cornered king")
                .isEmpty();
    }

    @Test
    void testInCheck_whenOnlyKnightCanBlock() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/8/8/8/8/N7/PPP5/K6r w - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when only the knight can block check")
                .hasSize(1);
    }

    @Test
    void testInCheck_whenOnlyBishopCanBlock() {
        PieceConfiguration pieceConfiguration = FENReader.read("qq6/q6B/8/8/8/8/8/7K w - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when only the bishop can block check")
                .hasSize(1);
    }

    @Test
    void testInCheck_whenPawnTakeCanBlock() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/PP6/K5rr/PP5P/8/8/8/8 w - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when only taking the rook with the pawn can block")
                .hasSize(2);
    }

    @Test
    void testInCheck_whenOnlyPawnCanBlock() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/PP6/K5rr/PP4P1/8/8/8/8 w - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when pawn cannot take checking rook")
                .hasSize(1);
    }

    @Test
    void testInCheck_whenPawnMovingBehindKingDoesNotBlock() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/PP5r/1K5r/PP5r/8/8/8/8 w - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when a pawn moving behind the king does not block check")
                .isEmpty();
    }

    @Test
    void testInCheck_whenBishopCannotBlockQueenAndRook() {
        PieceConfiguration pieceConfiguration = FENReader.read("qq6/q6B/8/8/8/8/8/r6K w - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when the king is pinned down and checked from 2 angles")
                .isEmpty();
    }

    @Test
    void testInCheck_whenBishopCannotBlockKnight() {
        PieceConfiguration pieceConfiguration = FENReader.read("qqq4B/q7/8/8/8/5K2/8/6n1 w - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when the king is pinned down and checked by a knight and a queen")
                .isEmpty();
    }

    @Test
    void testInCheck_whenBothDirectionsCheckedByBishops() {
        PieceConfiguration pieceConfiguration = FENReader.read("7b/b7/8/3P4/2PKP3/3P4/8/8 w - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when the king is pinned down by two bishops")
                .isEmpty();
    }

    @Test
    void testInCheck_whenMovingPawnToProtectWouldExposeKingToRook() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/b7/8/3PP3/r1PKP3/2PP4/8/8 w - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when moving the pawn to protect would expose to a rook")
                .isEmpty();
    }

    @Test
    void testInCheck_tryingToMoveAwayFromCheckingRook() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/8/8/8/8/8/PPP5/1K5r w - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when the king has been check-mated")
                .isEmpty();
    }

    @Test
    void testInCheck_tryingToMoveAwayFromCheckingPawn() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/8/5p2/4p3/3K4/8/8/8 w - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when the king has been checked by a pawn")
                .hasSize(7);
    }

    @Test
    void testInCheck_tryingToMoveAwayFromCheckingKnight() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/8/8/8/8/1nn5/8/K2n4 w - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when the king has been checked and cornered by knights")
                .isEmpty();
    }

    @Test
    void testInCheck_whenKingCannotMoveButPawnCanTakeChecker() {
        PieceConfiguration pieceConfiguration = FENReader.read("7k/6pp/6N1/8/8/8/B7/8 b - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();

        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when the king has been checked by a takeable knight")
                .hasSize(1);
    }

    @Test
    void testInCheck_whenOpposingPieceBlocksTakingCheckingPiece() {
        PieceConfiguration pieceConfiguration = FENReader.read("4k3/8/8/q2BQ/8/8/8/7K b - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();
        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when the king has been checked by an untakeable queen")
                .hasSize(3);
    }

    @Test
    void testInCheck_whenOpposingPieceDoesNotBlockTakingCheckingPiece() {
        PieceConfiguration pieceConfiguration = FENReader.read("4k3/8/8/q3Q/8/8/8/7K b - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();
        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when the king has been checked by an takeable queen")
                .hasSize(5);
    }

    @Test
    void testConfinedMovement_withOrthogonalDirectionalBitFlag() {
        PieceConfiguration pieceConfiguration = FENReader.read("3rkr2/4q3/8/8/4P3/8/4K3/8 w - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();
        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when the king and pawn can only move in one plane")
                .hasSize(3);
    }

    @Test
    void testConfinedMovement_withDiagonalDirectionalBitFlag_pawnTakes() {
        PieceConfiguration pieceConfiguration = FENReader.read("4kbb1/7b/5b1b/4b3/3P4/8/1K6/8 w - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();
        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when the king and pawn can only move in one plane")
                .hasSize(3);
    }

    @Test
    void testConfinedMovement_withDiagonalDirectionalBitFlag_pawnPinned() {
        PieceConfiguration pieceConfiguration = FENReader.read("4kbb1/7b/5b1b/8/3P4/8/1K6/8 w - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();
        assertThat(pieceConfigurations)
                .as("Unexpected number of available moves when the king can only move in one plane")
                .hasSize(2);
    }

    @Test
    void testKnightMovement_withFriendlyAndOpposingKnightsBlockingCheck() {
        PieceConfiguration pieceConfiguration = FENReader.read("r1b2rk1/ppp1qppp/2n5/3pN3/2P1nB2/5Q2/PPP2PPP/R3KB1R w KQ - 6 10");

        int pieceBitFlag = pieceConfiguration.getPieceAtPosition(36);
        PieceConfiguration[] pieceConfigurations = readObjectsFromStream(pieceConfiguration.getOnwardConfigurationsForPiece(pieceBitFlag), PieceConfiguration::fromInputStream, 260);

        assertThat(pieceConfigurations)
                .as("Expected moves to be available to knight at e5")
                .hasSize(6);
    }

    @Test
    void testPawnMovement_withFriendlyPawnBlockingCheck() {
        PieceConfiguration pieceConfiguration = FENReader.read("7k/8/8/K3P2r/8/8/8/8 w - - 0 1");

        int pieceBitFlag = pieceConfiguration.getPieceAtPosition(36);
        PieceConfiguration[] pieceConfigurations = readObjectsFromStream(pieceConfiguration.getOnwardConfigurationsForPiece(pieceBitFlag), PieceConfiguration::fromInputStream, 260);

        assertThat(pieceConfigurations)
                .as("Expected no moves to be available to pawn at e5 because it blocks check")
                .isEmpty();
    }

    @Test
    void testPawnMovement_withFriendlyAndOpposingPawnsBlockingCheck() {
        PieceConfiguration pieceConfiguration = FENReader.read("7k/8/8/K1p1P2r/8/8/8/8 w - - 0 1");

        int pieceBitFlag = pieceConfiguration.getPieceAtPosition(36);
        PieceConfiguration[] pieceConfigurations = readObjectsFromStream(pieceConfiguration.getOnwardConfigurationsForPiece(pieceBitFlag), PieceConfiguration::fromInputStream, 260);

        assertThat(pieceConfigurations)
                .as("Expected the pawn to be able to move because an opposing pawn also blocks check")
                .hasSize(1);
    }

    @Test
    void testPawnMovement_withFriendlyPawnAndOpposingKingBlockingCheck() {
        PieceConfiguration pieceConfiguration = FENReader.read("8/8/8/K1k1P2r/8/8/8/8 w - - 0 1");

        int pieceBitFlag = pieceConfiguration.getPieceAtPosition(36);
        PieceConfiguration[] pieceConfigurations = readObjectsFromStream(pieceConfiguration.getOnwardConfigurationsForPiece(pieceBitFlag), PieceConfiguration::fromInputStream, 260);

        assertThat(pieceConfigurations)
                .as("Expected the pawn to be able to move because the opposing king also blocks check")
                .hasSize(1);
    }

    @Test
    void testPawnMovement_withKingCheckedByTakeableOpposingPawn() {
        PieceConfiguration pieceConfiguration = FENReader.read("7k/8/8/3p4/2P1K3/8/8/3q4 w - - 0 1");

        int pieceBitFlag = pieceConfiguration.getPieceAtPosition(26);
        PieceConfiguration[] pieceConfigurations = readObjectsFromStream(pieceConfiguration.getOnwardConfigurationsForPiece(pieceBitFlag), PieceConfiguration::fromInputStream, 260);

        assertThat(pieceConfigurations)
                .as("Expected pawn to be able to end check")
                .hasSize(1);
    }

    @Test
    void testPawnMovement_withKingCheckedByUntakeableOpposingPawn() {
        PieceConfiguration pieceConfiguration = FENReader.read("7k/8/8/3p4/r1P1K3/8/8/3q4 w - - 0 1");

        int pieceBitFlag = pieceConfiguration.getPieceAtPosition(26);
        PieceConfiguration[] pieceConfigurations = readObjectsFromStream(pieceConfiguration.getOnwardConfigurationsForPiece(pieceBitFlag), PieceConfiguration::fromInputStream, 260);

        assertThat(pieceConfigurations)
                .as("Expected pawn not to be able to end check because of opposing rook")
                .isEmpty();
    }

    @Test
    void testEnPassantSquareIsCleared() {
        PieceConfiguration pieceConfiguration = FENReader.read("rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq d3 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();
        List<String> fens = Arrays.stream(pieceConfigurations).map(PieceConfiguration::toString).toList();
        assertThat(fens).as("En-passant square should be cleared after every move").noneMatch(fen -> fen.contains(" d3 "));
    }

    @Test
    void testPromotionWhileTaking() {
        PieceConfiguration pieceConfiguration = FENReader.read("7k/8/8/8/8/8/p7/1Q5K b - - 0 1");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();
        List<String> fens = Arrays.stream(pieceConfigurations).map(PieceConfiguration::toString).toList();

        assertThat(fens).as("Promotion to black queen should be possible").contains("7k/8/8/8/8/8/8/1q5K w - - 0 2");
    }

    @Test
    void testPawnMovementWhenJumpingForwards() {
        PieceConfiguration pieceConfiguration = FENReader.read("rnk5/pp3ppp/5P2/8/2P1n1B1/1Pr5/PK5P/3RR3 b - - 1 26");

        PieceConfiguration[] pieceConfigurations = pieceConfiguration.getOnwardConfigurations();
        List<String> fens = Arrays.stream(pieceConfigurations).map(PieceConfiguration::toString).toList();

        assertThat(fens)
            .as("Pawns should not be able to jump other pieces")
            .doesNotContain("rnk5/pp4pp/5P2/5p2/2P1n1B1/1Pr5/PK5P/3RR3 w - f6 0 27");
    }

    @Test
    void testEnPassant_1() {
        PieceConfiguration pieceConfiguration = FENReader.read("rnb1k2r/p1p2pp1/1b2p3/1p1pN1Pp/qP1P1P2/P1PK4/1B1N4/R7 w kq h6 0 24");

        int pieceBitFlag = pieceConfiguration.getPieceAtPosition(38);
        PieceConfiguration[] pieceConfigurations = readObjectsFromStream(pieceConfiguration.getOnwardConfigurationsForPiece(pieceBitFlag), PieceConfiguration::fromInputStream, 260);
        List<String> fens = Arrays.stream(pieceConfigurations)
            .map(PieceConfiguration::toString)
            .toList();

        assertThat(fens)
            .as("En-passant should be available")
            .contains("rnb1k2r/p1p2pp1/1b2p2P/1p1pN3/qP1P1P2/P1PK4/1B1N4/R7 b kq - 0 24");
    }

    @Test
    void testEnPassant_2() {
        PieceConfiguration pieceConfiguration = FENReader.read("rnb1k2r/ppqpb3/2p1p2p/4Pppn/1PB5/P1N1QNPP/2PB1P2/3RK2R w Kkq f6 0 16");

        int pieceBitFlag = pieceConfiguration.getPieceAtPosition(36);
        PieceConfiguration[] pieceConfigurations = readObjectsFromStream(pieceConfiguration.getOnwardConfigurationsForPiece(pieceBitFlag), PieceConfiguration::fromInputStream, 260);
        List<String> fens = Arrays.stream(pieceConfigurations)
            .map(PieceConfiguration::toString)
            .toList();

        assertThat(fens)
            .as("En-passant should be available")
            .contains("rnb1k2r/ppqpb3/2p1pP1p/6pn/1PB5/P1N1QNPP/2PB1P2/3RK2R b Kkq - 0 16");
    }

    @Test
    void testEnPassant_3() {
        PieceConfiguration pieceConfiguration = FENReader.read("r1b1k1n1/p1p2p2/2n1p3/1p1pP3/3P1P2/2P2B2/PP6/R1K5 w q d6 0 22");

        int pieceBitFlag = pieceConfiguration.getPieceAtPosition(36);
        PieceConfiguration[] pieceConfigurations = readObjectsFromStream(pieceConfiguration.getOnwardConfigurationsForPiece(pieceBitFlag), PieceConfiguration::fromInputStream, 260);
        List<String> fens = Arrays.stream(pieceConfigurations)
            .map(PieceConfiguration::toString)
            .toList();

        assertThat(fens)
            .as("En-passant should be available")
            .contains("r1b1k1n1/p1p2p2/2nPp3/1p6/3P1P2/2P2B2/PP6/R1K5 b q - 0 22");
    }
}