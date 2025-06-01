package chess.api.ai.openings;

import java.util.Arrays;

public class D4D5Opening extends D4Opening {

    D4D5Opening(String... fens) {
        super("rnbqkbnr/ppp1pppp/8/3p4/3P4/8/PPP1PPPP/RNBQKBNR w KQkq d6 0 2");
        this.fens.addAll(Arrays.stream(fens).toList());
    }
}
