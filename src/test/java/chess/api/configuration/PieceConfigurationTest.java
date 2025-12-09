package chess.api.configuration;

import chess.api.FENReader;
import chess.api.FENWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.Stream;

import static chess.api.utils.TestUtils.loadConfigurationWithHistory;
import static org.assertj.core.api.Assertions.assertThat;

public class PieceConfigurationTest {

    private final PieceConfiguration pieceConfiguration = new IntsPieceConfiguration();

    @Test
    void testSetTurnSide_0() {
        pieceConfiguration.setAuxiliaryData(-1);
        pieceConfiguration.setTurnSide(0);
        assertThat(pieceConfiguration.getAuxiliaryData()).isEqualTo(0b11111111111111111111111111111110);
    }

    @Test
    void testSetTurnSide_1() {
        pieceConfiguration.setAuxiliaryData(0);
        pieceConfiguration.setTurnSide(1);
        assertThat(pieceConfiguration.getAuxiliaryData()).isEqualTo(0b00000000000000000000000000000001);
    }

    @Test
    void testGetTurnSide_0() {
        pieceConfiguration.setAuxiliaryData(0b11111111111111111111111111111110);
        assertThat(pieceConfiguration.getTurnSide()).isEqualTo(0);
    }

    @Test
    void testGetTurnSide_1() {
        pieceConfiguration.setAuxiliaryData(0b10000000000000000000000000000001);
        assertThat(pieceConfiguration.getTurnSide()).isEqualTo(1);
    }

    @ParameterizedTest
    @MethodSource("getCastlePositionValues")
    void testAddCastlePosition(int inputCastlePosition, int expectedAuxData) {
        pieceConfiguration.setAuxiliaryData(0);
        pieceConfiguration.addCastlePosition(inputCastlePosition);
        assertThat(pieceConfiguration.getAuxiliaryData()).isEqualTo(expectedAuxData);
    }

    @ParameterizedTest
    @MethodSource("getCastlePositionValues")
    void testRemoveCastlePosition(int inputCastlePosition, int expectedAuxNotData) {
        pieceConfiguration.setAuxiliaryData(-1);
        pieceConfiguration.removeCastlePosition(inputCastlePosition);
        int expectedAuxData = ~expectedAuxNotData;
        assertThat(pieceConfiguration.getAuxiliaryData()).isEqualTo(expectedAuxData);
    }

    @Test
    void testSetHalfMoveClock_0() {
        pieceConfiguration.setAuxiliaryData(-1);
        pieceConfiguration.setHalfMoveClock(0);
        assertThat(pieceConfiguration.getAuxiliaryData()).isEqualTo(0b11111111111111111111000000011111);
    }

    @Test
    void testSetHalfMoveClock_Max() {
        pieceConfiguration.setAuxiliaryData(0);
        pieceConfiguration.setHalfMoveClock(127);
        assertThat(pieceConfiguration.getAuxiliaryData()).isEqualTo(0b00000000000000000000111111100000);
    }

    @Test
    void testGetHalfMoveClock_0() {
        pieceConfiguration.setAuxiliaryData(0);
        assertThat(pieceConfiguration.getHalfMoveClock()).isEqualTo(0);
    }

    @Test
    void testGetHalfMoveClock_Max() {
        pieceConfiguration.setAuxiliaryData(-1);
        assertThat(pieceConfiguration.getHalfMoveClock()).isEqualTo(127);
    }

    @Test
    void testSetFullMoveNumber_0() {
        pieceConfiguration.setAuxiliaryData(-1);
        pieceConfiguration.setFullMoveNumber(0);
        assertThat(pieceConfiguration.getAuxiliaryData()).isEqualTo(0b11111110000000000000111111111111);
    }

    @Test
    void testSetFullMoveNumber_Max() {
        pieceConfiguration.setAuxiliaryData(0);
        pieceConfiguration.setFullMoveNumber(8191);
        assertThat(pieceConfiguration.getAuxiliaryData()).isEqualTo(0b00000001111111111111000000000000);
    }

    @Test
    void testGetFullMoveNumber_0() {
        pieceConfiguration.setAuxiliaryData(0);
        assertThat(pieceConfiguration.getFullMoveNumber()).isEqualTo(0);
    }

    @Test
    void testGetFullMoveNumber_Max() {
        pieceConfiguration.setAuxiliaryData(-1);
        assertThat(pieceConfiguration.getFullMoveNumber()).isEqualTo(8191);
    }

