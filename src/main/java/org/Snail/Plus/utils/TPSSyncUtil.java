package org.snail.plus.utils;

import java.util.function.Supplier;

public class TPSSyncUtil {
    private static Supplier<Double> tpsSupplier;
    private static boolean syncEnabled;

    public static double getCurrentTPS() {
        return tpsSupplier != null ? tpsSupplier.get() : 20.0;
    }

    public static void setSyncEnabled(boolean enabled) {
        syncEnabled = enabled;
    }

}
