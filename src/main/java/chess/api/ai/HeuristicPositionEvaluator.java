package chess.api.ai;

import chess.api.PieceConfiguration;

import java.util.List;
import java.util.Random;

public class HeuristicPositionEvaluator implements IPositionEvaluator{

    private static final Random RANDOM = new Random();

    private static final int BIT_FLAG_COUNT = 8;

    private static final int AUXILIARY_DATA_PART_COUNT = 8;

    private static final int INPUT_COUNT = (64 * BIT_FLAG_COUNT) + AUXILIARY_DATA_PART_COUNT;

    private static final int WEIGHTING_COUNT = INPUT_COUNT * INPUT_COUNT;

    private final double[] weightings = new double[WEIGHTING_COUNT];

    public HeuristicPositionEvaluator() {
        generateRandomWeightings();
    }

    public HeuristicPositionEvaluator(HeuristicPositionEvaluator parent1, HeuristicPositionEvaluator parent2) {
        breedParents(parent1.weightings, parent2.weightings);
    }

    @Override
    public PieceConfiguration getBestMove(PieceConfiguration inputConfiguration) {
        final List<PieceConfiguration> onwardConfigurations = inputConfiguration.getPossiblePieceConfigurations();
        PieceConfiguration bestConfiguration = null;
        double bestScore = -Double.MAX_VALUE;
        for(PieceConfiguration onwardConfiguration : onwardConfigurations) {
            // Calculate the onward-onward configurations just to set the bit flags
//            onwardConfiguration.getPossiblePieceConfigurations();
            // Get all the inputs
            final int[] inputs = getInputs(onwardConfiguration);
            final double score = calculateScore(inputs);
            if (score > bestScore) {
                bestScore = score;
                bestConfiguration = onwardConfiguration;
            }
        }
        return bestConfiguration;
    }

    private int[] getInputs(PieceConfiguration onwardConfiguration) {
        final int[] inputs = new int[INPUT_COUNT];
        // Get the position bit flag inputs
        final int[] positionBitFlags = onwardConfiguration.getPositionBitFlags();
        for(int position = 0; position < 64; position++) {
            final int positionBitFlag = positionBitFlags[position];
            for(int bitFlagIndex = 0; bitFlagIndex < BIT_FLAG_COUNT; bitFlagIndex++) {
                final int inputIndex = (position * BIT_FLAG_COUNT) + bitFlagIndex;
                inputs[inputIndex] = positionBitFlag >> (8 + bitFlagIndex);
            }
        }
        // Get the auxiliary data inputs
        // Turn side
        final int auxInputStartIndex = 64 * BIT_FLAG_COUNT;
        inputs[auxInputStartIndex] = onwardConfiguration.getTurnSide();
        // Available castle positions
        for(int castlePositionIndex = 1; castlePositionIndex < 5; castlePositionIndex++) {
            inputs[auxInputStartIndex + castlePositionIndex] = (onwardConfiguration.getAuxiliaryData() >> castlePositionIndex) & 1;
        }
        // Half move clock
        inputs[auxInputStartIndex + 5] = onwardConfiguration.getHalfMoveClock();
        // Full move number
        inputs[auxInputStartIndex + 6] = onwardConfiguration.getFullMoveNumber();
        // En passant square
        inputs[auxInputStartIndex + 7] = onwardConfiguration.getEnPassantSquare();
        return inputs;
    }

    private double calculateScore(int[] inputs) {
        double score = 0;
        for(int inputIndex = 0; inputIndex < inputs.length; inputIndex++) {
            int input = inputs[inputIndex];
            for(int inputIndex2 = 0; inputIndex2 < inputs.length; inputIndex2++) {
                if (inputIndex != inputIndex2) {
                    final int weightingIndex = (inputIndex * INPUT_COUNT) + inputIndex2;
                    score += input * weightings[weightingIndex] * inputs[inputIndex2];
                }
            }
        }
        return score;
    }

    private void generateRandomWeightings() {
        fillWithRandomDoubles(weightings);
    }

    private void fillWithRandomDoubles(double[] toFill) {
        for(int index = 0; index < toFill.length; index++) {
            toFill[index] = RANDOM.nextDouble(-1.0, 1.0);
        }
    }

    private void breedParents(double[] weightings1, double[] weightings2) {
        for(int weightingIndex = 0; weightingIndex < WEIGHTING_COUNT; weightingIndex++) {
            weightings[weightingIndex] = RANDOM.nextBoolean() ? weightings1[weightingIndex] : weightings2[weightingIndex];
        }
    }
}
