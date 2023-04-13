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
		setupTest("4k3/8/8/3q4/4P3/8/8/4K3 w - - 0 1");
		newPieceConfiguration = PositionEvaluator.getBestMoveRecursively(pieceConfiguration, 4);
		softly.assertThat(FENWriter.write(newPieceConfiguration))
				.as("White pawn should take black queen")
				.isEqualTo("4k3/8/8/3P4/8/8/8/4K3 b - - 0 1");

		setupTest("4k3/8/8/3q1r2/4P3/8/8/4K3 w - - 0 1");
		newPieceConfiguration = PositionEvaluator.getBestMoveRecursively(pieceConfiguration, 4);
		softly.assertThat(FENWriter.write(newPieceConfiguration))
				.as("White pawn should take black queen")
				.isEqualTo("4k3/8/8/3P1r2/8/8/8/4K3 b - - 0 1");
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
	
	private void setupTest(String fen) {
		pieceConfiguration = FENReader.read(fen);
	}
}

