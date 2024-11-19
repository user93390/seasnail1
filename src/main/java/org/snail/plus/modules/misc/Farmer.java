package org.snail.plus.modules.misc;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.CropBlock;
import net.minecraft.item.HoeItem;
import net.minecraft.util.math.BlockPos;
import org.snail.plus.Addon;
import org.snail.plus.utils.MathUtils;
import org.snail.plus.utils.WorldUtils;
import org.snail.plus.utils.swapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Farmer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("The maximum distance to search for crops.")
            .sliderRange(0, 8)
            .build());

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Automatically faces the crops.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> harvest = sgGeneral.add(new BoolSetting.Builder()
            .name("harvest")
            .description("Automatically harvests crops.")
            .defaultValue(true)
            .build());

    private final Setting<Integer> minAge = sgGeneral.add(new IntSetting.Builder()
            .name("min-age")
            .description("The minimum age of the crop to harvest.")
            .sliderRange(0, 10)
            .visible(harvest::get)
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

    ExecutorService executor = Executors.newSingleThreadExecutor();
    private List<BlockPos> cropsList = new ArrayList<>();

    public Farmer() {
        super(Addon.Snail, "Auto farmer", "Automatically harvests and replants crops.");
    }

    @Override
    public void onActivate() {
        if (executor == null || executor.isShutdown() || executor.isTerminated()) {
            executor = Executors.newSingleThreadExecutor();
        }

        cropsList = new ArrayList<>();
    }

    @Override
    public void onDeactivate() {
        cropsList.clear();
        if (executor != null) {
            executor.shutdown();
        }
    }

    private List<BlockPos> findCrops(BlockPos start) {
        return MathUtils.getSphere(start, MathUtils.getRadius((int) Math.sqrt(range.get()), (int) Math.sqrt(range.get())))
                .stream()
                .filter(pos -> {
                    if (isCrop(pos)) {
                        return harvest.get() && mc.world.getBlockState(pos).get(CropBlock.AGE) >= minAge.get();
                    }
                    return false;
                })
                .findFirst()
                .map(Collections::singletonList)
                .orElse(Collections.emptyList());
    }

    private boolean isCrop(BlockPos pos) {
        return mc.world.getBlockState(pos).getBlock() instanceof CropBlock;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        executor.submit(() -> {
            for (BlockPos pos : findCrops(mc.player.getBlockPos())) {
                if (pos.isWithinDistance(mc.player.getPos(), range.get()))
                    cropsList.add(pos);

                FindItemResult tool = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof HoeItem);

                if (tool.found()) {
                    if (harvest.get()) {
                        BreakCrop(pos, tool);
                        cropsList.remove(pos);
                    }
                }
            }
        });
    }

    private void BreakCrop(BlockPos pos, FindItemResult tool) {
        if (tool.found()) {
            InvUtils.move().from(tool.slot()).to(mc.player.getInventory().selectedSlot);
            WorldUtils.breakBlock(pos, WorldUtils.HandMode.MainHand, WorldUtils.DirectionMode.Down, false, false, swapUtils.swapMode.normal, rotate.get());
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        for (BlockPos pos : cropsList) {
            if (mc.player.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ()) > range.get()) continue;
            event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }
}
