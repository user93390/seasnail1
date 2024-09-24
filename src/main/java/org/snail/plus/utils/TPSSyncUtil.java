package org.snail.plus.utils;

import net.minecraft.client.MinecraftClient;

import java.util.Objects;
import java.util.function.Supplier;

public class TPSSyncUtil {
    private static MinecraftClient mc = MinecraftClient.getInstance();
    public static Float getTPS() {
        return Objects.requireNonNull(mc.getServer()).getAverageTickTime();
    }
}
