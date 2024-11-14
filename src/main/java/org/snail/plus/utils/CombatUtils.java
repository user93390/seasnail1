package org.snail.plus.utils;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class CombatUtils {

    private static boolean isValidBlock(BlockPos pos) {
        Block block = mc.world.getBlockState(pos).getBlock();
        return block == Blocks.OBSIDIAN || block == Blocks.BEDROCK || block == Blocks.REINFORCED_DEEPSLATE || block == Blocks.NETHERITE_BLOCK || block == Blocks.CRYING_OBSIDIAN || block == Blocks.ENDER_CHEST || block == Blocks.ANVIL;
    }

    public static boolean isCentered(PlayerEntity target) {
        BlockPos blockPos = target.getBlockPos();
        double centerX = blockPos.getX() + 0.5;
        double centerZ = blockPos.getZ() + 0.5;
        double playerX = target.getX();
        double playerZ = target.getZ();
        double threshold = 0.2;
        return Math.abs(playerX - centerX) < threshold && Math.abs(playerZ - centerZ) < threshold;
    }

    public static boolean isBurrowed(PlayerEntity target) {
        Vec3d pos = new Vec3d(target.getX(), target.getY() + 0.4, target.getZ());
        return target.getY() == target.prevY && isValidBlock(BlockPos.ofFloored(pos));
    }

    public static PlayerEntity filter(List<AbstractClientPlayerEntity> playerEntities, filterMode mode, double range) {
        playerEntities.remove(mc.player);
        playerEntities.removeAll(WorldUtils.getAllFriends());
        return playerEntities.stream()
            .filter(player -> mc.player != null && mc.player.distanceTo(player) <= range)
            .min((player1, player2) -> {
                double distance1 = mc.player != null ? mc.player.distanceTo(player1) : Double.MAX_VALUE;
                double distance2 = mc.player != null ? mc.player.distanceTo(player2) : Double.MAX_VALUE;
                double health1 = player1.getHealth();
                double health2 = player2.getHealth();

                return switch (mode) {
                    case Closet -> Double.compare(distance1, distance2);
                    case Furthest -> Double.compare(distance2, distance1);
                    case LowestHealth -> Double.compare(health1, health2);
                    case HighestHealth -> Double.compare(health2, health1);
                };
            })
            .orElse(null);
    }

    public static PlayerEntity getLastAttacker(Entity entity) {
        if (!entity.isAlive() && entity instanceof LivingEntity livingEntity) {
            return (PlayerEntity) livingEntity.getAttacker();
        }
        return null;
    }

    public enum filterMode {
        Closet,
        Furthest,
        LowestHealth,
        HighestHealth,
    }
}