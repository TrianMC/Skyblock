package io.github.trianmc.skyblock.util.math;

import java.text.DecimalFormat;

import static java.lang.Math.ceil;
import static java.lang.Math.sqrt;

public class MathUtils {
    // https://math.stackexchange.com/a/163101
    public static int[] spiralAt(int n) {
        n++; // one-index
        int k = (int) ceil((sqrt(n) - 1) / 2);
        int t = 2 * k + 1;
        int m = t * t;
        t--;

        if (n > m - t) {
            return new int[]{k - (m - n), -k};
        } else {
            m -= t;
        }

        if (n > m - t) {
            return new int[]{-k, -k + (m - n)};
        } else {
            m -= t;
        }

        if (n > m - t) {
            return new int[]{-k + (m - n), k};
        } else {
            return new int[]{k, k - (m - n - t)};
        }
    }

    // https://stackoverflow.com/questions/4753251/how-to-go-about-formatting-1200-to-1-2k-in-java
    public static String compact(double value) {
        if (value <= 999) {
            return String.valueOf(value);
        }

        final String[] units = new String[]{"", "K", "M", "B", "T", "Q", "QT"};
        int digitGroups = (int) (Math.log10(value) / Math.log10(1000));
        return new DecimalFormat("#,##0.#").format(value / Math.pow(1000, digitGroups)) + "" + units[digitGroups];
    }
}
