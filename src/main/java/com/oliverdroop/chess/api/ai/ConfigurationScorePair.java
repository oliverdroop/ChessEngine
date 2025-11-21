package com.oliverdroop.chess.api.ai;

import com.oliverdroop.chess.api.configuration.PieceConfiguration;

public record ConfigurationScorePair(PieceConfiguration pieceConfiguration, double score) implements Comparable<ConfigurationScorePair> {

    @Override
    public int compareTo(ConfigurationScorePair configurationScorePair) {
        return Double.compare(configurationScorePair.score, this.score); // highest scores first when used in a sort
    }
}
