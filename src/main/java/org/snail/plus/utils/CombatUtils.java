package org.snail.plus.utils;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.explosion.Explosion;

import java.util.Objects;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class CombatUtils {

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
        return !WorldUtils.isAir(target.getBlockPos());
    }

    public static boolean isTrapped(PlayerEntity player) {
        int x = player.getBlockZ();
        int y = (player.getBlockZ() + 2);
        int z = player.getBlockZ();
        return WorldUtils.isAir(new BlockPos(x, y, z));
    }

    public static boolean willPop(PlayerEntity entity, Explosion explosion) {
        if(mc.player.getOffHandStack() == Items.TOTEM_OF_UNDYING.getDefaultStack()) {
            if(explosion.getPower() >= entity.getHealth()) {
                return true;
            }
        }
        return false;
    }
}