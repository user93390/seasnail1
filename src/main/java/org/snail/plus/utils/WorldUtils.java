package org.snail.plus.utils;

import net.minecraft.block.Blocks;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Objects;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class WorldUtils {
    public static boolean isAir(BlockPos position) {
    return Objects.requireNonNull(mc.world).isAir(position) || mc.world.getBlockState(position).getBlock() == Blocks.FIRE;
    }
    public  static  boolean isBreakable(BlockPos position) {
        return Objects.requireNonNull(mc.world).isAir(position) || mc.world.getBlockState(position).getBlock() == Blocks.BEDROCK || mc.world.getBlockState(position).getBlock() == Blocks.BARRIER ;
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

}
