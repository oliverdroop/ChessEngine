package com.oliverdroop.chess.api.storage.ephemeral;

import java.util.Comparator;

public class SawtoothShortArrayComparator implements Comparator<short[]> {

    @Override
    public int compare(short[] t1, short[] t2) {
        if (t1.length == t2.length) {
            return deepCompare(t1, t2, t1.length, 0);
        }
        final int shorterLength = Math.min(t1.length, t2.length);
        return deepCompare(t1, t2, shorterLength, t1.length - t2.length);
    }

    private int deepCompare(short[] t1, short[] t2, int depth, int fallback) {
        for(int index = 0; index < depth; index++) {
            final short val1 = t1[index];
            final short val2 = t2[index];
            if (val1 == val2) {
                continue;
            }
            return val1 - val2;
        }
        return fallback;
    }
}
