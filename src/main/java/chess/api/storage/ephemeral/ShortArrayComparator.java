package chess.api.storage.ephemeral;

import java.util.Comparator;

public class ShortArrayComparator implements Comparator<short[]> {

    @Override
    public int compare(short[] t1, short[] t2) {
        if (t1.length == 0 && t2.length == 0) {
            return 0;
        } else if (t1.length == 0) {
            return -1;
        } else if (t2.length == 0) {
            return 1;
        }
        for(int index = 0; index < t1.length; index++) {
            if (index >= t2.length) {
                return 1;
            }
            short val1 = t1[index];
            short val2 = t2[index];
            if (val1 < val2) {
                return -1;
            } else if (val1 > val2) {
                return 1;
            }
        }
        if (t1.length < t2.length) {
            return -1;
        }
        return 0;
    }
}
