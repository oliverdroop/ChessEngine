package chess.api;

import org.junit.jupiter.api.Test;

import static chess.api.PositionEvaluator.getBestMoveRecursively;
import static org.assertj.core.api.Assertions.assertThat;

public class AITest {

	private PieceConfiguration pieceConfiguration;

	private PieceConfiguration newPieceConfiguration;
	
	@Test
	void testAITakesQueen_edgeOfBoard() {
		setupTest("7k/8/8/q7/1P6/8/8/7K w - - 0 1");
		newPieceConfiguration = getBestMoveRecursively(pieceConfiguration, 4);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("White pawn should take black queen")
				.isEqualTo("7k/8/8/P7/8/8/8/7K b - - 0 1");
	}

	@Test
	void testAITakesQueen_middleOfBoard() {
		setupTest("4k3/8/8/3q1p2/4P3/8/8/4K3 w - - 0 1");
		newPieceConfiguration = getBestMoveRecursively(pieceConfiguration, 4);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("White pawn should take black queen")
				.isEqualTo("4k3/8/8/3P1p2/8/8/8/4K3 b - - 0 1");
	}
	
	@Test
	void testAIChoosesCheckmate_bishopWithRooks() {
		setupTest("k5B1/7R/8/8/8/8/P1PPPPPP/1R2K3 w - - 0 1");
		newPieceConfiguration = getBestMoveRecursively(pieceConfiguration, 4);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("White bishop should force checkmate")
				.isEqualTo("k7/7R/8/3B4/8/8/P1PPPPPP/1R2K3 b - - 1 1");
	}
	@Test
	void testAIChoosesCheckmate_bishopWithKnights() {
		setupTest("k5B1/3N4/8/1N6/8/8/8/4K3 w - - 0 1");
		newPieceConfiguration = getBestMoveRecursively(pieceConfiguration, 4);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("White bishop should force checkmate")
				.isEqualTo("k7/3N4/8/1N1B4/8/8/8/4K3 b - - 1 1");
	}
	@Test
	void testAIChoosesCheckmate_twoRooks() {
		setupTest("k7/7R/6R1/8/8/8/p3K3/8 w - - 0 1");
		newPieceConfiguration = getBestMoveRecursively(pieceConfiguration, 4);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("White rook should force checkmate")
				.isEqualTo("k5R1/7R/8/8/8/8/p3K3/8 b - - 1 1");
	}

	@Test
	void testAIChoosesCheckmate_twoRooksTowardsEdge() {
		setupTest("K7/8/8/2r5/2r5/8/8/7k b - - 0 1");
		newPieceConfiguration = getBestMoveRecursively(pieceConfiguration, 4);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("White rook should progress towards forcing checkmate")
				.isIn("K7/8/8/1r6/2r5/8/8/7k w - - 1 2",
						"K7/8/8/2r5/1r6/8/8/7k w - - 1 2",
						"K7/8/8/2r5/r7/8/8/7k w - - 1 2",
						"K7/8/8/r7/2r5/8/8/7k w - - 1 2");
	}

	@Test
	void testAIChoosesCheckmate_foolsMate() {
		setupTest("rnbqkbnr/pppp1ppp/8/4p3/6P1/5P2/PPPPP2P/RNBQKBNR b KQkq - 0 2");
		newPieceConfiguration = getBestMoveRecursively(pieceConfiguration, 4);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("Black bishop should force checkmate")
				.isEqualTo("rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3");
	}
	
	@Test
	void testAIUpgradesPawn() {
		setupTest("8/kBPN4/8/8/8/8/8/KR6 w - - 0 1");
		newPieceConfiguration = getBestMoveRecursively(pieceConfiguration, 4);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("Upgrading white pawn should avoid stalemate")
				.isEqualTo("2N5/kB1N4/8/8/8/8/8/KR6 b - - 0 1");
	}

