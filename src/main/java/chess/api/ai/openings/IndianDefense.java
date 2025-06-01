package chess.api.ai.openings;

import java.util.Arrays;

public class IndianDefense extends D4Opening {

    IndianDefense(String... fens) {
        super("rnbqkb1r/pppppppp/5n2/8/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 1 2");
        this.fens.addAll(Arrays.stream(fens).toList());
    }
}
