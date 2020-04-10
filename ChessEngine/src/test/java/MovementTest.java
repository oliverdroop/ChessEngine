import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import chess.Board;
import chess.FENReader;
import chess.Game;
import chess.Move;
import chess.Piece;

@RunWith (BlockJUnit4ClassRunner.class)
public class MovementTest {
	
	private Game game = new Game();
	
	private Board board;
	
	private FENReader fenReader = new FENReader();
	
	@Rule
	public JUnitSoftAssertions softly = new JUnitSoftAssertions();
	
	@Test
	public void testPawnMove1() {
		setupTest("4k3/8/8/8/8/8/6P1/4K3 w - - 0 1");
		Piece piece = board.getPiece(1, 1, board.getPieces());
		softly.assertThat(piece.getAvailableMoves(board.getPieces()))
			.as("Expected available moves to have size 2").hasSize(2);
	}
	
	@Test
	public void testPawnMove2() {
		setupTest("4k3/8/8/8/8/6P1/6P1/4K3 w - - 0 1");
		Piece piece = board.getPiece(1, 2, board.getPieces());
		softly.assertThat(piece.getAvailableMoves(board.getPieces()))
			.as("Expected available moves to have size 1").hasSize(1);
		piece = board.getPiece(1, 1, board.getPieces());
		softly.assertThat(piece.getAvailableMoves(board.getPieces()))
			.as("Expected available moves to be empty").isEmpty();
	}
	
	@Test
	public void testPawnTake1() {
		setupTest("4k3/8/8/5p2/6P1/8/8/4K3 w - - 0 1");
		Piece piece = board.getPiece(1, 3, board.getPieces());
		softly.assertThat(piece.getAvailableMoves(board.getPieces()))
			.as("Expected available moves to have size 2").hasSize(2);
		piece = board.getPiece(2, 4, board.getPieces());
		softly.assertThat(piece.getAvailableMoves(board.getPieces()))
			.as("Expected available moves to have size 2").hasSize(2);
	}
	
	@Test
	public void testEnPassantAvailable() {
		setupTest("4k3/8/8/5pP1/8/8/8/4K3 w - c6 0 1");
		Piece piece = board.getPiece(1, 4, board.getPieces());
		softly.assertThat(piece.getAvailableMoves(board.getPieces()))
			.as("Expected available moves to have size 2").hasSize(2);
	}
	
	@Test
	public void testEnPassantUnavailable() {
		setupTest("4k3/8/8/5pP1/8/8/8/4K3 w - - 0 1");
		Piece piece = board.getPiece(1, 4, board.getPieces());
		softly.assertThat(piece.getAvailableMoves(board.getPieces()))
			.as("Expected available moves to have size 1").hasSize(1);
	}
	
	@Test
	public void testCastlingAvailability() {
		setupTest("4k3/8/8/8/8/8/8/R3K2R w - - 0 1");
		Piece piece = board.getPiece(3, 0, board.getPieces());
		softly.assertThat(piece.getAvailableMoves(board.getPieces()))
			.as("Expected available moves to have size 5").hasSize(5);

		setupTest("4k3/1r6/8/8/8/8/8/R3K2R w KQ - 0 1");
		piece = board.getPiece(3, 0, board.getPieces());
		softly.assertThat(piece.getAvailableMoves(board.getPieces()))
			.as("Expected available moves to have size 7").hasSize(7);

		setupTest("4k3/3r4/8/8/8/8/8/R3K2R w KQ - 0 1");
		piece = board.getPiece(3, 0, board.getPieces());
		softly.assertThat(piece.getAvailableMoves(board.getPieces()))
			.as("Expected available moves to have size 4").hasSize(4);

		setupTest("4k3/5r2/8/8/8/8/8/R3K2R w KQ - 0 1");
		piece = board.getPiece(3, 0, board.getPieces());
		softly.assertThat(piece.getAvailableMoves(board.getPieces()))
			.as("Expected available moves to have size 4").hasSize(4);

		setupTest("4k3/2r5/8/8/8/8/8/R3K2R w KQ - 0 1");
		piece = board.getPiece(3, 0, board.getPieces());
		softly.assertThat(piece.getAvailableMoves(board.getPieces()))
			.as("Expected available moves to have size 6").hasSize(6);

		setupTest("4k3/6r1/8/8/8/8/8/R3K2R w KQ - 0 1");
		piece = board.getPiece(3, 0, board.getPieces());
		softly.assertThat(piece.getAvailableMoves(board.getPieces()))
			.as("Expected available moves to have size 6").hasSize(6);

		setupTest("4k3/4r3/8/8/8/8/8/R3K2R w KQ - 0 1");
		piece = board.getPiece(3, 0, board.getPieces());
		softly.assertThat(piece.getAvailableMoves(board.getPieces()))
			.as("Expected available moves to have size 4").hasSize(4);
	}
	
	@Test
	public void testEscapeCheck() {
		setupTest("4k3/8/8/8/8/8/7b/7K w - - 0 1");
		Piece piece = board.getPiece(0, 0, board.getPieces());
		softly.assertThat(piece.getAvailableMoves(board.getPieces()))
			.as("Expected available moves to have size 2").hasSize(2);
		softly.assertThat(piece.getAvailableMoves(board.getPieces()))
			.as("Expected square not in available moves").contains(board.getSquare(0, 1));
		softly.assertThat(piece.getAvailableMoves(board.getPieces()))
			.as("Expected square not in available moves").contains(board.getSquare(1, 1));
		
		setupTest("4k3/6rr/8/8/8/8/R7/7K w - - 0 1");
		softly.assertThat(piece.getAvailableMoves(board.getPieces()))
			.as("Expected available moves to be empty").isEmpty();
		piece = board.getPiece(7, 1, board.getPieces());
		softly.assertThat(piece.getAvailableMoves(board.getPieces()))
			.as("Expected available moves to have size 1").hasSize(1);
	}
	
	private void setupTest(String fen) {
		board = fenReader.read(fen, game);
		game.setBoard(board);
	}
}