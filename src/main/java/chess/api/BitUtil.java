package chess.api;

import java.util.Collection;

public class BitUtil {
    public static int applyBitFlag(int number, int bitFlag) {
        return number | bitFlag;
    }

    public static boolean hasBitFlag(int number, int bitFlag) {
        return (number & bitFlag) == bitFlag;
    }

    public static int reverseBitFlag(int number, int bitFlag) {
        return number ^ bitFlag;
    }
}
