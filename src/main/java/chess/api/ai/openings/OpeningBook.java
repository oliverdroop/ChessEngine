package chess.api.ai.openings;

import chess.api.FENReader;
import chess.api.MoveDescriber;
import chess.api.PieceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static chess.api.MoveDescriber.getMoveFromAlgebraicNotation;

public class OpeningBook {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpeningBook.class);
    private static final Random RANDOM = new Random();
    private static final List<Opening> openings = new ArrayList<>();

    static {
        openings.add(new AlekhinesDefense());
        openings.add(new BenkoGambit());
        openings.add(new BogoIndianDefense());
        openings.add(new CatalanOpening());
        openings.add(new DutchDefense());
        openings.add(new FrenchDefense());
        openings.add(new GrunfeldDefense());
        openings.add(new ItalianGame());
        openings.add(new KaroKannDefense());
        openings.add(new KingsGambit());
        openings.add(new KingsIndianAttack());
        openings.add(new KingsIndianDefense());
        openings.add(new LondonSystem());
        openings.add(new ModernBenoniDefense());
        openings.add(new NimzoIndianDefense());
        openings.add(new PircDefense());
        openings.add(new QueensGambitAccepted());
        openings.add(new QueensGambitDeclined());
        openings.add(new QueensIndianDefense());
        openings.add(new RetiOpening());
        openings.add(new RuyLopezOpening());
        openings.add(new ScandinavianDefense());
        openings.add(new ScotchGame());
        openings.add(new SicilianDefense());
        openings.add(new SlavDefense());
        openings.add(new TrompowskiAttack());
        openings.add(new ViennaGame());
    }

    public static PieceConfiguration getOpeningResponse(PieceConfiguration inputConfiguration) {
        if (inputConfiguration.getFullMoveNumber() < 4) {
            final String inputFEN = inputConfiguration.toString();
            final List<Opening> possibleOpenings = getPossibleOpenings(inputFEN);

            if (!possibleOpenings.isEmpty()) {
                LOGGER.debug("The following openings are valid for this FEN: {}", possibleOpenings.stream()
                    .map(opening -> opening.getClass().getSimpleName())
                    .toList());
                final Opening chosenOpening = possibleOpenings.get(RANDOM.nextInt(possibleOpenings.size()));
                LOGGER.info("Choosing {}", chosenOpening.getClass().getSimpleName());
                final int fenIndex = chosenOpening.fens.indexOf(inputFEN);
                final PieceConfiguration outputConfiguration = FENReader.read(chosenOpening.fens.get(fenIndex + 1));
                outputConfiguration.addHistoricMove(
                    inputConfiguration,
                    getMoveFromAlgebraicNotation(outputConfiguration.getAlgebraicNotation(inputConfiguration))
                );
                return outputConfiguration;
            }
            LOGGER.info("No openings available for this configuration");
        }
        return null;
    }

    static List<Opening> getOpenings() {
        return openings;
    }

    private static List<Opening> getPossibleOpenings(String inputFEN) {
        return openings.stream()
            .filter(opening -> {
                final int fenIndex = opening.fens.indexOf(inputFEN);
                return fenIndex >= 0 && fenIndex < opening.fens.size() - 1;
            })
            .toList();
    }
}
