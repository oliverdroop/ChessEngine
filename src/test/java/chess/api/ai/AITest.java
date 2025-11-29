package chess.api.ai;

import chess.api.FENReader;
import chess.api.FENWriter;
import chess.api.GameEndType;
import chess.api.configuration.IntsPieceConfiguration;
import chess.api.configuration.LongsPieceConfiguration;
import chess.api.configuration.PieceConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiFunction;
import java.util.stream.Stream;

import static chess.api.utils.TestUtils.loadConfigurationWithHistory;
import static org.assertj.core.api.Assertions.assertThat;

public class AITest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AITest.class);

	private static final int DEPTH = 4;

	private PieceConfiguration pieceConfiguration;

	private PieceConfiguration newPieceConfiguration;

    @ParameterizedTest
    @MethodSource("providePositionEvaluatorArguments")
	void testAITakesQueen_edgeOfBoard(
        Class<? extends PieceConfiguration> configurationClass,
        BiFunction<PieceConfiguration, Integer, PieceConfiguration> aiFunction)
    {
        setupTest("7k/8/8/q7/1P6/8/8/7K w - - 0 1", configurationClass);
		newPieceConfiguration = aiFunction.apply(pieceConfiguration, DEPTH);
		Assertions.assertThat(FENWriter.write(newPieceConfiguration))
				.as("White pawn should take black queen")
				.isEqualTo("7k/8/8/P7/8/8/8/7K b - - 0 1");
	}

    @ParameterizedTest
    @MethodSource("providePositionEvaluatorArguments")
	void testAITakesQueen_middleOfBoard(
        Class<? extends PieceConfiguration> configurationClass,
        BiFunction<PieceConfiguration, Integer, PieceConfiguration> aiFunction)
    {
        setupTest("4k3/8/8/3q1p2/4P3/8/8/4K3 w - - 0 1", configurationClass);
		newPieceConfiguration = aiFunction.apply(pieceConfiguration, DEPTH);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("White pawn should take black queen")
				.isEqualTo("4k3/8/8/3P1p2/8/8/8/4K3 b - - 0 1");
	}

    @ParameterizedTest
    @MethodSource("providePositionEvaluatorArguments")
	void testAIChoosesCheckmate_bishopWithRooks(
        Class<? extends PieceConfiguration> configurationClass,
        BiFunction<PieceConfiguration, Integer, PieceConfiguration> aiFunction)
    {
        setupTest("k5B1/7R/8/8/8/8/P1PPPPPP/1R2K3 w - - 0 1", configurationClass);
		newPieceConfiguration = aiFunction.apply(pieceConfiguration, DEPTH);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("White bishop should force checkmate")
				.isEqualTo("k7/7R/8/3B4/8/8/P1PPPPPP/1R2K3 b - - 1 1");
	}
    
    @ParameterizedTest
    @MethodSource("providePositionEvaluatorArguments")
	void testAIChoosesCheckmate_bishopWithKnights(
        Class<? extends PieceConfiguration> configurationClass,
        BiFunction<PieceConfiguration, Integer, PieceConfiguration> aiFunction)
    {
        setupTest("k5B1/3N4/8/1N6/8/8/8/4K3 w - - 0 1", configurationClass);
		newPieceConfiguration = aiFunction.apply(pieceConfiguration, DEPTH);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("White bishop should force checkmate")
				.isEqualTo("k7/3N4/8/1N1B4/8/8/8/4K3 b - - 1 1");
	}
    
    @ParameterizedTest
    @MethodSource("providePositionEvaluatorArguments")
	void testAIChoosesCheckmate_twoRooks(
        Class<? extends PieceConfiguration> configurationClass,
        BiFunction<PieceConfiguration, Integer, PieceConfiguration> aiFunction)
    {
        setupTest("k7/7R/6R1/8/8/8/p3K3/8 w - - 0 1", configurationClass);
		newPieceConfiguration = aiFunction.apply(pieceConfiguration, DEPTH);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("White rook should force checkmate")
				.isEqualTo("k5R1/7R/8/8/8/8/p3K3/8 b - - 1 1");
	}

    @ParameterizedTest
    @MethodSource("providePositionEvaluatorArguments")
	void testAIChoosesCheckmate_twoRooksTowardsEdge(
        Class<? extends PieceConfiguration> configurationClass,
        BiFunction<PieceConfiguration, Integer, PieceConfiguration> aiFunction)
    {
        setupTest("K7/8/8/2r5/2r5/8/8/7k b - - 0 1", configurationClass);
		newPieceConfiguration = aiFunction.apply(pieceConfiguration, DEPTH);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("Black rook should progress towards forcing checkmate")
				.isIn("K7/8/8/1r6/2r5/8/8/7k w - - 1 2",
						"K7/8/8/2r5/1r6/8/8/7k w - - 1 2",
						"K7/8/8/2r5/r7/8/8/7k w - - 1 2",
						"K7/8/8/r7/2r5/8/8/7k w - - 1 2");
	}

    @ParameterizedTest
    @MethodSource("providePositionEvaluatorArguments")
	void testAIChoosesCheckmate_foolsMate(
        Class<? extends PieceConfiguration> configurationClass,
        BiFunction<PieceConfiguration, Integer, PieceConfiguration> aiFunction)
    {
        setupTest("rnbqkbnr/pppp1ppp/8/4p3/6P1/5P2/PPPPP2P/RNBQKBNR b KQkq - 0 2", configurationClass);
		newPieceConfiguration = aiFunction.apply(pieceConfiguration, DEPTH);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("Black bishop should force checkmate")
				.isEqualTo("rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3");
	}

    @ParameterizedTest
    @MethodSource("providePositionEvaluatorArguments")
	void testAIUpgradesPawn(
        Class<? extends PieceConfiguration> configurationClass,
        BiFunction<PieceConfiguration, Integer, PieceConfiguration> aiFunction)
    {
        setupTest("8/kBPN4/8/8/8/8/8/KR6 w - - 0 1", configurationClass);
		newPieceConfiguration = aiFunction.apply(pieceConfiguration, DEPTH);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("Promoting white pawn should choose knight to avoid stalemate")
				.isEqualTo("2N5/kB1N4/8/8/8/8/8/KR6 b - - 0 1");
	}

    @ParameterizedTest
    @MethodSource("providePositionEvaluatorArguments")
    void testAIAvoidsStalemate_toExtraDepth(
        Class<? extends PieceConfiguration> configurationClass,
        BiFunction<PieceConfiguration, Integer, PieceConfiguration> aiFunction)
    {
        setupTest("7k/8/5N2/5N2/8/8/8/K7 w - - 0 1", configurationClass);
        newPieceConfiguration = aiFunction.apply(pieceConfiguration, DEPTH + 1);
        assertThat(FENWriter.write(newPieceConfiguration))
            .as("One of the knights should move to avoid blocking the black king in the corner")
            .doesNotContain("5N2/5N2");
    }

    @ParameterizedTest
    @MethodSource("providePositionEvaluatorArguments")
	void testAIAvoidsCheckmate_earlyGame(
        Class<? extends PieceConfiguration> configurationClass,
        BiFunction<PieceConfiguration, Integer, PieceConfiguration> aiFunction)
    {
        setupTest("rn2kbnr/pbqp1pp1/1pp1p3/4N1Bp/3PP3/2NB1Q2/PPP2PPP/R3K2R b KQkq - 2 8", configurationClass);
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
	void testAIAvoidsCheckmate_lateGame(
        Class<? extends PieceConfiguration> configurationClass,
        BiFunction<PieceConfiguration, Integer, PieceConfiguration> aiFunction)
    {
        setupTest("k7/ppp5/8/8/8/8/8/QK5B b - - 2 8", configurationClass);
		newPieceConfiguration = aiFunction.apply(pieceConfiguration, DEPTH);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("Black should avoid mate in 1")
				.isIn("k7/1pp5/p7/8/8/8/8/QK5B w - - 0 9",
						"k7/1pp5/8/p7/8/8/8/QK5B w - a6 0 9");
	}

    @ParameterizedTest
    @MethodSource("providePositionEvaluatorArguments")
	void simpleCornerTest_chooseCheckmateOverStalemate(
        Class<? extends PieceConfiguration> configurationClass,
        BiFunction<PieceConfiguration, Integer, PieceConfiguration> aiFunction)
    {
        setupTest("k7/2P5/K7/8/8/8/8/8 w - - 0 50", configurationClass);
		newPieceConfiguration = aiFunction.apply(pieceConfiguration, DEPTH);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("White should choose checkmate")
				.isIn("k1Q5/8/K7/8/8/8/8/8 b - - 0 50",
						"k1R5/8/K7/8/8/8/8/8 b - - 0 50");
	}

    @ParameterizedTest
    @MethodSource("providePositionEvaluatorArguments")
    void testAiChoosesDraw_fiftyMoveRuleFailure(
        Class<? extends PieceConfiguration> configurationClass,
        BiFunction<PieceConfiguration, Integer, PieceConfiguration> aiFunction)
    {
        setupTest("7K/7P/8/8/8/8/8/k7 w - - 99 50", configurationClass);
        newPieceConfiguration = aiFunction.apply(pieceConfiguration, DEPTH);
        assertThat(newPieceConfiguration)
            .isNotNull()
            .extracting(FENWriter::write)
            .as("Expected white to draw by moving king and failing fifty move rule " +
                "when the king is the only piece which can move")
            .isIn("6K1/7P/8/8/8/8/8/k7 b - - 100 50",
                "8/6KP/8/8/8/8/8/k7 b - - 100 50");
    }

    @ParameterizedTest
    @MethodSource("providePositionEvaluatorArguments")
    void testAiChoosesDraw_fiftyMoveRuleFailureWithPieceDisadvantage(
        Class<? extends PieceConfiguration> configurationClass,
        BiFunction<PieceConfiguration, Integer, PieceConfiguration> aiFunction)
    {
        setupTest("7K/7P/8/8/3b4/4P3/8/k2q4 w - - 99 50", configurationClass);
        newPieceConfiguration = aiFunction.apply(pieceConfiguration, DEPTH);
        assertThat(newPieceConfiguration)
            .isNotNull()
            .extracting(FENWriter::write)
            .as("Expected white to draw by moving king and failing fifty move rule " +
                "even when other moves are available")
            .isEqualTo("6K1/7P/8/8/3b4/4P3/8/k2q4 b - - 100 50");
    }

    @ParameterizedTest
    @MethodSource("providePositionEvaluatorArguments")
	void fiftyMoveRuleTest_continueByMovingPawn(
        Class<? extends PieceConfiguration> configurationClass,
        BiFunction<PieceConfiguration, Integer, PieceConfiguration> aiFunction)
    {
        setupTest("7K/7P/8/8/7r/2P4k/8/B7 w - - 99 50", configurationClass);
		newPieceConfiguration = aiFunction.apply(pieceConfiguration, DEPTH);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("Expected white to avoid draw by moving a pawn")
				.isEqualTo("7K/7P/8/8/2P4r/7k/8/B7 b - - 0 50");
	}

    @ParameterizedTest
    @MethodSource("providePositionEvaluatorArguments")
	void testAIDoesNotBlunderQueen_earlyGame1(
            Class<? extends PieceConfiguration> configurationClass,
            BiFunction<PieceConfiguration, Integer, PieceConfiguration> aiFunction)
    {
		setupTest("r1b1kbnr/pppp1ppp/2n1p3/8/3PP2q/3B2P1/PPP2P1P/RNBQK1NR b KQkq - 5 4", configurationClass);
		newPieceConfiguration = aiFunction.apply(pieceConfiguration, DEPTH);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("Expected black not to blunder its queen")
				.isNotEqualTo("r1b1kbnr/pppp1ppp/2n1p3/8/3PP3/3B2q1/PPP2P1P/RNBQK1NR w KQkq - 0 5");
	}


    @ParameterizedTest
    @MethodSource("providePositionEvaluatorArguments")
	void testAIDoesNotBlunderQueen_earlyGame2(
            Class<? extends PieceConfiguration> configurationClass,
            BiFunction<PieceConfiguration, Integer, PieceConfiguration> aiFunction)
    {
		setupTest("r1b1k1nr/pppp1ppp/2n1p3/8/1b1PP2q/2PB2P1/PP3P1P/RNBQK1NR b KQkq - 7 5", configurationClass);
		newPieceConfiguration = aiFunction.apply(pieceConfiguration, DEPTH);
		assertThat(FENWriter.write(newPieceConfiguration))
				.as("Expected black not to blunder its queen")
				.isIn("r1b1k1nr/pppp1ppp/2n1pq2/8/1b1PP3/2PB2P1/PP3P1P/RNBQK1NR w KQkq - 8 6",
						"r1b1k1nr/pppp1ppp/2n1p3/8/1b1PP3/2PB2P1/PP3P1P/RNBQK1NR w KQkq - 8 6",
						"r1bqk1nr/pppp1ppp/2n1p3/8/1b1PP3/2PB2P1/PP3P1P/RNBQK1NR w KQkq - 8 6",
						"r1b1k1nr/pppp1ppp/2n1p3/8/3PP2q/2bB2P1/PP3P1P/RNBQK1NR w KQkq - 0 6");
	}

    @ParameterizedTest
    @MethodSource("providePositionEvaluatorArguments")
    void deriveGameEndType(
            Class<? extends PieceConfiguration> configurationClass,
            BiFunction<PieceConfiguration, Integer, PieceConfiguration> aiFunction)
    {
        setupTest("r3q1nN/ppp4p/8/kb5R/1N1Q4/6P1/PP1BB3/R3K3 b Q - 6 20", configurationClass);
        newPieceConfiguration = aiFunction.apply(pieceConfiguration, DEPTH);
        assertThat(newPieceConfiguration.isCheck()).isTrue();
        assertThat(DepthFirstPositionEvaluator.deriveGameEndType(newPieceConfiguration))
            .isEqualTo(GameEndType.BLACK_VICTORY);
    }

    @ParameterizedTest
    @MethodSource("providePositionEvaluatorArguments")
    void testAIAvoidsDraw_threefoldRepetition(
        Class<? extends PieceConfiguration> configurationClass,
        BiFunction<PieceConfiguration, Integer, PieceConfiguration> aiFunction
    ) {
        pieceConfiguration = loadConfigurationWithHistory(
            configurationClass,
            FENWriter.STARTING_POSITION,
            "rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq d3 0 1",
            "rnbqkb1r/pppppppp/5n2/8/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 1 2",
            "rnbqkb1r/pppppppp/5n2/8/2PP4/8/PP2PPPP/RNBQKBNR b KQkq c3 0 2",
            "rnbqkb1r/pppp1ppp/4pn2/8/2PP4/8/PP2PPPP/RNBQKBNR w KQkq - 0 3",
            "rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq d3 0 1",
            "rnbqkbnr/ppppp1pp/8/5p2/3P4/8/PPP1PPPP/RNBQKBNR w KQkq f6 0 2",
            "rnbqkbnr/ppppp1pp/8/5p2/2PP4/8/PP2PPPP/RNBQKBNR b KQkq c3 0 2",
            "rnbqkbnr/pppp2pp/4p3/5p2/2PP4/8/PP2PPPP/RNBQKBNR w KQkq - 0 3",
            "rnbqkbnr/pppp2pp/4p3/5p2/2PP4/2N5/PP2PPPP/R1BQKBNR b KQkq - 1 3",
            "rnbqk1nr/pppp2pp/4p3/5p2/1bPP4/2N5/PP2PPPP/R1BQKBNR w KQkq - 2 4",
            "rnbqk1nr/pppp2pp/4p3/5p2/1bPP4/2N5/PPQ1PPPP/R1B1KBNR b KQkq - 3 4",
            "rnbqk1nr/pppp2pp/4p3/5p2/2PP4/2b5/PPQ1PPPP/R1B1KBNR w KQkq - 0 5",
            "rnbqk1nr/pppp2pp/4p3/5p2/2PP4/2Q5/PP2PPPP/R1B1KBNR b KQkq - 0 5",
            "rnb1k1nr/pppp2pp/4p3/5p2/2PP3q/2Q5/PP2PPPP/R1B1KBNR w KQkq - 1 6",
            "rnb1k1nr/pppp2pp/4p3/5p2/2PP3q/2Q2N2/PP2PPPP/R1B1KB1R b KQkq - 2 6",
            "rnb1k1nr/pppp2pp/4p3/5p2/2PP2q1/2Q2N2/PP2PPPP/R1B1KB1R w KQkq - 3 7",
            "rnb1k1nr/pppp2pp/4p3/5p2/2PP2q1/2Q2N1P/PP2PPP1/R1B1KB1R b KQkq - 0 7",
            "rnb1k1nr/pppp2pp/4p3/5p1q/2PP4/2Q2N1P/PP2PPP1/R1B1KB1R w KQkq - 1 8",
            "rnb1k1nr/pppp2pp/4p3/5p1q/2PP1B2/2Q2N1P/PP2PPP1/R3KB1R b KQkq - 2 8",
            "rnb1k1nr/ppp3pp/3pp3/5p1q/2PP1B2/2Q2N1P/PP2PPP1/R3KB1R w KQkq - 0 9",
            "rnb1k1nr/ppp3pp/3pp3/5p1q/2PP1B2/2Q1PN1P/PP3PP1/R3KB1R b KQkq - 0 9",
            "r1b1k1nr/ppp3pp/2npp3/5p1q/2PP1B2/2Q1PN1P/PP3PP1/R3KB1R w KQkq - 1 10",
            "r1b1k1nr/ppp3pp/2npp3/5p1q/2PP1B2/2Q1PN1P/PP2BPP1/R3K2R b KQkq - 2 10",
            "r1b1k1nr/ppp3pp/2npp1q1/5p2/2PP1B2/2Q1PN1P/PP2BPP1/R3K2R w KQkq - 3 11",
            "r1b1k1nr/ppp3pp/2npp1q1/5p2/2PP1B2/2Q1PN1P/PP2BPP1/R4RK1 b kq - 4 11",
            "r1b1k1nr/ppp3pp/2npp3/5p1q/2PP1B2/2Q1PN1P/PP2BPP1/R4RK1 w kq - 5 12",
            "r1b1k1nr/ppp3pp/2npp3/5p1q/2PP1B2/P1Q1PN1P/1P2BPP1/R4RK1 b kq - 0 12",
            "r1b1k1nr/1pp3pp/p1npp3/5p1q/2PP1B2/P1Q1PN1P/1P2BPP1/R4RK1 w kq - 0 13",
            "r1b1k1nr/1pp3pp/p1npp3/5p1q/2PP1B2/P3PN1P/1PQ1BPP1/R4RK1 b kq - 1 13",
            "r1b1k1nr/1pp3pp/p2pp3/n4p1q/2PP1B2/P3PN1P/1PQ1BPP1/R4RK1 w kq - 2 14",
            "r1b1k1nr/1pp3pp/p2pp3/n4p1q/2PP1B2/P3PN1P/1PQ1BPP1/R3R1K1 b kq - 3 14",
            "r1b1k1nr/2p3pp/pp1pp3/n4p1q/2PP1B2/P3PN1P/1PQ1BPP1/R3R1K1 w kq - 0 15",
            "r1b1k1nr/2p3pp/pp1pp3/n4p1q/2PP1B2/P3PN1P/1PQ1BPP1/3RR1K1 b kq - 1 15",
            "r3k1nr/1bp3pp/pp1pp3/n4p1q/2PP1B2/P3PN1P/1PQ1BPP1/3RR1K1 w kq - 2 16",
            "r3k1nr/1bp3pp/pp1pp3/n4pNq/2PP1B2/P3P2P/1PQ1BPP1/3RR1K1 b kq - 3 16",
            "r3k1nr/1bp3pp/pp1pp1q1/n4pN1/2PP1B2/P3P2P/1PQ1BPP1/3RR1K1 w kq - 4 17",
            "r3k1nr/1bp3pp/pp1pp1q1/n4pN1/1PPP1B2/P3P2P/2Q1BPP1/3RR1K1 b kq b3 0 17",
            "r3k1nr/1bp3pp/ppnpp1q1/5pN1/1PPP1B2/P3P2P/2Q1BPP1/3RR1K1 w kq - 1 18",
            "r3k1nr/1bp3pp/ppnpp1q1/5p2/1PPP1B2/P3PN1P/2Q1BPP1/3RR1K1 b kq - 2 18",
            "r3k1nr/1bp3p1/ppnpp1qp/5p2/1PPP1B2/P3PN1P/2Q1BPP1/3RR1K1 w kq - 0 19",
            "r3k1nr/1bp3p1/ppnpp1qp/5p2/1PPP4/P3PN1P/2Q1BPPB/3RR1K1 b kq - 1 19",
            "r3k1nr/1bp3p1/ppnpp1q1/5p1p/1PPP4/P3PN1P/2Q1BPPB/3RR1K1 w kq - 0 20",
            "r3k1nr/1bp3p1/ppnpp1q1/5p1p/1PPP4/P3PN1P/2QRBPPB/4R1K1 b kq - 1 20",
            "2kr2nr/1bp3p1/ppnpp1q1/5p1p/1PPP4/P3PN1P/2QRBPPB/4R1K1 w - - 2 21",
            "2kr2nr/1bp3p1/ppnpp1q1/5p1p/1PPP3N/P3P2P/2QRBPPB/4R1K1 b - - 3 21",
            "2kr2nr/1bp3p1/ppnpp3/5pqp/1PPP3N/P3P2P/2QRBPPB/4R1K1 w - - 4 22",
            "2kr2nr/1bp3p1/ppnpp3/5pqp/1PPP4/P3PN1P/2QRBPPB/4R1K1 b - - 5 22",
            "2kr2nr/1bp3p1/ppnpp1q1/5p1p/1PPP4/P3PN1P/2QRBPPB/4R1K1 w - - 6 23",
            "2kr2nr/1bp3p1/ppnpp1q1/5p1p/1PPP3N/P3P2P/2QRBPPB/4R1K1 b - - 7 23",
            "2kr2nr/1bp3p1/ppnpp3/5pqp/1PPP3N/P3P2P/2QRBPPB/4R1K1 w - - 8 24",
            "2kr2nr/1bp3p1/ppnpp3/5pqp/1PPP4/P3PN1P/2QRBPPB/4R1K1 b - - 9 24"
        );
        newPieceConfiguration = aiFunction.apply(pieceConfiguration, 4);

        assertThat(FENWriter.write(newPieceConfiguration))
            .as("AI should avoid repeating the same position three times when not at a major disadvantage")
            .doesNotContain("2kr2nr/1bp3p1/ppnpp1q1/5p1p/1PPP4/P3PN1P/2QRBPPB/4R1K1 w - -");
    }

    @ParameterizedTest
    @MethodSource("providePositionEvaluatorArguments")
    void testAiChoosesDraw_threefoldRepetitionWithPieceDisadvantage(
        Class<? extends PieceConfiguration> configurationClass,
        BiFunction<PieceConfiguration, Integer, PieceConfiguration> aiFunction
    ) {
        pieceConfiguration = loadConfigurationWithHistory(
            configurationClass,
            FENWriter.STARTING_POSITION,
            "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
            "rnbqkb1r/pppppppp/5n2/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 1 2",
            "rnbqkb1r/pppppppp/5n2/8/4P1Q1/8/PPPP1PPP/RNB1KBNR b KQkq - 2 2",
            "rnbqkb1r/pppppppp/8/8/4P1n1/8/PPPP1PPP/RNB1KBNR w KQkq - 0 3",
            "rnbqkb1r/pppppppp/8/8/4P1n1/P7/1PPP1PPP/RNB1KBNR b KQkq - 0 3",
            "r1bqkb1r/pppppppp/2n5/8/4P1n1/P7/1PPP1PPP/RNB1KBNR w KQkq - 1 4",
            "r1bqkb1r/pppppppp/2n5/8/4P1n1/P1N5/1PPP1PPP/R1B1KBNR b KQkq - 2 4",
            "rnbqkb1r/pppppppp/8/8/4P1n1/P1N5/1PPP1PPP/R1B1KBNR w KQkq - 3 5",
            "rnbqkb1r/pppppppp/8/8/4P1n1/P7/1PPP1PPP/RNB1KBNR b KQkq - 4 5",
            "r1bqkb1r/pppppppp/2n5/8/4P1n1/P7/1PPP1PPP/RNB1KBNR w KQkq - 5 6",
            "r1bqkb1r/pppppppp/2n5/8/4P1n1/P1N5/1PPP1PPP/R1B1KBNR b KQkq - 6 6",
            "rnbqkb1r/pppppppp/8/8/4P1n1/P1N5/1PPP1PPP/R1B1KBNR w KQkq - 7 7"
        );

        newPieceConfiguration = aiFunction.apply(pieceConfiguration, DEPTH);

        assertThat(FENWriter.write(newPieceConfiguration))
            .as("AI should choose draw when at a considerable piece disadvantage")
            .isEqualTo("rnbqkb1r/pppppppp/8/8/4P1n1/P7/1PPP1PPP/RNB1KBNR b KQkq - 8 7");
    }

    @Test
    @Disabled
    void testBothConfigurationTypesSameOutput() {
        PieceConfiguration intsPieceConfiguration = FENReader.read(
            FENWriter.STARTING_POSITION, IntsPieceConfiguration.class);

        PieceConfiguration longsPieceConfiguration = FENReader.read(
            FENWriter.STARTING_POSITION, LongsPieceConfiguration.class);
        PieceConfiguration previousIntsConfiguration = null;
        PieceConfiguration previousLongsConfiguration = null;
        final int depth = 4;

        while(intsPieceConfiguration != null && longsPieceConfiguration != null) {
            LOGGER.info("IntsPieceConfiguration: {}, LongsPieceConfiguration: {}", intsPieceConfiguration, longsPieceConfiguration);
            previousIntsConfiguration = intsPieceConfiguration;
            previousLongsConfiguration = longsPieceConfiguration;
            final PieceConfiguration newIntsConfiguration = DepthFirstPositionEvaluator
                .getBestMoveRecursively(intsPieceConfiguration, depth);
            final PieceConfiguration newLongsConfiguration = DepthFirstPositionEvaluator
                .getBestMoveRecursively(longsPieceConfiguration, depth);
            final String intsConfigurationFen = newIntsConfiguration.toString();
            final String longsConfigurationFen = newLongsConfiguration.toString();
            assertThat(intsConfigurationFen)
                .as("A different IntsPieceConfiguration and LongsPieceConfiguration were chosen for the same input")
                .isEqualTo(longsConfigurationFen);
            intsPieceConfiguration = newIntsConfiguration;
            longsPieceConfiguration = newLongsConfiguration;
        }
        LOGGER.info(DepthFirstPositionEvaluator.deriveGameEndType(previousIntsConfiguration).toString());
        LOGGER.info(DepthFirstPositionEvaluator.deriveGameEndType(previousLongsConfiguration).toString());
    }

    @Test
    @Disabled
    void testBothEvaluatorsSameOutput() {
        PieceConfiguration pieceConfiguration = FENReader.read(FENWriter.STARTING_POSITION, IntsPieceConfiguration.class);
        PieceConfiguration previousConfiguration = null;
        final int depth = 4;

        while(pieceConfiguration != null) {
            LOGGER.info(pieceConfiguration.toString());
            previousConfiguration = pieceConfiguration;
            final PieceConfiguration depthFirstEvaluatorConfiguration = DepthFirstPositionEvaluator
                .getBestMoveRecursively(pieceConfiguration, depth);
            final PieceConfiguration breadthFirstEvaluatorConfiguration = BreadthFirstPositionEvaluator
                .getBestMoveRecursively(pieceConfiguration, depth);
            final String depthFirstFen = depthFirstEvaluatorConfiguration.toString();
            final String breadthFirstFen = breadthFirstEvaluatorConfiguration.toString();
            assertThat(breadthFirstFen)
                .as("Depth first and breadth first evaluators output different moves for same input")
                .isEqualTo(depthFirstFen);
            pieceConfiguration = depthFirstEvaluatorConfiguration;
        }
        LOGGER.info(DepthFirstPositionEvaluator.deriveGameEndType(previousConfiguration).toString());
    }

    private static Stream<Arguments> providePositionEvaluatorArguments() {
        return Stream.of(
            Arguments.of(
                IntsPieceConfiguration.class,
                (BiFunction<PieceConfiguration, Integer, PieceConfiguration>) ConcurrentPositionEvaluator::getBestMoveRecursively
            ),
            Arguments.of(
                IntsPieceConfiguration.class,
                (BiFunction<PieceConfiguration, Integer, PieceConfiguration>) BreadthFirstPositionEvaluator::getBestMoveRecursively
            ),
            Arguments.of(
                LongsPieceConfiguration.class,
                (BiFunction<PieceConfiguration, Integer, PieceConfiguration>) ConcurrentPositionEvaluator::getBestMoveRecursively
            ),
            Arguments.of(
                LongsPieceConfiguration.class,
                (BiFunction<PieceConfiguration, Integer, PieceConfiguration>) BreadthFirstPositionEvaluator::getBestMoveRecursively
            )
        );
    }
	
	private void setupTest(String fen, Class<? extends PieceConfiguration> configurationClass) {
        pieceConfiguration = FENReader.read(fen, configurationClass);
	}
}

