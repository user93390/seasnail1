package dev.seasnail1.utilities;

import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.ItemEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class WorldUtils {
    public static boolean isAir(BlockPos position, boolean liquid) {
        BlockState blockState = mc.world.getBlockState(position);
        if (liquid) {
            return !blockState.getFluidState().isEmpty() || blockState.isAir();
        } else {
            return blockState.isAir();
        }
    }

    public static boolean replaceable(BlockPos block) {
        return mc.world.getBlockState(block).isReplaceable();
    }

    public static boolean intersects(BlockPos pos, boolean ignoreItem) {
        return !EntityUtils.intersectsWithEntity(new Box(pos), ignoreItem ? entity -> !(entity instanceof ItemEntity) : entity -> true);
    }

    public static List<AbstractClientPlayerEntity> getAllFriends() {
        return mc.world.getPlayers().stream().filter(
                Friends.get()::isFriend
        ).toList();
    }

    public static void playSound(SoundEvent sound, float pitch) {
        mc.getSoundManager().play(PositionedSoundInstance.master(sound, pitch));
    }

    public static String getCoords(BlockPos pos) {
        return "%s, %s, %s".formatted(pos.getX(), pos.getY(), pos.getZ());
    }

    public static Hand swingHand(HandMode Mode) {
        return switch (Mode) {
            case MainHand -> Hand.MAIN_HAND;
            case Offhand -> Hand.OFF_HAND;
        };
    }

    public static Direction directionMode(DirectionMode Mode) {
        return switch (Mode) {
            case Up -> Direction.UP;
            case Down -> Direction.DOWN;
            case North -> Direction.NORTH;
            case South -> Direction.SOUTH;
            case East -> Direction.EAST;
            case West -> Direction.WEST;
        };
    }

    public enum HandMode {
        MainHand,
        Offhand,
    }

    public enum DirectionMode {
        Up,
        Down,
        North,
        South,
        East,
        West,
    }
}