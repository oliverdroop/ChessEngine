package chessTests;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

import chess.Board;
import chess.FENReader;
import chess.FENWriter;
import chess.Game;
import chess.Move;

public class AITest {
	
	private FENReader fenReader = new FENReader();
	
	private Game game = new Game();
	
	private Board board;
	
	@Rule
	public JUnitSoftAssertions softly = new JUnitSoftAssertions();
	
	@Test
	public void testAITakesQueen() {
		setupTest("4k3/8/8/3q4/4P3/8/8/4K3 w - - 0 1");
		softly.assertThat(game.getAIMove().toString()).as("White pawn should take black queen").contains("WHITE PAWN true 3 3 [3 3] [4 4]");
		setupTest("4k3/8/8/3q1r2/4P3/8/8/4K3 w - - 0 1");
		softly.assertThat(game.getAIMove().toString()).as("White pawn should take black queen").contains("WHITE PAWN true 3 3 [3 3] [4 4]");
	}
	
	@Test
	public void testAIChoosesCheckmate() {
		setupTest("k5B1/7R/8/8/8/8/P1PPPPPP/1R2K3 w - - 0 1");
		softly.assertThat(game.getAIMove().toString()).as("White bishop should force checkmate").contains("WHITE BISHOP true 1 7 [1 7] [4 4]");
		setupTest("k5B1/3N4/8/1N6/8/8/8/4K3 w - - 0 1");
		softly.assertThat(game.getAIMove().toString()).as("White bishop should force checkmate").contains("WHITE BISHOP true 1 7 [1 7] [4 4]");
		setupTest("k7/7R/6R1/8/8/8/p3K3/8 w - - 0 1");
		softly.assertThat(game.getAIMove().toString()).as("White rook should force checkmate").contains("WHITE ROOK true 1 5 [1 5] [1 7]");
	}
	
	@Test
	public void testAIUpgradesPawn() {
		setupTest("8/kBPN4/8/8/8/8/8/KR6 w - - 0 1");
		String expectedFEN = "2N5/kB1N4/8/8/8/8/8/KR6";
		softly.assertThat(board.getAvailableMoves()).extracting("toString").contains("WHITE BISHOP true 5 6 [5 6] [5 7] 1.0");
		softly.assertThat(board.getAvailableMoves()).extracting("toString").contains("WHITE ROOK true 5 6 [5 6] [5 7] 1.0");
		softly.assertThat(board.getAvailableMoves()).extracting("toString").contains("WHITE KNIGHT true 5 6 [5 6] [5 7] 1.0");
		softly.assertThat(board.getAvailableMoves()).extracting("toString").contains("WHITE QUEEN true 5 6 [5 6] [5 7] 1.0");
		Move chosenMove = game.getAIMove();
		Board resultantBoard = chosenMove.getResultantBoard();
		FENWriter fenWriter = new FENWriter();
		softly.assertThat(fenWriter.write(resultantBoard)).as("Upgrading white pawn should avoid stalemate").contains(expectedFEN);
	}
	
	private void setupTest(String fen) {
		board = fenReader.read(fen, game);
		game.setBoard(board);
	}
}

