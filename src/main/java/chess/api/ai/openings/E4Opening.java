package chess.api.ai.openings;

public class E4Opening extends Opening {

    E4Opening(String... fens) {
        super("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");
        setFensAndMaxFullMoveNumber(fens);
    }
}
