package chess.api;

import org.apache.maven.surefire.shared.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class WeightingConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(WeightingConfig.class);

    private static final double DEFAULT_THREATENED_SQUARE_WEIGHTING = 0.00625;

    private static final double[] threatenedSquareWeightings = new double[2];

    private static final int THREATENED_SQUARE_MAX = 64;

    private static final double DEFAULT_THREATENED_PIECE_WEIGHTING = 0.00625;

    private static final double[] threatenedPieceWeightings = new double[2];

    private static final int THREATENED_PIECE_MAX = 16;

    private static final double DEFAULT_OCCUPIED_CENTRE_WEIGHTING = 0.125;

    private static final double[] occupiedCentreWeightings = new double[2];

    private static final int OCCUPIED_CENTRE_MAX = 4;

    static {
        generateRandomWeightings();
    }

    public static double getThreatenedSquareWeighting(int turnSide) {
        return threatenedSquareWeightings[turnSide];
    }

    public static double getThreatenedPieceWeighting(int turnSide) {
        return threatenedPieceWeightings[turnSide];
    }

    public static double getOccupiedCentreWeighting(int turnSide) {
        return occupiedCentreWeightings[turnSide];
    }

    public static double calculateMaxWeighting(int turnSide) {
        return (getThreatenedSquareWeighting(turnSide) * THREATENED_SQUARE_MAX)
            + (getThreatenedPieceWeighting(turnSide) * THREATENED_PIECE_MAX)
            + (getOccupiedCentreWeighting(turnSide) * OCCUPIED_CENTRE_MAX);
    }

    public static void generateRandomWeightings(int turnSide) {
        final Random rnd = new Random();
        double remaining = 1;
        final double[] weightings = new double[3];
        for(int i = 0; i < 2; i++) {
            double weighting = rnd.nextDouble(remaining);
            remaining -= weighting;
            weightings[i] = weighting;
        }
        weightings[2] = remaining;

        final int randomIndex = rnd.nextInt(3);
        threatenedSquareWeightings[turnSide] = weightings[randomIndex] / (double) THREATENED_SQUARE_MAX;
        threatenedPieceWeightings[turnSide] = weightings[(randomIndex + 1) % 3] / (double) THREATENED_PIECE_MAX;
        occupiedCentreWeightings[turnSide] = weightings[(randomIndex + 2) % 3] / (double) OCCUPIED_CENTRE_MAX;

        logWeightings(turnSide);
    }

    public static void breedWeightings() {
        LOGGER.info("Preserving an average of the weightings");
        double rawWhiteThreatenedSquareWeighting = getThreatenedSquareWeighting(0) * THREATENED_SQUARE_MAX;
        double rawWhiteThreatenedPieceWeighting = getThreatenedPieceWeighting(0) * THREATENED_PIECE_MAX;
        double rawWhiteOccupiedCentreWeighting = getOccupiedCentreWeighting(0) * OCCUPIED_CENTRE_MAX;
        double rawBlackThreatenedSquareWeighting = getThreatenedSquareWeighting(1) * THREATENED_SQUARE_MAX;
        double rawBlackThreatenedPieceWeighting = getThreatenedPieceWeighting(1) * THREATENED_PIECE_MAX;
        double rawBlackOccupiedCentreWeighting = getOccupiedCentreWeighting(1) * OCCUPIED_CENTRE_MAX;

        double averageRawThreatenedSquareWeighting = (rawWhiteThreatenedSquareWeighting + rawBlackThreatenedSquareWeighting) / 2;
        double averageRawThreatenedPieceWeighting = (rawWhiteThreatenedPieceWeighting + rawBlackThreatenedPieceWeighting) / 2;
        double averageRawOccupiedCentreWeighting = (rawWhiteOccupiedCentreWeighting + rawBlackOccupiedCentreWeighting) / 2;

        threatenedSquareWeightings[0] = averageRawThreatenedSquareWeighting / (double) THREATENED_SQUARE_MAX;
        threatenedPieceWeightings[0] = averageRawThreatenedPieceWeighting / (double) THREATENED_PIECE_MAX;
        occupiedCentreWeightings[0] = averageRawOccupiedCentreWeighting / (double) OCCUPIED_CENTRE_MAX;
        logWeightings(0);
        generateRandomWeightings(1);
    }

    public static void logWeightings(int turnSide) {
        final Side side = Side.values()[turnSide];
        LOGGER.info("{} Threatened square weighting is {}", side, threatenedSquareWeightings[turnSide]);
        LOGGER.info("{} Threatened piece weighting is {}", side, threatenedPieceWeightings[turnSide]);
        LOGGER.info("{} Occupied centre weighting is {}", side, occupiedCentreWeightings[turnSide]);
    }

    public static void generateRandomWeightings() {
        LOGGER.info("Generating new random weighting values for both sides");
        generateRandomWeightings(0);
        generateRandomWeightings(1);
    }

    public static void swapWeightings() {
        ArrayUtils.reverse(threatenedSquareWeightings);
        ArrayUtils.reverse(threatenedPieceWeightings);
        ArrayUtils.reverse(occupiedCentreWeightings);
    }
}
