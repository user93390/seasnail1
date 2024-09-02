package org.snail.plus.utils;

import java.util.function.Supplier;

public class TPSSyncUtil {
    private static Supplier<Double> tpsSupplier;
    public static double getCurrentTPS() {
        return tpsSupplier != null ? tpsSupplier.get() : 20.0;
    }
}
