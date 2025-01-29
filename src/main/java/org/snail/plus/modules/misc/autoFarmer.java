package org.snail.plus.modules.misc;

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
import net.minecraft.block.CropBlock;
import net.minecraft.item.HoeItem;
import net.minecraft.util.math.BlockPos;
import org.snail.plus.Addon;
import org.snail.plus.utilities.MathHelper;
import org.snail.plus.utilities.swapUtils;

import java.util.*;

import static org.snail.plus.utilities.MathHelper.getCrosshairBlock;

public class autoFarmer extends Module {
    private final SettingGroup sgBreak = settings.createGroup("Break");
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<breakMode> Mode = sgBreak.add(new EnumSetting.Builder<breakMode>()
            .name("break-mode")
            .description("How to calculate the blocks to break.")
            .defaultValue(breakMode.crosshair)
            .build());

    private final Setting<Boolean> rotate = sgBreak.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Automatically faces the crops.")
            .defaultValue(true)
            .visible(() -> Mode.get() != breakMode.crosshair)
            .build());

    private final Setting<Double> range = sgBreak.add(new DoubleSetting.Builder()
            .name("range")
            .description("The maximum distance to search for crops.")
            .sliderRange(0, 8)
            .build());

    private final Setting<Integer> minAge = sgBreak.add(new IntSetting.Builder()
            .name("min-age")
            .description("The minimum age of the crop to harvest.")
            .sliderRange(0, 10)
            .build());

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The side color.")
            .defaultValue(new SettingColor(255, 255, 255, 75))
            .build());

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The line color.")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .build());

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build());

    public autoFarmer() {
        super(Addon.CATEGORY, "Farmer", "Automatically breaks crop blocks");
    }

    private final Set<BlockPos> blocks = new HashSet<>();
    private FindItemResult hoe;
    private BlockPos crosshairBlock;
    private BlockPos bestPos;
    Runnable breakBlock = () -> {
        for (BlockPos pos : blocks) {
            if (mc.player.getBlockPos().isWithinDistance(pos, range.get())) {
                bestPos = pos;
                InvUtils.swap(hoe.slot(), true);
                breakCrop(bestPos);
                InvUtils.swapBack();
            }
        }
    };

    Runnable reset = () -> {
        crosshairBlock = null;
        synchronized (blocks) {
            blocks.clear();
        }
        bestPos = null;
    };

    @Override
    public void onActivate() {
        reset.run();
    }

    @Override
    public void onDeactivate() {
        reset.run();
    }

    public List<BlockPos> getBlocks(BlockPos center, double radius) {
        return MathHelper.getSphere(center, radius).stream().filter(pos -> {
                    if (mc.world.getBlockState(pos).getBlock() instanceof CropBlock crop) {
                        return crop.getMaxAge() - mc.world.getBlockState(pos).get(CropBlock.AGE) >= minAge.get();
                    }
                    return false;
                }).findFirst()
                .map(Collections::singletonList)
                .orElse(Collections.emptyList());
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        try {
            switch (Mode.get()) {
                case crosshair -> {
                    if (mc.world != null && mc.player != null) {
                        hoe = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof HoeItem);
                        //check if crosshair is on crop
                        crosshairBlock = getCrosshairBlock();
                        if (crosshairBlock != null && mc.world.getBlockState(crosshairBlock).getBlock() instanceof CropBlock) {
                            blocks.clear();
                            blocks.add(crosshairBlock);
                        }
                    }
                }

                case radius -> {
                    if (mc.player != null) {
                        blocks.clear();
                        blocks.addAll(getBlocks(mc.player.getBlockPos(), range.get()));
                    }
                }
            }
            breakBlock.run();
        } catch (Exception e) {
            error("An error occurred: " + e.getMessage());
            Addon.Logger.error("An error occurred: {}", Arrays.toString(e.getStackTrace()));
        }
    }

    private void breakCrop(BlockPos cropBlock) {
        if (hoe != null) {
            swapUtils.moveSwitch(hoe.slot(), mc.player.getInventory().selectedSlot);
            if (rotate.get()) {
                Rotations.rotate(Rotations.getYaw(cropBlock), Rotations.getPitch(cropBlock), 100, () -> BlockUtils.breakBlock(cropBlock, true));
            } else {
                BlockUtils.breakBlock(cropBlock, true);
            }
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (bestPos != null && mc.world != null && mc.world.getBlockState(bestPos).getBlock() instanceof CropBlock) {
            event.renderer.box(bestPos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }

    public enum breakMode {
        crosshair,
        radius
    }
}