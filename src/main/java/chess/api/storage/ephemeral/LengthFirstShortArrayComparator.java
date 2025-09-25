package chess.api.storage.ephemeral;

import java.util.Comparator;

public class LengthFirstShortArrayComparator implements Comparator<short[]> {

    @Override
    public int compare(short[] t1, short[] t2) {
        if (t1.length == t2.length) {
            for(int index = 0; index < t1.length; index++) {
                final short val1 = t1[index];
                final short val2 = t2[index];
                if (val1 == val2) {
                    continue;
                }
                return val1 - val2;
            }
            return 0;
        }
        return t1.length - t2.length;
    }
}
