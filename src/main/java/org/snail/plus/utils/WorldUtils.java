package org.snail.plus.utils;

import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.Camera;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
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

    public static boolean hitBoxCheck(BlockPos pos) {
        return !EntityUtils.intersectsWithEntity(new Box(pos), entity -> !(entity instanceof ItemEntity));
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
        switch (Mode) {
            case Inventory -> {
                swapUtils.pickSwitch(item.slot());
                if (!packet) {
                    BlockUtils.place(pos, item, rotate, 100, true);
                } else {
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(pos), directionMode(directionMode), pos, false));
                }
                swapUtils.pickSwapBack();
                mc.player.swingHand(swingHand(hand));
            }
            case silent -> {
                InvUtils.swap(item.slot(), true);
                if (!packet) {
                    BlockUtils.place(pos, item, rotate, 100, true);
                } else {
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(pos), directionMode(directionMode), pos, false));
                }
                InvUtils.swapBack();
                mc.player.swingHand(swingHand(hand));
            }
            case normal -> {
                InvUtils.swap(item.slot(), true);
                if (!packet) {
                    BlockUtils.place(pos, item, rotate, 100, true);
                } else {
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(pos), directionMode(directionMode), pos, false));
                }
                mc.player.swingHand(swingHand(hand));
                }

            case none -> {
                if (!packet) {
                    BlockUtils.place(pos, item, rotate, 100, true);
                } else {
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(pos), directionMode(directionMode), pos, false));
                }
                mc.player.swingHand(swingHand(hand));
            }
        default -> throw new IllegalArgumentException("Unexpected value: " + Mode);
            }
        }
        public static void breakBlock(BlockPos pos, HandMode hand, DirectionMode directionMode, boolean packet, boolean instant, swapUtils.swapMode Mode, boolean rotate) {
           FindItemResult item = InvUtils.findFastestTool(mc.world.getBlockState(pos));
            if (rotate) {
                Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), 100);
            }
            switch (Mode) {
                case Inventory -> {
                    swapUtils.pickSwitch(item.slot());
                    if (!packet) {
                        BlockUtils.breakBlock(pos, false);
                    } else {
                        if(!instant) {
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, directionMode(directionMode)));
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, directionMode(directionMode)));
                        } else {
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, directionMode(directionMode)));
                        }
                    }
                    swapUtils.pickSwapBack();
                    mc.player.swingHand(swingHand(hand));
                }
                case silent -> {
                    InvUtils.swap(item.slot(), true);
                    if (!packet) {
                        BlockUtils.breakBlock(pos,  false);
                    } else {
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(pos), directionMode(directionMode), pos, false));
                    }
                    InvUtils.swapBack();
                    mc.player.swingHand(swingHand(hand));
                }
                case normal -> {
                    InvUtils.swap(item.slot(), false);
                    if (!packet) {
                        BlockUtils.breakBlock(pos, false);
                    } else {
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(pos), directionMode(directionMode), pos, false));
                    }
                    mc.player.swingHand(swingHand(hand));
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