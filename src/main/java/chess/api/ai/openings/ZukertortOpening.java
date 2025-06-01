package chess.api.ai.openings;

import java.util.Arrays;

public class ZukertortOpening extends Opening {

    ZukertortOpening(String... fens) {
        super("rnbqkbnr/pppppppp/8/8/8/5N2/PPPPPPPP/RNBQKB1R b KQkq - 1 1");
        this.fens.addAll(Arrays.stream(fens).toList());
    }
}
