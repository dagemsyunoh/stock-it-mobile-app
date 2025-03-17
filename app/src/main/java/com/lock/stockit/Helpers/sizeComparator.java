package com.lock.stockit.Helpers;


import java.util.Comparator;

public class sizeComparator implements Comparator<String> {
    @Override
    public int compare(String o1, String o2) {
        String[] d1 = o1.split("x");
        String[] d2 = o2.split("x");
        for (int i = 0; i < 3; i++ ) {
            int a = Integer.parseInt(d1[i]);
            int b = Integer.parseInt(d2[i]);
            int compareSize = Integer.compare(a, b);
            if (compareSize != 0) return compareSize;
        }
        return 0;
    }
}
