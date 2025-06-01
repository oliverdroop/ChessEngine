package chess.api.ai.openings;

import java.util.Arrays;

public class KingsIndianDefense extends IndianDefense {

    KingsIndianDefense(String... fens) {
        super(
            "rnbqkb1r/pppppppp/5n2/8/2PP4/8/PP2PPPP/RNBQKBNR b KQkq c3 0 2",
            "rnbqkb1r/pppppp1p/5np1/8/2PP4/8/PP2PPPP/RNBQKBNR w KQkq - 0 3"
        );
        this.fens.addAll(Arrays.stream(fens).toList());
    }
}
