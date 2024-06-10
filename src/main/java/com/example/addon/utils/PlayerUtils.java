package com.example.addon.utils;

import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class PlayerUtils {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static boolean isSurrounded(PlayerEntity target) {
        if (target == null) return false;

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

    private static boolean IsValidBlock(BlockPos pos) {
        Block block = mc.world.getBlockState(pos).getBlock();
        return block == Blocks.OBSIDIAN || block == Blocks.BEDROCK || block == Blocks.REINFORCED_DEEPSLATE || block == Blocks.NETHERITE_BLOCK;
    }

    public static boolean isCentered(PlayerEntity target) {
        if (target == null) return false;

        // Get the block position the player is standing on
        BlockPos blockPos = target.getBlockPos();

        // Calculate the center position of the block
        double centerX = blockPos.getX() + 0.5;
        double centerZ = blockPos.getZ() + 0.5;

        // Get the player's current position
        double playerX = target.getX();
        double playerZ = target.getZ();

        // Calculate the distance between the player's position and the center of the block
        double distanceX = Math.abs(playerX - centerX);
        double distanceZ = Math.abs(playerZ - centerZ);

        // Define a threshold to consider the player as centered
        double threshold = 0.2; // You can adjust this threshold based on your needs

        // Check if the player is within the threshold distance from the center in both X and Z directions
        return distanceX < threshold && distanceZ < threshold;
    }
}