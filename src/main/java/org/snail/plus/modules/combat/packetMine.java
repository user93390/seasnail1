package org.snail.plus.modules.combat;

import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import org.snail.plus.Addon;

import java.util.Arrays;

public class packetMine extends Module {
    private static BlockPos position;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("The range to mine blocks.")
            .defaultValue(5)
            .sliderRange(1, 10)
            .build());

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Faces towards the block being mined.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> instant = sgGeneral.add(new BoolSetting.Builder()
            .name("instant")
            .description("Instantly mines blocks.")
            .defaultValue(false)
            .build());

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("The delay between breaks in ticks.")
            .defaultValue(0)
            .sliderRange(0, 20)
            .build());

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-switch")
            .description("Automatically switches to the best tool in your hotbar.")
            .defaultValue(true)
            .build());

    private final Setting<Integer> maxBreaks = sgGeneral.add(new IntSetting.Builder()
            .name("max-breaks")
            .description("The maximum amount of breaks before resetting.")
            .defaultValue(5)
            .sliderRange(1, 10)
            .build());

    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The side color.")
            .defaultValue(new SettingColor(255, 255, 255, 75))
            .build());

    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The line color.")
            .defaultValue(new SettingColor(255, 255, 255, 75))
            .build());

    private double progress = 0;
    private boolean swapped, resync;
    private int slot = 0, breaks = 0;
    Direction direction;
    int delayTimer = 0;
    long lastMineTime = 0;

    private final Runnable sendPacket = () -> {
        if (instant.get()) {
            mc.player.networkHandler.sendPacket(
                    new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, position, Direction.DOWN)
            );
        } else {
            mc.player.networkHandler.sendPacket(
                    new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, position, direction)
            );

            if (progress >= 0.95) {
                mc.player.networkHandler.sendPacket(
                        new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, position, direction)
                );
            }
        }
    };

    private final Runnable reset = () -> {
        delayTimer = 0;
        progress = 0;
        slot = 0;
        swapped = false;
        resync = true;
        breaks = 0;
    };

    private final Runnable breakBlock = () -> {
        sendPacket.run();
        breaks++;
    };

    public packetMine() {
        super(Addon.Snail, "packetMine", "Mines blocks better and faster using packets.");
    }

    @Override
    public void onActivate() {
        reset.run();
        position = null;
    }

    @Override
    public void onDeactivate() {
        reset.run();
        position = null;
    }

    @EventHandler
    private void onMine(StartBreakingBlockEvent event) {
        if (mc.player.getBlockPos().getSquaredDistance(event.blockPos) > range.get() * range.get() || mc.world.getBlockState(event.blockPos).getBlock() == Blocks.BEDROCK)
            return;

        if (position != event.blockPos) {
            reset.run();
        }
        position = event.blockPos;
        direction = event.direction;
        swapped = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        try {
            if (breaks >= maxBreaks.get()) {
                reset.run();
            }

            if (position != null && mc.world != null) {
                FindItemResult pickaxe = InvUtils.findFastestTool(mc.world.getBlockState(position));
                slot = pickaxe.slot();

                if (resync) {
                    InvUtils.swap(mc.player.getInventory().selectedSlot, false);
                    resync = false;
                }

                if (!pickaxe.found() || mc.player.getInventory().selectedSlot == slot) return;

                if (autoSwitch.get() && progress >= 0.95 && mc.player.getInventory().selectedSlot != slot && !swapped) {
                    mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
                    swapped = true;
                    if (rotate.get()) {
                        Rotations.rotate(Rotations.getYaw(position), Rotations.getPitch(position));
                    }
                    return;
                }

                if (progress >= 1.0) {
                    mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
                    swapped = false;

                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastMineTime < delay.get() * 50) {
                        return;
                    }

                    breakBlock.run();
                    lastMineTime = currentTime;
                } else {
                    progress += BlockUtils.getBreakDelta(slot, mc.world.getBlockState(position));
                }
            }
        } catch (Exception e) {
            error("An error occurred while mining: " + e.getMessage());
            Addon.LOGGER.error("An error occurred while mining: {}", Arrays.toString(e.getStackTrace()));
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (position != null && slot != -1 && progress != -1) {
            double clampedValue = 1.0F - MathHelper.clamp((progress > 0.5F ? progress - 0.5F : 0.5F - progress) * 2.0F, 0.0F, 1.0F);
            VoxelShape shape = mc.world.getBlockState(position).getOutlineShape(mc.world, position);
            if (!shape.isEmpty()) {
                Box box = shape.getBoundingBox();
                double shrinkX = box.getLengthX() * clampedValue * 0.5;
                double shrinkY = box.getLengthY() * clampedValue * 0.5;
                double shrinkZ = box.getLengthZ() * clampedValue * 0.5;

                //smoothness
                double smoothness = Math.max(1, 1 / clampedValue);
                event.renderer.box(
                        position.getX() + box.minX + shrinkX / smoothness,
                        position.getY() + box.minY + shrinkY / smoothness,
                        position.getZ() + box.minZ + shrinkZ / smoothness,
                        position.getX() + box.maxX - shrinkX / smoothness,
                        position.getY() + box.maxY - shrinkY / smoothness,
                        position.getZ() + box.maxZ - shrinkZ / smoothness,
                        sideColor.get(), lineColor.get(), ShapeMode.Both, 0
                );
            }
        }
    }
}
