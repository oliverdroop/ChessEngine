package chess.api.storage.ephemeral;

import java.math.BigInteger;
import java.util.Comparator;

public class BigIntegerComparator implements Comparator<BigInteger> {

    @Override
    public int compare(BigInteger t1, BigInteger t2) {
        final int shortCount1 = (int)Math.ceil(t1.bitLength() / (double) 16);
        final int shortCount2 = (int)Math.ceil(t2.bitLength() / (double) 16);
        if (shortCount1 == shortCount2) {
            return t1.compareTo(t2);
        }
        if (shortCount1 > shortCount2) {
            return t1.compareTo(t2.shiftLeft((shortCount1 - shortCount2) * 16));
        }
        return t1.shiftLeft((shortCount2 - shortCount1) * 16).compareTo(t2);
    }
}
