package dev.seasnail1.utilities;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class CombatUtils {
    public static boolean isValidBlock(BlockPos pos) {
        Block block = mc.world.getBlockState(pos).getBlock();
        return block.equals(Blocks.OBSIDIAN) || block.equals(Blocks.BEDROCK) || block.equals(Blocks.REINFORCED_DEEPSLATE) || block.equals(Blocks.NETHERITE_BLOCK) || block.equals(Blocks.CRYING_OBSIDIAN) || block.equals(Blocks.ENDER_CHEST) || block.equals(Blocks.ANVIL);
    }

    public static boolean isCentered(PlayerEntity target) {
        BlockPos blockPos = target.getBlockPos();
        double centerX = blockPos.getX() + 0.5;
        double centerZ = blockPos.getZ() + 0.5;
        double threshold = 0.2;
        return Math.abs(target.getX() - centerX) < threshold && Math.abs(target.getZ() - centerZ) < threshold;
    }

    public static boolean isBurrowed(PlayerEntity target) {
        Vec3d pos = new Vec3d(target.getX(), target.getY() + 0.4, target.getZ());
        return target.getY() == target.prevY && isValidBlock(BlockPos.ofFloored(pos));
    }

    public static PlayerEntity filter(List<AbstractClientPlayerEntity> playerEntities, filterMode mode, double range) {
        WorldUtils.getAllFriends().forEach(playerEntities::remove);

        return playerEntities.stream()
                .filter(player -> mc.player != null && mc.player.distanceTo(player) <= range)
                .min((player1, player2) -> {
                    double distance1 = mc.player.distanceTo(player1);
                    double distance2 = mc.player.distanceTo(player2);
                    double health1 = player1.getHealth();
                    double health2 = player2.getHealth();

                    return switch (mode) {
                        case Closet -> Double.compare(distance1, distance2);
                        case Furthest -> Double.compare(distance2, distance1);
                        case LowestHealth -> Double.compare(health1, health2);
                        case HighestHealth -> Double.compare(health2, health1);
                        case none -> 0;
                        case null -> throw new IllegalStateException("Unexpected value");
                    };
                }).orElse(null);
    }

    public enum filterMode {
        Closet,
        Furthest,
        LowestHealth,
        HighestHealth,
        none,
    }
}
