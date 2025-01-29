package org.snail.plus.modules.combat;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import org.snail.plus.Addon;

public class antiBurrow extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("The range to prevent players from burrowing.")
            .defaultValue(4.0)
            .sliderRange(0.0, 10.0)
            .build());

    private final Setting<mode> trapMode = sgGeneral.add(new EnumSetting.Builder<mode>()
            .name("trap mode")
            .description("The mode of the anti-burrow.")
            .defaultValue(mode.trap)
            .build());

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Automatically rotates towards the position where the player is burrowed.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> autoDisable = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-disable")
            .description("Disables the module when placed")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> renderBlock = sgRender.add(new BoolSetting.Builder()
            .name("render block")
            .description("Render the block where the player is burrowed.")
            .defaultValue(true)
            .build());

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side color")
            .description("Side color")
            .defaultValue(new SettingColor(0, 255, 255, 100))
            .build());

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line color")
            .description("Line color")
            .defaultValue(new SettingColor(0, 255, 255, 255))
            .build());

    private BlockPos targetPos;

    public antiBurrow() {
        super(Addon.CATEGORY, "anti-burrow", "Prevents players from burrowing. Rotations idea");
    }

    Runnable runnable = () -> {
        targetPos = null;
    };

    @Override
    public void onActivate() {
        runnable.run();
    }

    @Override
    public void onDeactivate() {
        runnable.run();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.world != null && mc.player != null) {
            for (PlayerEntity entity : mc.world.getPlayers()) {
                if (entity != mc.player && mc.player.distanceTo(entity) <= range.get()) {
                    targetPos = entity.getBlockPos();
                    switch (trapMode.get()) {
                        case trap -> {
                            FindItemResult item = InvUtils.findInHotbar(Blocks.OBSIDIAN.asItem());
                            targetPos = targetPos.up(2);
                            if (mc.world.getBlockState(targetPos).isAir()) {
                                BlockUtils.place(targetPos, item, rotate.get(), 100, true);
                            }
                        }

                        case button -> {
                            FindItemResult item = InvUtils.findInHotbar(itemStack -> itemStack.getItem() == Items.STONE_BUTTON || itemStack.getItem() == Items.OAK_BUTTON || itemStack.getItem() == Items.SPRUCE_BUTTON || itemStack.getItem() == Items.BIRCH_BUTTON || itemStack.getItem() == Items.JUNGLE_BUTTON || itemStack.getItem() == Items.ACACIA_BUTTON || itemStack.getItem() == Items.DARK_OAK_BUTTON || itemStack.getItem() == Items.CRIMSON_BUTTON || itemStack.getItem() == Items.WARPED_BUTTON);
                            BlockUtils.place(targetPos, item, rotate.get(), 100, false);
                        }
                    }
                    if (autoDisable.get()) toggle();
                }
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.world != null && mc.player != null) {
            if (targetPos != null) {
                event.renderer.box(targetPos, sideColor.get(), lineColor.get(), ShapeMode.Both, 0);
            }
        }
    }

    public enum mode {
        trap,
        button
    }
}
