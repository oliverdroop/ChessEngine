package chess.api.ai.openings;

import java.util.Arrays;

public class E4Opening extends Opening {

    E4Opening(String... fens) {
        super("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");
        this.fens.addAll(Arrays.stream(fens).toList());
    }
}
