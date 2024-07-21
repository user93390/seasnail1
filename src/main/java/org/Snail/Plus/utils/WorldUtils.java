package org.Snail.Plus.utils;

import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class WorldUtils {
    public static boolean isAir(BlockPos position) {
    return Objects.requireNonNull(mc.world).isAir(position) || mc.world.getBlockState(position).getBlock() == Blocks.FIRE;
    }
}
