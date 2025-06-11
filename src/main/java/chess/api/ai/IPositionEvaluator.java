package chess.api.ai;

import chess.api.PieceConfiguration;

public interface IPositionEvaluator {
    PieceConfiguration getBestMove(PieceConfiguration inputConfiguration);
}
