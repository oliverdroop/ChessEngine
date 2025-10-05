package chess.api.storage.ephemeral;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class MoveHistoryConverter {

    public static BigInteger fromMoves(short[] moves) {
        if (moves.length == 0) {
            return BigInteger.ZERO;
        }
        final byte[] bytes = new byte[moves.length * 2];
        for(int i = 0; i < moves.length; i++) {
            final short move = moves[i];
            bytes[i * 2] = (byte) (move >>> 8);
            bytes[(i * 2) + 1] = (byte) (move);
        }
        return new BigInteger(bytes);
    }

    public static short[] toMoves(BigInteger value) {
        if (value.equals(BigInteger.ZERO)) {
            return new short[0];
        }
        final byte[] rawBytes = value.toByteArray();
        byte[] bytes = new byte[(int) Math.ceil(rawBytes.length / (double) 2) * 2];
        System.arraycopy(rawBytes, 0, bytes, rawBytes.length % 2, rawBytes.length);
        final short[] moves = new short[bytes.length / 2];
        for(int i = 0; i < moves.length; i++) {
            final int bytesIndex = i * 2;
            moves[i] = ByteBuffer.wrap(Arrays.copyOfRange(bytes, bytesIndex, bytesIndex + 2)).getShort();
        }
        return moves;
    }

    public static int getLengthInShorts(BigInteger value) {
        return (int) Math.ceil(value.bitLength() / (double) 16);
    }
}