    @Test
    void testSetEnPassantSquare_Null() {
        pieceConfiguration.setAuxiliaryData(0);
        pieceConfiguration.setEnPassantSquare(-1);
        assertThat(pieceConfiguration.getAuxiliaryData()).isEqualTo(0b11111110000000000000000000000000);
    }

    @Test
    void testGetEnPassantSquare_Null() {
        pieceConfiguration.setAuxiliaryData(-1);
        assertThat(pieceConfiguration.getEnPassantSquare()).isLessThan(0);
    }

    @ParameterizedTest
    @MethodSource("getEnPassantSquareValues")
    void testSetEnPassantSquare(int inputEnPassantPosition, int expectedAuxData) {
        pieceConfiguration.setAuxiliaryData(-1);
        pieceConfiguration.setEnPassantSquare(inputEnPassantPosition);
        assertThat(pieceConfiguration.getAuxiliaryData()).isEqualTo(expectedAuxData);
    }

    @ParameterizedTest
    @MethodSource("getEnPassantSquareValues")
    void testGetEnPassantSquare(int expectedEnPassantPosition, int inputAuxData) {
        pieceConfiguration.setAuxiliaryData(inputAuxData);
        assertThat(pieceConfiguration.getEnPassantSquare()).isEqualTo(expectedEnPassantPosition);
    }

