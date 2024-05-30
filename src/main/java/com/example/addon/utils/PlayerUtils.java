package com.example.addon.utils;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

public class PlayerUtils {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static boolean isSurrounded(PlayerEntity target) {
        if (target == null) return false;

        BlockPos north = target.getBlockPos().north();
        BlockPos east = target.getBlockPos().east();
        BlockPos south = target.getBlockPos().south();
        BlockPos west = target.getBlockPos().west();

        boolean isNorthBlocked = isObsidianOrBedrock(north);
        boolean isEastBlocked = isObsidianOrBedrock(east);
        boolean isSouthBlocked = isObsidianOrBedrock(south);
        boolean isWestBlocked = isObsidianOrBedrock(west);

        return isNorthBlocked && isEastBlocked && isSouthBlocked && isWestBlocked;
    }

    private static boolean isObsidianOrBedrock(BlockPos pos) {
        Block block = mc.world.getBlockState(pos).getBlock();
        return block == Blocks.OBSIDIAN || block == Blocks.BEDROCK;
    }
}
