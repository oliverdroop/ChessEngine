package com.oliverdroop.chess.api.ai.openings;

import com.oliverdroop.chess.api.FENWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Opening {

    protected final List<String> fens = new ArrayList<>();
    protected int maxFullMoveNumber;

    protected Opening(String... fens) {
        this.fens.add(FENWriter.STARTING_POSITION);
        setFensAndMaxFullMoveNumber(fens);
    }

    protected int getMaxFullMoveNumber() {
        return maxFullMoveNumber;
    }

    protected void setFensAndMaxFullMoveNumber(String... fens) {
        this.fens.addAll(Arrays.stream(fens).toList());
        final String finalFen = fens[fens.length - 1];
        this.maxFullMoveNumber = Integer.parseInt(finalFen.substring(finalFen.length() - 1));
    }
}
