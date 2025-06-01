package chess.api.ai.openings;

import java.util.Arrays;

public class E4E5Opening extends E4Opening {

    E4E5Opening(String... fens) {
        super("rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2");
        this.fens.addAll(Arrays.stream(fens).toList());
    }
}
