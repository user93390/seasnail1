package org.snail.plus.utils;

import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class WorldUtils {
    public static boolean isAir(BlockPos position) {
        return mc.world.isAir(position) || mc.world.getBlockState(position).getBlock() == Blocks.FIRE;
    }

    public static List<AbstractClientPlayerEntity> getPlayers() {
        return mc.world.getPlayers();
    }

    public static boolean strictDirection(BlockPos position, DirectionMode Direction) {
        return switch (Direction) {
            case Down, Up -> mc.player.getEyePos().y <= position.getY() + 0.5;
            case North, West -> mc.player.getZ() < position.getZ();
            case East, South -> mc.player.getX() >= position.getX() + 1;
        };
    }

    public static boolean hitBoxCheck(BlockPos pos, boolean ignoreItem) {
        return !EntityUtils.intersectsWithEntity(new Box(pos), ignoreItem ? entity -> !(entity instanceof ItemEntity) : entity -> true);
    }

    public static List<AbstractClientPlayerEntity> getAllFriends() {
        return mc.world.getPlayers().stream().filter(Friends.get()::isFriend).toList();
    }

    public static void playSound(SoundEvent sound, float pitch) {
        mc.getSoundManager().play(PositionedSoundInstance.master(sound, pitch));
    }

    public static String getName(PlayerEntity entity) {
        return entity.getName().getString();
    }

    public static String getCoords(PlayerEntity player) {
        return "%s, %s, %s".formatted(player.getX(), player.getY(), player.getZ());
    }

    public static void placeBlock(FindItemResult item, BlockPos pos, HandMode hand, DirectionMode directionMode, boolean packet, swapUtils.swapMode Mode, boolean rotate) {
        if (rotate) {
            Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), 100);
        }
        Runnable placeAction = () -> {
            if (!packet) {
                BlockUtils.place(pos, item, rotate, 100, true);
            } else {
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(pos), directionMode(directionMode), pos, false));
            }
            mc.player.swingHand(swingHand(hand));
        };

        switch (Mode) {
            case Inventory -> {
                swapUtils.pickSwitch(item.slot());
                placeAction.run();
                swapUtils.pickSwapBack();
            }
            case silent -> {
                InvUtils.swap(item.slot(), true);
                placeAction.run();
                InvUtils.swapBack();
            }
            case normal -> {
                InvUtils.swap(item.slot(), true);
                placeAction.run();
            }
            case Move -> {
                swapUtils.moveSwitch(item.slot(), mc.player.getInventory().selectedSlot);
                placeAction.run();
                swapUtils.moveSwitch(mc.player.getInventory().selectedSlot, item.slot());
            }
            case none -> placeAction.run();
            default -> throw new IllegalArgumentException("Unexpected value: " + Mode);
        }
    }

    public static void breakBlock(BlockPos pos, HandMode hand, DirectionMode directionMode, boolean packet, boolean instant, swapUtils.swapMode Mode, boolean rotate) {
        FindItemResult item = InvUtils.findFastestTool(mc.world.getBlockState(pos));
        if (rotate) {
            Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), 100);
        }
        Runnable breakAction = () -> {
            if (!packet) {
                BlockUtils.breakBlock(pos, false);
            } else {
                if (!instant) {
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, directionMode(directionMode)));
                }
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, directionMode(directionMode)));
            }
            mc.player.swingHand(swingHand(hand));
        };

        switch (Mode) {
            case Inventory -> {
                swapUtils.pickSwitch(item.slot());
                breakAction.run();
                swapUtils.pickSwapBack();
            }
            case silent -> {
                InvUtils.swap(item.slot(), true);
                breakAction.run();
                InvUtils.swapBack();
            }
            case normal -> {
                InvUtils.swap(item.slot(), false);
                breakAction.run();
            }
        }
    }

    public static Hand swingHand(HandMode Mode) {
        return switch (Mode) {
            case MainHand -> Hand.MAIN_HAND;
            case Offhand -> Hand.OFF_HAND;
        };
    }

    protected static Direction directionMode(DirectionMode Mode) {
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