    @ParameterizedTest
    @MethodSource("getAlgebraicNotationFENs")
    void testGetAlgebraicNotation(
            String fen1,
            String fen2,
            String expectedNotation,
            Class<? extends PieceConfiguration> configurationClass)
    {
        PieceConfiguration pieceConfiguration1 = FENReader.read(fen1, configurationClass);
        PieceConfiguration pieceConfiguration2 = FENReader.read(fen2, configurationClass);

        assertThat(pieceConfiguration2.getAlgebraicNotation(pieceConfiguration1))
                .as("Unexpected algebraic notation")
                .isEqualTo(expectedNotation);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testGetAlgebraicNotation_fromStartingPosition(Class<? extends PieceConfiguration> configurationClass) {
        final PieceConfiguration pieceConfiguration1 = FENReader.read(FENWriter.STARTING_POSITION, configurationClass);
        final int pieceBitFlag = pieceConfiguration1.getPieceAtPosition(12);
        final List<String> algebraicNotations = pieceConfiguration1.getOnwardConfigurationsForPiece(pieceBitFlag)
            .stream()
            .map(pieceConfiguration -> pieceConfiguration.getAlgebraicNotation(pieceConfiguration1))
            .toList();

        assertThat(algebraicNotations).containsExactlyInAnyOrder("e2e3", "e2e4");
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testStartingDifferential(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = FENReader.read(
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", configurationClass);

        assertThat(pieceConfiguration.getValueDifferential())
                .as("The starting position piece values should be equal")
                .isEqualTo(0);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testPlayerTeamTotalValue(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = FENReader.read(
            "8/8/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", configurationClass);

        assertThat(pieceConfiguration.getValueDifferential())
                .as("The player side values should be totalled correctly")
                .isEqualTo(39);
    }

    @ParameterizedTest
    @ValueSource(classes = {IntsPieceConfiguration.class, LongsPieceConfiguration.class})
    void testOpponentTotalValue(Class<? extends PieceConfiguration> configurationClass) {
        PieceConfiguration pieceConfiguration = FENReader.read(
            "rnbqkbnr/pppppppp/8/8/8/8/8/8 w KQkq - 0 1", configurationClass);

        assertThat(pieceConfiguration.getValueDifferential())
                .as("The opposing side values should be totalled correctly")
                .isEqualTo(-39);
    }

    @ParameterizedTest
    @MethodSource("getMoveRepetitionArguments")
    void isThreefoldRepetitionFailure(boolean expectedIsFailure, String[] fens) {
        PieceConfiguration pieceConfiguration = loadConfigurationWithHistory(LongsPieceConfiguration.class, fens);
        assertThat(pieceConfiguration.isThreefoldRepetitionFailure()).isEqualTo(expectedIsFailure);
    }

    @ParameterizedTest
    @MethodSource("getDeadPositionArguments")
    void isDeadPosition(Class<? extends PieceConfiguration> configurationClass, String fen, boolean expectedResult) {
        PieceConfiguration pieceConfiguration = FENReader.read(fen, configurationClass);
        String negation = expectedResult ? "" : "not ";
        assertThat(pieceConfiguration.isDeadPosition())
            .as("Expected \"%s\" %sto be a dead position", fen, negation)
            .isEqualTo(expectedResult);
    }

    private static Stream<Arguments> getCastlePositionValues() {
        return Stream.of(
            Arguments.of(2, 0b00000000000000000000000000000010),
            Arguments.of(6, 0b00000000000000000000000000000100),
            Arguments.of(58, 0b00000000000000000000000000001000),
            Arguments.of(62, 0b00000000000000000000000000010000)
        );
    }

    private static Stream<Arguments> getEnPassantSquareValues() {
        return Stream.of(
                Arguments.of(16, 0b00100001111111111111111111111111),
                Arguments.of(17, 0b00100011111111111111111111111111),
                Arguments.of(18, 0b00100101111111111111111111111111),
                Arguments.of(19, 0b00100111111111111111111111111111),
                Arguments.of(20, 0b00101001111111111111111111111111),
                Arguments.of(21, 0b00101011111111111111111111111111),
                Arguments.of(22, 0b00101101111111111111111111111111),
                Arguments.of(23, 0b00101111111111111111111111111111),
                Arguments.of(40, 0b01010001111111111111111111111111),
                Arguments.of(41, 0b01010011111111111111111111111111),
                Arguments.of(42, 0b01010101111111111111111111111111),
                Arguments.of(43, 0b01010111111111111111111111111111),
                Arguments.of(44, 0b01011001111111111111111111111111),
                Arguments.of(45, 0b01011011111111111111111111111111),
                Arguments.of(46, 0b01011101111111111111111111111111),
                Arguments.of(47, 0b01011111111111111111111111111111)
        );
    }

    private static Stream<Arguments> getAlgebraicNotationFENs() {
        return Stream.of(
            Arguments.of("7k/8/8/8/8/P7/8/7K w - - 0 1", "7k/8/8/8/P7/8/8/7K b - - 0 1", "a3a4", IntsPieceConfiguration.class),
            Arguments.of("7k/8/8/8/8/Q7/8/7K w - - 0 1", "7k/8/8/8/Q7/8/8/7K b - - 1 1", "a3a4", IntsPieceConfiguration.class),
            Arguments.of("7k/8/8/8/p7/Q7/8/7K w - - 0 1", "7k/8/8/8/Q7/8/8/7K b - - 0 1", "a3xa4", IntsPieceConfiguration.class),
            Arguments.of("7k/P7/8/8/8/8/8/7K w - - 0 1", "Q6k/8/8/8/8/8/8/7K b - - 0 1", "a7a8q", IntsPieceConfiguration.class),
            Arguments.of("p6k/1P6/8/8/8/8/8/7K w - - 0 1", "Q6k/8/8/8/8/8/8/7K b - - 0 1", "b7xa8q", IntsPieceConfiguration.class),
            Arguments.of("7k/8/8/8/8/8/p7/7K w - - 0 1", "7k/8/8/8/8/8/8/q6K b - - 0 1", "a2a1q", IntsPieceConfiguration.class),
            Arguments.of("7k/8/8/8/8/8/1p6/P6K w - - 0 1", "7k/8/8/8/8/8/8/q6K b - - 0 1", "b2xa1q", IntsPieceConfiguration.class),
            Arguments.of("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1", "r3k2r/8/8/8/8/8/8/R4RK1 b kq - 1 1", "e1g1", IntsPieceConfiguration.class),
            Arguments.of("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1", "r3k2r/8/8/8/8/8/8/2KR3R b kq - 1 1", "e1c1", IntsPieceConfiguration.class),
            Arguments.of("7k/8/8/8/8/P7/8/7K w - - 0 1", "7k/8/8/8/P7/8/8/7K b - - 0 1", "a3a4", LongsPieceConfiguration.class),
            Arguments.of("7k/8/8/8/8/Q7/8/7K w - - 0 1", "7k/8/8/8/Q7/8/8/7K b - - 1 1", "a3a4", LongsPieceConfiguration.class),
            Arguments.of("7k/8/8/8/p7/Q7/8/7K w - - 0 1", "7k/8/8/8/Q7/8/8/7K b - - 0 1", "a3xa4", LongsPieceConfiguration.class),
            Arguments.of("7k/P7/8/8/8/8/8/7K w - - 0 1", "Q6k/8/8/8/8/8/8/7K b - - 0 1", "a7a8q", LongsPieceConfiguration.class),
            Arguments.of("p6k/1P6/8/8/8/8/8/7K w - - 0 1", "Q6k/8/8/8/8/8/8/7K b - - 0 1", "b7xa8q", LongsPieceConfiguration.class),
            Arguments.of("7k/8/8/8/8/8/p7/7K w - - 0 1", "7k/8/8/8/8/8/8/q6K b - - 0 1", "a2a1q", LongsPieceConfiguration.class),
            Arguments.of("7k/8/8/8/8/8/1p6/P6K w - - 0 1", "7k/8/8/8/8/8/8/q6K b - - 0 1", "b2xa1q", LongsPieceConfiguration.class),
            Arguments.of("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1", "r3k2r/8/8/8/8/8/8/R4RK1 b kq - 1 1", "e1g1", LongsPieceConfiguration.class),
            Arguments.of("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1", "r3k2r/8/8/8/8/8/8/2KR3R b kq - 1 1", "e1c1", LongsPieceConfiguration.class)
        );
    }

    private static Stream<Arguments> getMoveRepetitionArguments() {
        return Stream.of(
            Arguments.of(
                false,
                new String[]{
                    FENWriter.STARTING_POSITION,
                    "rnbqkbnr/pppppppp/8/8/8/5N2/PPPPPPPP/RNBQKB1R b KQkq - 1 1",
                    "r1bqkbnr/pppppppp/2n5/8/8/5N2/PPPPPPPP/RNBQKB1R w KQkq - 2 2",
                    "r1bqkbnr/pppppppp/2n5/8/8/8/PPPPPPPP/RNBQKBNR b KQkq - 3 2"
                }
            ),
            Arguments.of(
                false,
                new String[]{
                    FENWriter.STARTING_POSITION,
                    "rnbqkbnr/pppppppp/8/8/8/5N2/PPPPPPPP/RNBQKB1R b KQkq - 1 1",
                    "r1bqkbnr/pppppppp/2n5/8/8/5N2/PPPPPPPP/RNBQKB1R w KQkq - 2 2",
                    "r1bqkbnr/pppppppp/2n5/8/8/8/PPPPPPPP/RNBQKBNR b KQkq - 3 2",
                    "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 4 3"
                }
            ),
            Arguments.of(
                true,
                new String[]{
                    FENWriter.STARTING_POSITION,
                    "rnbqkbnr/pppppppp/8/8/8/5N2/PPPPPPPP/RNBQKB1R b KQkq - 1 1",
                    "r1bqkbnr/pppppppp/2n5/8/8/5N2/PPPPPPPP/RNBQKB1R w KQkq - 2 2",
                    "r1bqkbnr/pppppppp/2n5/8/8/8/PPPPPPPP/RNBQKBNR b KQkq - 3 2",
                    "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 4 3",
                    "rnbqkbnr/pppppppp/8/8/8/5N2/PPPPPPPP/RNBQKB1R b KQkq - 5 3",
                    "r1bqkbnr/pppppppp/2n5/8/8/5N2/PPPPPPPP/RNBQKB1R w KQkq - 6 4",
                    "r1bqkbnr/pppppppp/2n5/8/8/8/PPPPPPPP/RNBQKBNR b KQkq - 7 4",
                    "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 8 5"
                }
            ),
            Arguments.of(
                true,
                new String[]{
                    FENWriter.STARTING_POSITION,
                    "rnbqkbnr/pppppppp/8/8/8/5N2/PPPPPPPP/RNBQKB1R b KQkq - 1 1",
                    "r1bqkbnr/pppppppp/2n5/8/8/5N2/PPPPPPPP/RNBQKB1R w KQkq - 2 2",
                    "r1bqkbnr/pppppppp/2n5/8/8/8/PPPPPPPP/RNBQKBNR b KQkq - 3 2",
                    "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 4 3",
                    "rnbqkbnr/pppppppp/8/8/8/2N5/PPPPPPPP/R1BQKBNR b KQkq - 5 3",
                    "r1bqkbnr/pppppppp/2n5/8/8/2N5/PPPPPPPP/R1BQKBNR w KQkq - 6 4",
                    "r1bqkbnr/pppppppp/2n5/8/8/8/PPPPPPPP/RNBQKBNR b KQkq - 7 4",
                    "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 8 5"
                }
            ),
            Arguments.of(
                false,
                new String[]{
                    FENWriter.STARTING_POSITION,
                    "rnbqkbnr/pppppppp/8/8/8/5N2/PPPPPPPP/RNBQKB1R b KQkq - 1 1",
                    "r1bqkbnr/pppppppp/2n5/8/8/5N2/PPPPPPPP/RNBQKB1R w KQkq - 2 2",
                    "r1bqkbnr/pppppppp/2n5/8/8/8/PPPPPPPP/RNBQKBNR b KQkq - 3 2",
                    "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 4 3",
                    "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 3",
                    "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 4",
                    "rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 4",
                    "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 5",
                    "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 3 5",
                    "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 4 6"
                }
            )
        );
    }

    private static Stream<Arguments> getDeadPositionArguments() {
        return Stream.of(
            Arguments.of(IntsPieceConfiguration.class, "k7/8/8/8/8/8/8/7K w - - 10 50", true),
            Arguments.of(IntsPieceConfiguration.class, "k7/n7/8/8/8/8/8/7K w - - 10 50", true),
            Arguments.of(IntsPieceConfiguration.class, "k7/8/8/8/8/8/7N/7K w - - 10 50", true),
            Arguments.of(IntsPieceConfiguration.class, "k7/b7/8/8/8/8/8/7K w - - 10 50", true),
            Arguments.of(IntsPieceConfiguration.class, "k7/8/8/8/8/8/7B/7K w - - 10 50", true),
            Arguments.of(IntsPieceConfiguration.class, "k7/8/8/8/8/8/7P/7K w - - 10 50", false),
            Arguments.of(IntsPieceConfiguration.class, "k7/p7/8/8/8/8/8/7K w - - 10 50", false),
            Arguments.of(IntsPieceConfiguration.class, "k7/bb6/8/8/8/8/8/7K w - - 10 50", false),
            Arguments.of(IntsPieceConfiguration.class, "k7/8/8/8/8/8/6BB/7K w - - 10 50", false),
            Arguments.of(IntsPieceConfiguration.class, "k7/bn6/8/8/8/8/8/7K w - - 10 50", false),
            Arguments.of(IntsPieceConfiguration.class, "k7/8/8/8/8/8/6NB/7K w - - 10 50", false),
            Arguments.of(IntsPieceConfiguration.class, "k7/8/8/8/8/8/5PPP/7K w - - 10 50", false),
            Arguments.of(IntsPieceConfiguration.class, "k7/ppp5/8/8/8/8/8/7K w - - 10 50", false),
            Arguments.of(LongsPieceConfiguration.class, "k7/8/8/8/8/8/8/7K w - - 10 50", true),
            Arguments.of(LongsPieceConfiguration.class, "k7/n7/8/8/8/8/8/7K w - - 10 50", true),
            Arguments.of(LongsPieceConfiguration.class, "k7/8/8/8/8/8/7N/7K w - - 10 50", true),
            Arguments.of(LongsPieceConfiguration.class, "k7/b7/8/8/8/8/8/7K w - - 10 50", true),
            Arguments.of(LongsPieceConfiguration.class, "k7/8/8/8/8/8/7B/7K w - - 10 50", true),
            Arguments.of(LongsPieceConfiguration.class, "k7/8/8/8/8/8/7P/7K w - - 10 50", false),
            Arguments.of(LongsPieceConfiguration.class, "k7/p7/8/8/8/8/8/7K w - - 10 50", false),
            Arguments.of(LongsPieceConfiguration.class, "k7/bb6/8/8/8/8/8/7K w - - 10 50", false),
            Arguments.of(LongsPieceConfiguration.class, "k7/8/8/8/8/8/6BB/7K w - - 10 50", false),
            Arguments.of(LongsPieceConfiguration.class, "k7/bn6/8/8/8/8/8/7K w - - 10 50", false),
            Arguments.of(LongsPieceConfiguration.class, "k7/8/8/8/8/8/6NB/7K w - - 10 50", false),
            Arguments.of(LongsPieceConfiguration.class, "k7/8/8/8/8/8/5PPP/7K w - - 10 50", false),
            Arguments.of(LongsPieceConfiguration.class, "k7/ppp5/8/8/8/8/8/7K w - - 10 50", false)
        );
    }
}
