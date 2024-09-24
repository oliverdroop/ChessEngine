package chess.api;

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

    public static int clearBits(int number, int bitsToClear) {
        return number & ~bitsToClear;
    }

    public static int overwriteBits(int number, int bitsToClear, int bitsToWrite) {
        return clearBits(number, bitsToClear) | bitsToWrite;
    }
}
