package org.snail.plus.utils;

import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.Blocks;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class WorldUtils {
    public static boolean isAir(BlockPos position) {
        return Objects.requireNonNull(mc.world).isAir(position) || mc.world.getBlockState(position).getBlock() == Blocks.FIRE;
    }

    public static boolean isBreakable(BlockPos position) {
        return Objects.requireNonNull(mc.world).isAir(position) || mc.world.getBlockState(position).getBlock() == Blocks.BEDROCK || mc.world.getBlockState(position).getBlock() == Blocks.BARRIER;
    }

    public static boolean strictDirection(BlockPos position, Direction Direction) {
        return switch (Direction) {
            case DOWN, UP -> Objects.requireNonNull(mc.player).getEyePos().y <= position.getY() + 0.5;
            case NORTH, WEST -> Objects.requireNonNull(mc.player).getZ() < position.getZ();
            case EAST, SOUTH -> Objects.requireNonNull(mc.player).getX() >= position.getX() + 1;
        };
    }

    public static SoundInstance playSound(SoundEvent sound, float Pitch) {
        mc.getSoundManager().play(PositionedSoundInstance.master(sound, Pitch));
        return (SoundInstance) sound;
    }

    public static String getName(PlayerEntity entity) {
        return entity.getName().getString();
    }

    public static String getCoords(PlayerEntity player) {
        return "%s, %s, %s".formatted(player.getX(), player.getY(), player.getZ());
    }

    public static void placeBlock(FindItemResult item, BlockPos pos, boolean packet, boolean silent, boolean rotate) {
        if (packet) {
            if(rotate) {
                Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), 100, () -> {
                    InvUtils.swap(item.slot(), true);
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false));
                    if (silent) InvUtils.swapBack();
                });
            }
        } else {
            BlockUtils.place(pos, item, rotate, 100, true);
        }
    }

    public static boolean doesntInsert(PlayerEntity entity) {
        return !entity.getBoundingBox().intersects(entity.getBlockPos().down(1).getX(), entity.getBlockPos().down(1).getY(), entity.getBlockPos().down(1).getZ(), entity.getBlockPos().down(1).getX() + 1, entity.getBlockPos().down(1).getY() + 1, entity.getBlockPos().down(1).getZ() + 1);
    }
}
