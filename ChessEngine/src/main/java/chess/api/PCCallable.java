package chess.api;

import chess.api.PieceConfiguration;

import java.util.List;
import java.util.concurrent.Callable;

public class PCCallable implements Callable<List<PieceConfiguration>> {

    private final PieceConfiguration pieceConfiguration;

    public PCCallable(PieceConfiguration pieceConfiguration) {
        this.pieceConfiguration = pieceConfiguration;
    }

    @Override
    public List<PieceConfiguration> call() {
        return pieceConfiguration.getPossiblePieceConfigurations();
    }
}
