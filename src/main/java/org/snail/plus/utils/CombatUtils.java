package org.snail.plus.utils;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.snail.plus.Addon;

import java.util.Objects;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class CombatUtils {
    public static boolean isSurrounded(PlayerEntity target) {
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
        Block block = Objects.requireNonNull(mc.world).getBlockState(pos).getBlock();
        return block == Blocks.OBSIDIAN || block == Blocks.BEDROCK || block == Blocks.REINFORCED_DEEPSLATE || block == Blocks.NETHERITE_BLOCK || block == Blocks.CRYING_OBSIDIAN || block == Blocks.ENDER_CHEST || block == Blocks.ANVIL || block == Blocks.COBWEB;
    }

    public static boolean isCentered(PlayerEntity target) {
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

    /**
     * Checks if the given player is burrowed (in other words, not in an air block).
     *
     * @param target The player entity to check.
     * @return True if the player is burrowed, false otherwise.
     */
    public static boolean isBurrowed(PlayerEntity target) {
        if(!mc.world.getBlockState(target.getBlockPos()).isAir()) {
            Addon.LOG.info("{} is burrowed", target.getName().getString());
            return  true;
        }
        return false;
    }


    public static Vec3d getBurrowedPos(PlayerEntity target) {
        return new Vec3d(target.getBlockZ(), target.getBlockZ(), target.getBlockZ());
    }

}