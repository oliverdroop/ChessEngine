package chessTests.api;

import chess.api.FENReader;
import chess.api.FENWriter;
import chess.api.PieceConfiguration;
import chess.api.PositionEvaluator;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

public class AITest {

	private PieceConfiguration pieceConfiguration;

	private PieceConfiguration newPieceConfiguration;
	
	@Rule
	public JUnitSoftAssertions softly = new JUnitSoftAssertions();
	
	@Test
	public void testAITakesQueen() {
		setupTest("7k/8/8/q7/1P6/8/8/7K w - - 0 1");
		newPieceConfiguration = PositionEvaluator.getBestMoveRecursively(pieceConfiguration, 4);
		softly.assertThat(FENWriter.write(newPieceConfiguration))
				.as("White pawn should take black queen")
				.isEqualTo("7k/8/8/P7/8/8/8/7K b - - 0 1");

		setupTest("4k3/8/8/3q1p2/4P3/8/8/4K3 w - - 0 1");
		newPieceConfiguration = PositionEvaluator.getBestMoveRecursively(pieceConfiguration, 4);
		softly.assertThat(FENWriter.write(newPieceConfiguration))
				.as("White pawn should take black queen")
				.isEqualTo("4k3/8/8/3P1p2/8/8/8/4K3 b - - 0 1");
	}
	
	@Test
	public void testAIChoosesCheckmate() {
		setupTest("k5B1/7R/8/8/8/8/P1PPPPPP/1R2K3 w - - 0 1");
		newPieceConfiguration = PositionEvaluator.getBestMoveRecursively(pieceConfiguration, 4);
		softly.assertThat(FENWriter.write(newPieceConfiguration))
				.as("White bishop should force checkmate")
				.isEqualTo("k7/7R/8/3B4/8/8/P1PPPPPP/1R2K3 b - - 1 1");

		setupTest("k5B1/3N4/8/1N6/8/8/8/4K3 w - - 0 1");
		newPieceConfiguration = PositionEvaluator.getBestMoveRecursively(pieceConfiguration, 4);
		softly.assertThat(FENWriter.write(newPieceConfiguration))
				.as("White bishop should force checkmate")
				.isEqualTo("k7/3N4/8/1N1B4/8/8/8/4K3 b - - 1 1");

		setupTest("k7/7R/6R1/8/8/8/p3K3/8 w - - 0 1");
		newPieceConfiguration = PositionEvaluator.getBestMoveRecursively(pieceConfiguration, 4);
		softly.assertThat(FENWriter.write(newPieceConfiguration))
				.as("White rook should force checkmate")
				.isEqualTo("k5R1/7R/8/8/8/8/p3K3/8 b - - 1 1");

		setupTest("rnbqkbnr/pppp1ppp/8/4p3/6P1/5P2/PPPPP2P/RNBQKBNR b KQkq - 0 2");
		newPieceConfiguration = PositionEvaluator.getBestMoveRecursively(pieceConfiguration, 4);
		softly.assertThat(FENWriter.write(newPieceConfiguration))
				.as("Black bishop should force checkmate")
				.isEqualTo("rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3");
	}
	
	@Test
	public void testAIUpgradesPawn() {
		setupTest("8/kBPN4/8/8/8/8/8/KR6 w - - 0 1");
		newPieceConfiguration = PositionEvaluator.getBestMoveRecursively(pieceConfiguration, 4);
		softly.assertThat(FENWriter.write(newPieceConfiguration))
				.as("Upgrading white pawn should avoid stalemate")
				.isEqualTo("2N5/kB1N4/8/8/8/8/8/KR6 b - - 0 1");
	}

	@Test
	public void testAIAvoidsStalemate() {
		setupTest("7k/8/5N2/5N2/8/8/8/K7 w - - 0 1");
		newPieceConfiguration = PositionEvaluator.getBestMoveRecursively(pieceConfiguration, 4);
		softly.assertThat(FENWriter.write(newPieceConfiguration))
				.as("One of the knights should move to avoid blocking the black king in the corner")
				.doesNotContain("5N2/5N2");
	}

	@Test
	public void testAIAvoidsCheckmate_earlyGame() {
		setupTest("rn2kbnr/pbqp1pp1/1pp1p3/4N1Bp/3PP3/2NB1Q2/PPP2PPP/R3K2R b KQkq - 2 8");
		newPieceConfiguration = PositionEvaluator.getBestMoveRecursively(pieceConfiguration, 4);
		softly.assertThat(FENWriter.write(newPieceConfiguration))
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
	public void testAIAvoidsCheckmate_lateGame() {
		setupTest("k7/ppp5/8/8/8/8/8/QK5B b - - 2 8");
		newPieceConfiguration = PositionEvaluator.getBestMoveRecursively(pieceConfiguration, 4);
		softly.assertThat(FENWriter.write(newPieceConfiguration))
				.as("Black should avoid mate in 1")
				.isIn("k7/1pp5/p7/8/8/8/8/QK5B w - - 0 9",
						"k7/1pp5/8/p7/8/8/8/QK5B w - a6 0 9");
	}

	@Test
	public void simpleCornerTest_chooseChecmkateOverStalemate() {
		setupTest("k7/2P5/K7/8/8/8/8/8 w - - 0 50");
		newPieceConfiguration = PositionEvaluator.getBestMoveRecursively(pieceConfiguration, 4);
		softly.assertThat(FENWriter.write(newPieceConfiguration))
				.as("White should choose checkmate")
				.isIn("k1Q5/8/K7/8/8/8/8/8 b - - 0 50",
						"k1R5/8/K7/8/8/8/8/8 b - - 0 50");
	}

//	@Test
//	public void myTest() {
//		setupTest("rnb5/pp1k3p/q2BQp2/2P3P1/P1NNP3/2PP2PB/7P/R3K2R b KQ - 9 24");
//		newPieceConfiguration = PositionEvaluator.getBestMoveRecursively(pieceConfiguration, 4);
//		softly.assertThat(FENWriter.write(newPieceConfiguration))
//				.isEqualTo("");
//	}
	
	private void setupTest(String fen) {
		pieceConfiguration = FENReader.read(fen);
	}
}
