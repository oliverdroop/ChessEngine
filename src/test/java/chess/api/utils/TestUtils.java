package chess.api.utils;

import chess.api.FENReader;
import chess.api.MoveDescriber;
import chess.api.configuration.PieceConfiguration;

public class TestUtils {

    public static PieceConfiguration loadConfigurationWithHistory(
        Class<? extends PieceConfiguration> configurationClass,
        String... fens
    ) {
        final int fenCount = fens.length;
        final short[] moves = new short[fenCount - 1];
        PieceConfiguration historicalConfiguration = FENReader.read(fens[0], configurationClass);
        for(int fenIndex = 1; fenIndex < fenCount; fenIndex++) {
            final String fen = fens[fenIndex];
            final PieceConfiguration nextConfiguration = FENReader.read(fen, configurationClass);
            final String algebraicNotation = nextConfiguration.getAlgebraicNotation(historicalConfiguration);
            historicalConfiguration = nextConfiguration;
            moves[fenIndex - 1] = MoveDescriber.getMoveFromAlgebraicNotation(algebraicNotation);
        }
        historicalConfiguration.setHistoricMoves(moves);
        return historicalConfiguration;
    }
}
