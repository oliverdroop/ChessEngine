package chess.api.ai.openings;

import java.util.Arrays;

public class QueensGambit extends D4D5Opening {

    public QueensGambit(String... fens) {
        super("rnbqkbnr/ppp1pppp/8/3p4/2PP4/8/PP2PPPP/RNBQKBNR b KQkq c3 0 2");
        this.fens.addAll(Arrays.stream(fens).toList());
    }
}
