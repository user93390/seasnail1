package org.Snail.Plus.utils;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class CombatUtils {
        private static boolean block;
        public static boolean isSurrounded (PlayerEntity target){
        BlockPos north = target.getBlockPos().north();
        BlockPos east = target.getBlockPos().east();
        BlockPos south = target.getBlockPos().south();
        BlockPos west = target.getBlockPos().west();
        boolean isNorthBlocked = IsValidBlock(north);
        boolean isEastBlocked = IsValidBlock(east);
        boolean isSouthBlocked = IsValidBlock(south);
        boolean isWestBlocked = IsValidBlock(west);

        return isNorthBlocked && isEastBlocked && isSouthBlocked && isWestBlocked;
    }
        private static boolean IsValidBlock (BlockPos pos){
        Block block = Objects.requireNonNull(mc.world).getBlockState(pos).getBlock();
        return block == Blocks.OBSIDIAN || block == Blocks.BEDROCK || block == Blocks.REINFORCED_DEEPSLATE || block == Blocks.NETHERITE_BLOCK;
    }
        public static boolean isCentered (PlayerEntity target) {
        BlockPos blockPos = target.getBlockPos();
        double centerX = blockPos.getX() + 0.5;
        double centerZ = blockPos.getZ() + 0.5;
        double playerX = target.getX();
        double playerZ = target.getZ();
        double distanceX = Math.abs(playerX - centerX);
        double distanceZ = Math.abs(playerZ - centerZ);
        double threshold = 0.2;
        return distanceX < threshold && distanceZ < threshold;
    }
        public static boolean isBurrowed (PlayerEntity target){
        if (target == null) return false;
        BlockPos blockPos = target.getBlockPos();
        Block block = Objects.requireNonNull(mc.world).getBlockState(blockPos).getBlock();
        return block == Blocks.OBSIDIAN || block == Blocks.BEDROCK || block == Blocks.REINFORCED_DEEPSLATE || block == Blocks.NETHERITE_BLOCK || block == Blocks.COBWEB;
    }
}