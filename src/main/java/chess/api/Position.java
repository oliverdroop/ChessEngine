package chess.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Position {

    private static final Logger LOGGER = LoggerFactory.getLogger(Position.class);

    public static final int[] POSITIONS = {
            0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,
            16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,
            32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,
            48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63};

    public static final int[][] DIRECTIONAL_BIT_FLAG_GRID = {
            {PieceConfiguration.DIRECTION_SW, PieceConfiguration.DIRECTION_S, PieceConfiguration.DIRECTION_SE},
            {PieceConfiguration.DIRECTION_W, -1, PieceConfiguration.DIRECTION_E},
            {PieceConfiguration.DIRECTION_NW, PieceConfiguration.DIRECTION_N, PieceConfiguration.DIRECTION_NE}
    };

    public static int[] getCoordinates(int position) {
        return new int[] {getX(position), getY(position)};
    }

    public static int getX(int position) {
        return position & 7;
    }

    public static int getY(int position) {
        return (position >> 3) & 7;
    }

    public static int getPosition(int x, int y) {
        return (y << 3) | x;
    }

    public static int getPosition(int positionBitFlag) {
        return positionBitFlag & 63;
    }

    public static String getCoordinateString(int position) {
        int[] coordinates = getCoordinates(position);
        return new StringBuilder()
                .append((char) (coordinates[0] + 97))
                .append((char) (coordinates[1] + 49))
                .toString();
    }

    public static int getPosition(String coordinateString) {
        if (!coordinateString.matches("^[a-h][1-8]$")) {
            throw new RuntimeException(String.format("Unable to parse %s as coordinate string", coordinateString));
        }
        return getPosition(coordinateString.charAt(0) - 97, coordinateString.charAt(1) - 49);
    }

    public static boolean isValidPosition(int testPosition) {
        return (testPosition & 63) == testPosition;
    }

    public static int applyTranslation(int position, int translationX, int translationY) {
        int newX = getX(position) + translationX;
        int newY = getY(position) + translationY;
        if (newX < 0 || newX > 7 || newY < 0 || newY > 7) {
            return -1;
        }
        return getPosition(newX, newY);
    }

    public static int applyTranslationTowardsThreat(int directionBitFlag, int positionBitFlag) {

        // Move in the opposite direction to the threat direction
        return switch (directionBitFlag) {
            case PieceConfiguration.DIRECTION_N -> Position.applyTranslation(positionBitFlag, 0, -1);
            case PieceConfiguration.DIRECTION_NE -> Position.applyTranslation(positionBitFlag, -1, -1);
            case PieceConfiguration.DIRECTION_E -> Position.applyTranslation(positionBitFlag, -1, 0);
            case PieceConfiguration.DIRECTION_SE -> Position.applyTranslation(positionBitFlag, -1, 1);
            case PieceConfiguration.DIRECTION_S -> Position.applyTranslation(positionBitFlag, 0, 1);
            case PieceConfiguration.DIRECTION_SW -> Position.applyTranslation(positionBitFlag, 1, 1);
            case PieceConfiguration.DIRECTION_W -> Position.applyTranslation(positionBitFlag, 1, 0);
            case PieceConfiguration.DIRECTION_NW -> Position.applyTranslation(positionBitFlag, 1, -1);
            default -> -1;
        };
    }

    public static int getPositionFromCoordinateString(String coordinateString) {
        return getPosition((int) coordinateString.charAt(0) - 97, Integer.parseInt(coordinateString.charAt(1) + "") - 1);
    }
}
