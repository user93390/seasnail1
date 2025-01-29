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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import org.snail.plus.Addon;
import org.snail.plus.utilities.WorldUtils;

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

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-switch")
            .description("Automatically switches to the best tool in your hotbar.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> grow = sgGeneral.add(new BoolSetting.Builder()
            .name("grow")
            .description("Grows the render outline as you mine.")
            .defaultValue(true)
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

    long currentTime = System.currentTimeMillis();
    private double progress = 0;
    private int slot = 0;
    Direction direction;

    private final Runnable sendPacket = () -> {
        if (instant.get()) {
            mc.player.networkHandler.sendPacket(
                    new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, position, direction)
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
        progress = 0;
        slot = 0;
        currentTime = System.currentTimeMillis();
    };

    private final Runnable breakBlock = sendPacket::run;

    public packetMine() {
        super(Addon.CATEGORY, "packet-mine+", "Mines blocks better and faster using packets.");
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
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        try {
            if (position != null && mc.world != null) {
                FindItemResult pickaxe = InvUtils.findFastestTool(mc.world.getBlockState(position));
                int originalSlot = mc.player.getInventory().selectedSlot;
                slot = pickaxe.slot();

                if (pickaxe.found()) {
                    if (!pickaxe.found() || originalSlot == slot) {
                        return;
                    }

                    if (progress >= 0.75 && !WorldUtils.isAir(position, false)) {

                        if (rotate.get()) {
                            Rotations.rotate(Rotations.getYaw(position), Rotations.getPitch(position));
                        }

                        if (autoSwitch.get()) {
                            InvUtils.swap(slot, true);

                            breakBlock.run();

                            InvUtils.swapBack();
                        } else {
                            breakBlock.run();
                        }
                    }

                    if (!(progress >= 1.0)) progress += BlockUtils.getBreakDelta(slot, mc.world.getBlockState(position));
                }
            }
        } catch (Exception e) {
            error("An error occurred while mining: " + e.getMessage());
            Addon.Logger.error("An error occurred while mining: {}", Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (position != null && slot != -1 && progress != -1) {
            double clampedValue = Math.abs(MathHelper.clamp(progress, 0, 1)) * (grow.get() ? 2 : 1);
            VoxelShape shape = mc.world.getBlockState(position).getOutlineShape(mc.world, position);
            if (!shape.isEmpty()) {
                Box box = shape.getBoundingBox();
                double shrinkX = box.getLengthX() * clampedValue * 0.5;
                double shrinkY = box.getLengthY() * clampedValue * 0.5;
                double shrinkZ = box.getLengthZ() * clampedValue * 0.5;

                //smoothness
                double smoothness = Math.max(1, 1 / clampedValue);
                event.renderer.box(
                        position.getX() + box.minX + (shrinkX / smoothness),
                        position.getY() + box.minY + (shrinkY / smoothness),
                        position.getZ() + box.minZ + (shrinkZ / smoothness),
                        position.getX() + box.maxX - (shrinkX / smoothness),
                        position.getY() + box.maxY - (shrinkY / smoothness),
                        position.getZ() + box.maxZ - (shrinkZ / smoothness),
                        sideColor.get(), lineColor.get(), ShapeMode.Both, 0
                );
            }
        }
    }
}
