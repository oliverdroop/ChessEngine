package chess.api.ai;

import chess.api.PieceConfiguration;

public record ConfigurationScorePair(PieceConfiguration pieceConfiguration, double score) {
}
