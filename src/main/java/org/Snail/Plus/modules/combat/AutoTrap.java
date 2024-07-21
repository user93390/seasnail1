package org.Snail.Plus.modules.combat;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import org.Snail.Plus.Addon;
import org.Snail.Plus.utils.CombatUtils;

import java.util.Objects;


public class AutoTrap extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Double> Delay = sgGeneral.add(new DoubleSetting.Builder()
            .name("delay")
            .description("delay to place blocks")
            .defaultValue(3.0)
            .sliderMax(10.0)
            .sliderMin(0.0)
            .build());
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("Range to target player(s)")
            .defaultValue(3.0)
            .sliderMax(10.0)
            .sliderMin(1.0)
            .build());
    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
            .name("target-priority")
            .description("How to filter targets within range.")
            .defaultValue(SortPriority.LowestDistance)
            .build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotation")
            .description("Rotates towards the block when placing.")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> OnlySurrounded = sgGeneral.add(new BoolSetting.Builder()
            .name("only surrounded")
            .description("only targets players when they are surrounded")
            .defaultValue(false)
            .build());
    private final Setting<Boolean> AutoDisable = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-disable")
            .description("disables the module after placing")
            .defaultValue(true)
            .build());
    private final Setting<TrapMode> Mode = sgGeneral.add(new EnumSetting.Builder<TrapMode>()
            .name("trap mode")
            .description("trap mode")
            .defaultValue(TrapMode.head)
            .build());
    private final Setting<Boolean> SupportPlace = sgGeneral.add(new BoolSetting.Builder()
            .name("support place")
            .description("places support blocks")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> AntiStep = sgGeneral.add(new BoolSetting.Builder()
            .name("anti step")
            .description("auto skill issue")
            .defaultValue(false)
            .build());
    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder()
            .name("side color")
            .description("Side color")
            .defaultValue(new SettingColor(255, 0, 0, 75))
            .build());
    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
            .name("line color")
            .description("Line color")
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .build());
    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build());
    private long lastPlaceTime = 0;
    public AutoTrap() {
        super(Addon.Snail, "Auto trap+", "Traps players using blocks");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {

        long time = System.currentTimeMillis();
        if ((time - lastPlaceTime) < Delay.get() * 1000) return;
        lastPlaceTime = time;

        PlayerEntity target = TargetUtils.getPlayerTarget(range.get(), priority.get());
        if (SupportPlace.get() && OnlySurrounded.get()) {
            if (CombatUtils.isSurrounded(Objects.requireNonNull(target))) {
                PlaceSupportBlocks(target);
            }
        } else if (SupportPlace.get()) {
            PlaceSupportBlocks(target);
        }
        if (CombatUtils.isCentered(Objects.requireNonNull(target))) {
            switch (Mode.get()) {
                case head:
                    placeTopBlocks(target);
                    if (AutoDisable.get()) {
                        toggle();
                        return;
                    }
                    break;
                case full:
                    placeFullBlocks(target);
                    if (AutoDisable.get()) {
                        toggle();
                        return;
                    }
                    break;
                case face:
                    placeFaceBlocks(target);
                    if (AutoDisable.get()) {
                        toggle();
                        return;
                    }
                    break;
            }
        }
    }
    @EventHandler
    private void TrapRender(Render3DEvent event) {
        PlayerEntity target = TargetUtils.getPlayerTarget(range.get(), priority.get());
        BlockPos FullPosSouth = Objects.requireNonNull(target).getBlockPos().south(1).up(1);
        BlockPos FullPosNorth = target.getBlockPos().north(1).up(1);
        BlockPos FullPosWest = target.getBlockPos().west(1).up(1);
        BlockPos FullPosEast = target.getBlockPos().east(1).up(1);
        BlockPos FullPosSouth1 = target.getBlockPos().south(1).up(2);
        BlockPos FullPosNorth1 = target.getBlockPos().north(1).up(2);
        BlockPos FullPosWest1 = target.getBlockPos().west(1).up(2);
        BlockPos FullPosEast1 = target.getBlockPos().east(1).up(2);
        BlockPos HeadPos = target.getBlockPos().up(2);

        if (Mode.get() == TrapMode.head) {
            event.renderer.box(HeadPos, sideColor.get(), lineColor.get(), shapeMode.get(), (int) 1.0f);

        } else if (Mode.get() == TrapMode.full) {
            event.renderer.box(FullPosSouth, sideColor.get(), lineColor.get(), shapeMode.get(), (int) 1.0f);
            event.renderer.box(FullPosEast, sideColor.get(), lineColor.get(), shapeMode.get(), (int) 1.0f);
            event.renderer.box(FullPosNorth, sideColor.get(), lineColor.get(), shapeMode.get(), (int) 1.0f);
            event.renderer.box(FullPosWest, sideColor.get(), lineColor.get(), shapeMode.get(), (int) 1.0f);

            if (AntiStep.get()) {
                event.renderer.box(FullPosSouth1, sideColor.get(), lineColor.get(), shapeMode.get(), (int) 1.0f);
                event.renderer.box(FullPosEast1, sideColor.get(), lineColor.get(), shapeMode.get(), (int) 1.0f);
                event.renderer.box(FullPosNorth1, sideColor.get(), lineColor.get(), shapeMode.get(), (int) 1.0f);
                event.renderer.box(FullPosWest1, sideColor.get(), lineColor.get(), shapeMode.get(), (int) 1.0f);
            }
            event.renderer.box(HeadPos, sideColor.get(), lineColor.get(), shapeMode.get(), (int) 1.0f);
        }
    }
    @EventHandler
    private void SupportRender(Render3DEvent event) {
        PlayerEntity target = TargetUtils.getPlayerTarget(range.get(), priority.get());
        if (target != null && SupportPlace.get()) {
            BlockPos supportPosNorthUpOne = target.getBlockPos().north(1).up(1);
            BlockPos supportPosNorthUpTwo = target.getBlockPos().north(1).up(2);

            event.renderer.box(supportPosNorthUpOne, sideColor.get(), lineColor.get(), shapeMode.get(), (int) 1.0f);
            event.renderer.box(supportPosNorthUpTwo, sideColor.get(), lineColor.get(), shapeMode.get(), (int) 1.0f);
        }
    }
    private void placeFullBlocks(PlayerEntity target) {
        BlockPos HeadPos = target.getBlockPos().up(2);
        if (target != null) {
            BlockPos FullPosSouth = target.getBlockPos().south(1).up(1);
            BlockPos FullPosNorth = target.getBlockPos().north(1).up(1);
            BlockPos FullPosWest = target.getBlockPos().west(1).up(1);
            BlockPos FullPosEast = target.getBlockPos().east(1).up(1);
            BlockPos FullPosSouth1 = target.getBlockPos().south(1).up(2);
            BlockPos FullPosNorth1 = target.getBlockPos().north(1).up(2);
            BlockPos FullPosWest1 = target.getBlockPos().west(1).up(2);
            BlockPos FullPosEast1 = target.getBlockPos().east(1).up(2);


            BlockUtils.place(HeadPos, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);
            BlockUtils.place(FullPosSouth, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);
            BlockUtils.place(FullPosNorth, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);
            BlockUtils.place(FullPosWest, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);
            BlockUtils.place(FullPosEast, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);


            if (AntiStep.get()) {
                BlockUtils.place(FullPosSouth1, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);
                BlockUtils.place(FullPosNorth1, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);
                BlockUtils.place(FullPosWest1, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);
                BlockUtils.place(FullPosWest1, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);
                BlockUtils.place(FullPosEast1, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);
            }
        }
    }

    private void placeTopBlocks(PlayerEntity target) {
        BlockPos HeadPos = target.getBlockPos().up(2);
        if (target != null) {
            BlockUtils.place(HeadPos, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);
            BlockPos AntiStepHead = target.getBlockPos().up(3);
            if (AntiStep.get()) {
                BlockUtils.place(AntiStepHead, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);
            }
        }
    }

    private void placeFaceBlocks(PlayerEntity target) {

        BlockPos FullPosSouth = target.getBlockPos().south(1).up(1);
        BlockPos FullPosNorth = target.getBlockPos().north(1).up(1);
        BlockPos FullPosWest = target.getBlockPos().west(1).up(1);
        BlockPos FullPosEast = target.getBlockPos().east(1).up(1);
        BlockPos FullPosSouth1 = target.getBlockPos().south(1).up(2);
        BlockPos FullPosNorth1 = target.getBlockPos().north(1).up(2);
        BlockPos FullPosWest1 = target.getBlockPos().west(1).up(2);
        BlockPos FullPosEast1 = target.getBlockPos().east(1).up(2);
        if (target != null) {
            BlockUtils.place(FullPosSouth, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);
            BlockUtils.place(FullPosNorth, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);
            BlockUtils.place(FullPosWest, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);
            BlockUtils.place(FullPosEast, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);

            if (AntiStep.get()) {

                BlockUtils.place(FullPosSouth1, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);
                BlockUtils.place(FullPosNorth1, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);
                BlockUtils.place(FullPosWest1, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);
                BlockUtils.place(FullPosWest1, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);
                BlockUtils.place(FullPosEast1, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);
            }
        }
    }
    private void PlaceSupportBlocks(PlayerEntity target) {
        if (target != null) {
        BlockPos supportPosNorthUpOne = target.getBlockPos().north(1).up(1);
        BlockPos supportPosNorthUpTwo = target.getBlockPos().north(1).up(2);


            BlockUtils.place(supportPosNorthUpOne, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);
            BlockUtils.place(supportPosNorthUpTwo, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);
        }
    }
    public enum TrapMode {
        full,
        head,
        face,
    }

    public enum RenderMode {
        fading,
        normal,

    }
}