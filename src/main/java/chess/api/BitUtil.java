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

    public static int clearBits(int number, int mask) {
        return number & ~mask;
    }

    public static int overwriteBits(int number, int mask, int bitsToWrite) {
        return clearBits(number, mask) | bitsToWrite;
    }
}
