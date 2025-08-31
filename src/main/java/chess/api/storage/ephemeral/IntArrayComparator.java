package chess.api.storage.ephemeral;

import java.util.Comparator;

public class IntArrayComparator implements Comparator<int[]> {

    @Override
    public int compare(int[] t1, int[] t2) {
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
            int val1 = t1[index];
            int val2 = t2[index];
            if (val1 < val2) {
                return -2;
            } else if (val1 > val2) {
                return 2;
            }
        }
        if (t1.length < t2.length) {
            return -1;
        }
        return 0;
    }
}
