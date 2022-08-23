package chess.api;

public class BitUtil {
    public static int applyBitFlag(int number, int bitFlag) {
        return number | bitFlag;
    }

    public static boolean hasBitFlag(int number, int bitFlag) {
        return (number & bitFlag) == bitFlag;
    }
}
