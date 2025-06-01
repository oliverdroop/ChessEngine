package chess.api.ai.openings;

import java.util.Arrays;

public class D4Opening extends Opening {

    D4Opening(String... fens) {
        super("rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq d3 0 1");
        this.fens.addAll(Arrays.stream(fens).toList());
    }
}
