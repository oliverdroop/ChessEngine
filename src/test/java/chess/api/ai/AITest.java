package chess.api.ai;

import chess.api.FENReader;
import chess.api.FENWriter;
import chess.api.PieceConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.BiFunction;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class AITest {

	private static final int DEPTH = 5;

	private PieceConfiguration pieceConfiguration;

	private PieceConfiguration newPieceConfiguration;

    @ParameterizedTest
    @MethodSource("providePositionEvaluatorArguments")
	void testAITakesQueen_edgeOfBoard(BiFunction<PieceConfiguration, Integer, PieceConfiguration> aiFunction) {
		setupTest("7k/8/8/q7/1P6/8/8/7K w - - 0 1");
		newPieceConfiguration = aiFunction.apply(pieceConfiguration, DEPTH);
		Assertions.assertThat(FENWriter.write(newPieceConfiguration))
				.as("White pawn should take black queen")
				.isEqualTo("7k/8/8/P7/8/8/8/7K b - - 0 1");
	}

    @ParameterizedTest
    @MethodSource("providePositionEvaluatorArguments")
	void testAITakesQueen_middleOfBoard(BiFunction<PieceConfiguration, Integer, PieceConfiguration> aiFunction) {
		setupTest("4k3/8/8/3q1p2/4P3/8/8/4K3 w - - 0 1");
		newPieceConfiguration = aiFunction.apply(pieceConfiguration, DEPTH);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("White pawn should take black queen")
				.isEqualTo("4k3/8/8/3P1p2/8/8/8/4K3 b - - 0 1");
	}

    @ParameterizedTest
    @MethodSource("providePositionEvaluatorArguments")
	void testAIChoosesCheckmate_bishopWithRooks(BiFunction<PieceConfiguration, Integer, PieceConfiguration> aiFunction) {
		setupTest("k5B1/7R/8/8/8/8/P1PPPPPP/1R2K3 w - - 0 1");
		newPieceConfiguration = aiFunction.apply(pieceConfiguration, DEPTH);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("White bishop should force checkmate")
				.isEqualTo("k7/7R/8/3B4/8/8/P1PPPPPP/1R2K3 b - - 1 1");
	}
    
    @ParameterizedTest
    @MethodSource("providePositionEvaluatorArguments")
	void testAIChoosesCheckmate_bishopWithKnights(BiFunction<PieceConfiguration, Integer, PieceConfiguration> aiFunction) {
		setupTest("k5B1/3N4/8/1N6/8/8/8/4K3 w - - 0 1");
		newPieceConfiguration = aiFunction.apply(pieceConfiguration, DEPTH);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("White bishop should force checkmate")
				.isEqualTo("k7/3N4/8/1N1B4/8/8/8/4K3 b - - 1 1");
	}
    
    @ParameterizedTest
    @MethodSource("providePositionEvaluatorArguments")
	void testAIChoosesCheckmate_twoRooks(BiFunction<PieceConfiguration, Integer, PieceConfiguration> aiFunction) {
		setupTest("k7/7R/6R1/8/8/8/p3K3/8 w - - 0 1");
		newPieceConfiguration = aiFunction.apply(pieceConfiguration, DEPTH);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("White rook should force checkmate")
				.isEqualTo("k5R1/7R/8/8/8/8/p3K3/8 b - - 1 1");
	}

    @ParameterizedTest
    @MethodSource("providePositionEvaluatorArguments")
	void testAIChoosesCheckmate_twoRooksTowardsEdge(BiFunction<PieceConfiguration, Integer, PieceConfiguration> aiFunction) {
		setupTest("K7/8/8/2r5/2r5/8/8/7k b - - 0 1");
		newPieceConfiguration = aiFunction.apply(pieceConfiguration, DEPTH);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("White rook should progress towards forcing checkmate")
				.isIn("K7/8/8/1r6/2r5/8/8/7k w - - 1 2",
						"K7/8/8/2r5/1r6/8/8/7k w - - 1 2",
						"K7/8/8/2r5/r7/8/8/7k w - - 1 2",
						"K7/8/8/r7/2r5/8/8/7k w - - 1 2");
	}

    @ParameterizedTest
    @MethodSource("providePositionEvaluatorArguments")
	void testAIChoosesCheckmate_foolsMate(BiFunction<PieceConfiguration, Integer, PieceConfiguration> aiFunction) {
		setupTest("rnbqkbnr/pppp1ppp/8/4p3/6P1/5P2/PPPPP2P/RNBQKBNR b KQkq - 0 2");
		newPieceConfiguration = aiFunction.apply(pieceConfiguration, DEPTH);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("Black bishop should force checkmate")
				.isEqualTo("rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3");
	}

    @ParameterizedTest
    @MethodSource("providePositionEvaluatorArguments")
	void testAIUpgradesPawn(BiFunction<PieceConfiguration, Integer, PieceConfiguration> aiFunction) {
		setupTest("8/kBPN4/8/8/8/8/8/KR6 w - - 0 1");
		newPieceConfiguration = aiFunction.apply(pieceConfiguration, DEPTH);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("Promoting white pawn should choose knight to avoid stalemate")
				.isEqualTo("2N5/kB1N4/8/8/8/8/8/KR6 b - - 0 1");
	}

    @ParameterizedTest
    @MethodSource("providePositionEvaluatorArguments")
	void testAIAvoidsStalemate(BiFunction<PieceConfiguration, Integer, PieceConfiguration> aiFunction) {
		setupTest("7k/8/5N2/5N2/8/8/8/K7 w - - 0 1");
		newPieceConfiguration = aiFunction.apply(pieceConfiguration, DEPTH);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("One of the knights should move to avoid blocking the black king in the corner")
				.doesNotContain("5N2/5N2");
	}

    @ParameterizedTest
    @MethodSource("providePositionEvaluatorArguments")
	void testAIAvoidsCheckmate_earlyGame(BiFunction<PieceConfiguration, Integer, PieceConfiguration> aiFunction) {
		setupTest("rn2kbnr/pbqp1pp1/1pp1p3/4N1Bp/3PP3/2NB1Q2/PPP2PPP/R3K2R b KQkq - 2 8");
		newPieceConfiguration = aiFunction.apply(pieceConfiguration, DEPTH);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("Black should avoid mate in 1")
				.isIn("rn2kbnr/pbqp2p1/1pp1pp2/4N1Bp/3PP3/2NB1Q2/PPP2PPP/R3K2R w KQkq - 0 9",
						"rn2kbnr/pbqp2p1/1pp1p3/4NpBp/3PP3/2NB1Q2/PPP2PPP/R3K2R w KQkq f6 0 9",
						"rn2kb1r/pbqp1pp1/1pp1pn2/4N1Bp/3PP3/2NB1Q2/PPP2PPP/R3K2R w KQkq - 3 9",
						"rn1k1bnr/pbqp1pp1/1pp1p3/4N1Bp/3PP3/2NB1Q2/PPP2PPP/R3K2R w KQkq - 3 9",
						"rn2kb1r/pbqp1pp1/1pp1p2n/4N1Bp/3PP3/2NB1Q2/PPP2PPP/R3K2R w KQkq - 3 9",
						"rn2kb1r/pbqpnpp1/1pp1p3/4N1Bp/3PP3/2NB1Q2/PPP2PPP/R3K2R w KQkq - 3 9",
						"rn2kbnr/pbq2pp1/1pppp3/4N1Bp/3PP3/2NB1Q2/PPP2PPP/R3K2R w KQkq - 0 9",
						"rn2kbnr/pbq2pp1/1pp1p3/3pN1Bp/3PP3/2NB1Q2/PPP2PPP/R3K2R w KQkq d6 0 9",
						"rn2kbnr/pb1p1pp1/1pp1p3/4q1Bp/3PP3/2NB1Q2/PPP2PPP/R3K2R w KQkq - 0 9");
	}

    @ParameterizedTest
    @MethodSource("providePositionEvaluatorArguments")
	void testAIAvoidsCheckmate_lateGame(BiFunction<PieceConfiguration, Integer, PieceConfiguration> aiFunction) {
		setupTest("k7/ppp5/8/8/8/8/8/QK5B b - - 2 8");
		newPieceConfiguration = aiFunction.apply(pieceConfiguration, DEPTH);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("Black should avoid mate in 1")
				.isIn("k7/1pp5/p7/8/8/8/8/QK5B w - - 0 9",
						"k7/1pp5/8/p7/8/8/8/QK5B w - a6 0 9");
	}

    @ParameterizedTest
    @MethodSource("providePositionEvaluatorArguments")
	void simpleCornerTest_chooseCheckmateOverStalemate(BiFunction<PieceConfiguration, Integer, PieceConfiguration> aiFunction) {
		setupTest("k7/2P5/K7/8/8/8/8/8 w - - 0 50");
		newPieceConfiguration = aiFunction.apply(pieceConfiguration, DEPTH);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("White should choose checkmate")
				.isIn("k1Q5/8/K7/8/8/8/8/8 b - - 0 50",
						"k1R5/8/K7/8/8/8/8/8 b - - 0 50");
	}

    @ParameterizedTest
    @MethodSource("providePositionEvaluatorArguments")
	void fiftyMoveRuleTest_loseByMovingKing(BiFunction<PieceConfiguration, Integer, PieceConfiguration> aiFunction) {
		setupTest("7K/7P/8/8/8/8/8/k7 w - - 99 50");
		newPieceConfiguration = aiFunction.apply(pieceConfiguration, DEPTH);
		assertThat(newPieceConfiguration).isNull();
	}

    @ParameterizedTest
    @MethodSource("providePositionEvaluatorArguments")
	void fiftyMoveRuleTest_continueByMovingPawn(BiFunction<PieceConfiguration, Integer, PieceConfiguration> aiFunction) {
		setupTest("7K/7P/8/8/7r/2P4k/8/B7 w - - 99 50");
		newPieceConfiguration = aiFunction.apply(pieceConfiguration, DEPTH);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("Expected white to avoid losing by moving a pawn")
				.isEqualTo("7K/7P/8/8/2P4r/7k/8/B7 b - - 0 50");
	}

    @ParameterizedTest
    @MethodSource("providePositionEvaluatorArguments")
	void testAIDoesNotBlunderQueen_earlyGame1(BiFunction<PieceConfiguration, Integer, PieceConfiguration> aiFunction) {
		setupTest("r1b1kbnr/pppp1ppp/2n1p3/8/3PP2q/3B2P1/PPP2P1P/RNBQK1NR b KQkq - 5 4");
		newPieceConfiguration = aiFunction.apply(pieceConfiguration, DEPTH);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("Expected black not to blunder its queen")
				.isNotEqualTo("r1b1kbnr/pppp1ppp/2n1p3/8/3PP3/3B2q1/PPP2P1P/RNBQK1NR w KQkq - 0 5");
	}


    @ParameterizedTest
    @MethodSource("providePositionEvaluatorArguments")
	void testAIDoesNotBlunderQueen_earlyGame2(BiFunction<PieceConfiguration, Integer, PieceConfiguration> aiFunction) {
		setupTest("r1b1k1nr/pppp1ppp/2n1p3/8/1b1PP2q/2PB2P1/PP3P1P/RNBQK1NR b KQkq - 7 5");
		newPieceConfiguration = aiFunction.apply(pieceConfiguration, DEPTH);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("Expected black not to blunder its queen")
				.isIn("r1b1k1nr/pppp1ppp/2n1pq2/8/1b1PP3/2PB2P1/PP3P1P/RNBQK1NR w KQkq - 8 6",
						"r1b1k1nr/pppp1ppp/2n1p3/8/1b1PP3/2PB2P1/PP3P1P/RNBQK1NR w KQkq - 8 6",
						"r1bqk1nr/pppp1ppp/2n1p3/8/1b1PP3/2PB2P1/PP3P1P/RNBQK1NR w KQkq - 8 6",
						"r1b1k1nr/pppp1ppp/2n1p3/8/3PP2q/2bB2P1/PP3P1P/RNBQK1NR w KQkq - 0 6");
	}

    private static Stream<Arguments> providePositionEvaluatorArguments() {
        return Stream.of(
            Arguments.of(
                (BiFunction<PieceConfiguration, Integer, PieceConfiguration>) ConcurrentPositionEvaluator::getBestMoveRecursively
            ),
            Arguments.of(
                (BiFunction<PieceConfiguration, Integer, PieceConfiguration>) BreadthFirstPositionEvaluator::getBestMoveRecursively
            )
        );
    }
	
	private void setupTest(String fen) {
		pieceConfiguration = FENReader.read(fen);
	}
}