	@Test
	void testAIAvoidsStalemate() {
		setupTest("7k/8/5N2/5N2/8/8/8/K7 w - - 0 1");
		newPieceConfiguration = getBestMoveRecursively(pieceConfiguration, 4);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("One of the knights should move to avoid blocking the black king in the corner")
				.doesNotContain("5N2/5N2");
	}

	@Test
	void testAIAvoidsCheckmate_earlyGame() {
		setupTest("rn2kbnr/pbqp1pp1/1pp1p3/4N1Bp/3PP3/2NB1Q2/PPP2PPP/R3K2R b KQkq - 2 8");
		newPieceConfiguration = getBestMoveRecursively(pieceConfiguration, 4);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("Black should avoid mate in 1")
				.isIn("rn2kbnr/pbqp2p1/1pp1pp2/4N1Bp/3PP3/2NB1Q2/PPP2PPP/R3K2R w KQkq - 0 9",
						"rn2kbnr/pbqp2p1/1pp1p3/4NpBp/3PP3/2NB1Q2/PPP2PPP/R3K2R w KQkq f6 0 9",
						"rn2kb1r/pbqp1pp1/1pp1pn2/4N1Bp/3PP3/2NB1Q2/PPP2PPP/R3K2R w KQkq - 3 9",
						"rn1k1bnr/pbqp1pp1/1pp1p3/4N1Bp/3PP3/2NB1Q2/PPP2PPP/R3K2R w KQkq - 3 9",
						"rn2kb1r/pbqp1pp1/1pp1p2n/4N1Bp/3PP3/2NB1Q2/PPP2PPP/R3K2R w KQkq - 3 9",
						"rn2kb1r/pbqpnpp1/1pp1p3/4N1Bp/3PP3/2NB1Q2/PPP2PPP/R3K2R w KQkq - 3 9",
						"rn2kbnr/pbq2pp1/1pppp3/4N1Bp/3PP3/2NB1Q2/PPP2PPP/R3K2R w KQkq - 0 9",
						"rn2kbnr/pbq2pp1/1pp1p3/3pN1Bp/3PP3/2NB1Q2/PPP2PPP/R3K2R w KQkq d6 0 9");
	}

	@Test
	void testAIAvoidsCheckmate_lateGame() {
		setupTest("k7/ppp5/8/8/8/8/8/QK5B b - - 2 8");
		newPieceConfiguration = getBestMoveRecursively(pieceConfiguration, 4);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("Black should avoid mate in 1")
				.isIn("k7/1pp5/p7/8/8/8/8/QK5B w - - 0 9",
						"k7/1pp5/8/p7/8/8/8/QK5B w - a6 0 9");
	}

	@Test
	void simpleCornerTest_chooseCheckmateOverStalemate() {
		setupTest("k7/2P5/K7/8/8/8/8/8 w - - 0 50");
		newPieceConfiguration = getBestMoveRecursively(pieceConfiguration, 4);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("White should choose checkmate")
				.isIn("k1Q5/8/K7/8/8/8/8/8 b - - 0 50",
						"k1R5/8/K7/8/8/8/8/8 b - - 0 50");
	}

	@Test
	void fiftyMoveRuleTest_loseByMovingKing() {
		setupTest("7K/7P/8/8/8/8/8/k7 w - - 99 50");
		newPieceConfiguration = getBestMoveRecursively(pieceConfiguration, 4);
		assertThat(newPieceConfiguration).isNull();
	}

	@Test
	void fiftyMoveRuleTest_continueByMovingPawn() {
		setupTest("7K/7P/8/8/7r/2P4k/8/B7 w - - 99 50");
		newPieceConfiguration = getBestMoveRecursively(pieceConfiguration, 4);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("Expected white to avoid losing by moving a pawn")
				.isEqualTo("7K/7P/8/8/2P4r/7k/8/B7 b - - 0 50");
	}
	
	private void setupTest(String fen) {
		pieceConfiguration = FENReader.read(fen);
	}
}

