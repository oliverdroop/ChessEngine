package chess.api.ai.openings;

public class ZukertortOpening extends Opening {

    ZukertortOpening(String... fens) {
        super("rnbqkbnr/pppppppp/8/8/8/5N2/PPPPPPPP/RNBQKB1R b KQkq - 1 1");
        setFensAndMaxFullMoveNumber(fens);
    }
}
