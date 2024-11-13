package org.snail.plus.utils;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Objects;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class CombatUtils {

    private static boolean IsValidBlock(BlockPos pos) {
        Block block = Objects.requireNonNull(mc.world).getBlockState(pos).getBlock();
        return block == Blocks.OBSIDIAN || block == Blocks.BEDROCK || block == Blocks.REINFORCED_DEEPSLATE || block == Blocks.NETHERITE_BLOCK || block == Blocks.CRYING_OBSIDIAN || block == Blocks.ENDER_CHEST || block == Blocks.ANVIL;
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


    public static boolean isBurrowed(PlayerEntity target) {
        Vec3d pos = new Vec3d(target.getX(), target.getY() + 0.4, target.getZ());
        return target.getY() == target.prevY && IsValidBlock(BlockPos.ofFloored(pos));
    }

    public static PlayerEntity filter(List<AbstractClientPlayerEntity> playerEntities, filterMode Mode) {
        PlayerEntity target = null;
        double distance = 0;
        double health = 0;

        playerEntities.remove(mc.player);
        playerEntities.removeAll(WorldUtils.getAllFriends());


        for (PlayerEntity player : playerEntities) {
            double currentDistance = mc.player.distanceTo(player);
            double currentHealth = player.getHealth();

            switch (Mode) {
                case Closet -> {
                    if (distance == 0 || currentDistance < distance) {
                        distance = currentDistance;
                        target = player;
                    }
                }
                case Furthest -> {
                    if (distance == 0 || currentDistance > distance) {
                        distance = currentDistance;
                        target = player;
                    }
                }
                case LowestHealth -> {
                    if (health == 0 || currentHealth < health) {
                        health = currentHealth;
                        target = player;
                    }
                }
                case HighestHealth -> {
                    if (health == 0 || currentHealth > health) {
                        health = currentHealth;
                        target = player;
                    }
                }
            }
        }
        return target;
    }

    public static boolean isSurrounded(PlayerEntity player) {
        BlockPos pos = player.getBlockPos();
        BlockPos[] blocks = {
                pos.north(), pos.south(), pos.east(), pos.west()
        };

        for (BlockPos block : blocks) {
            if (WorldUtils.isAir(block)) {
                return false;
            }
        }
        return true;
    }

    public enum DmgTye {
        Bed,
        crystal,
    }

    public enum filterMode {
        Closet,
        Furthest,
        LowestHealth,
        HighestHealth,
    }
}