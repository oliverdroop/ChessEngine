package chess.api.storage.ephemeral;

import java.util.Arrays;
import java.util.Comparator;

public class SawtoothShortArrayComparator implements Comparator<short[]> {

    @Override
    public int compare(short[] t1, short[] t2) {
        return Arrays.compare(t1, t2);
    }
}
