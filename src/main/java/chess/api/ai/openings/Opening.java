package chess.api.ai.openings;

import chess.api.FENWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Opening {

    protected final List<String> fens = new ArrayList<>();

    Opening(String... fens) {
        this.fens.add(FENWriter.STARTING_POSITION);
        this.fens.addAll(Arrays.stream(fens).toList());
    }
}
