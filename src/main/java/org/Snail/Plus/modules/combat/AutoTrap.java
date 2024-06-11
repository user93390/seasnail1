package org.Snail.Plus.modules.combat;

import org.Snail.Plus.utils.PlayerUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.Snail.Plus.Addon;


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
    private final Setting<Boolean> AirPlace = sgGeneral.add(new BoolSetting.Builder()
            .name("air place")
            .description("uses no support blocks (does not work on every server)")
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
    private final Setting<Integer> rendertime = sgGeneral.add(new IntSetting.Builder()
            .name("render time")
            .description("render time")
            .defaultValue(3)
            .sliderMax(100)
            .sliderMin(1)
            .build());
    private final Setting<RenderMode> renderMode = sgGeneral.add(new EnumSetting.Builder<RenderMode>()
            .name("render mode")
            .description("render mode")
            .defaultValue(RenderMode.normal)
            .build());
    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build());
    private long lastPlaceTime = 0;
    private BlockPos currentPos;

    public AutoTrap() {
        super(Addon.Snail, "Auto Trap+", "Traps players using blocks, has compatibility with other modules");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        long time = System.currentTimeMillis();
        if ((time - lastPlaceTime) < Delay.get() * 1000) return;
        lastPlaceTime = time;
        PlayerEntity target = TargetUtils.getPlayerTarget(range.get(), priority.get());
        FindItemResult Block = InvUtils.findInHotbar(Items.OBSIDIAN);
        if (mc.world.getDimension().respawnAnchorWorks()) {
            ChatUtils.sendMsg(Text.of("cannot use anchors in this dimension."));
        }
        if (target != null && Block != null && !mc.player.isRiding() && !mc.world.getDimension().respawnAnchorWorks()) {
            BlockPos HeadPos = target.getBlockPos().up(2);

            if (PlayerUtils.isCentered(target)) {
                if (OnlySurrounded.get() && PlayerUtils.isSurrounded(target)) {
                    if (Mode.get() == TrapMode.head) {
                        placeTopBlocks(target);
                        currentPos = HeadPos;
                    } else if (Mode.get() == TrapMode.full) {
                        placeFullBlocks(target);
                    } else if (Mode.get() == TrapMode.face) {
                        PlaceFaceBlocks(target);
                    }
                }
            }
            if (!AirPlace.get() && PlayerUtils.isCentered(target)) {
                PlaceSupportBlocks(target);
            }
        }
    }

    private void TrapRender(Render3DEvent event) {
        event.renderer.box(currentPos, sideColor.get(), lineColor.get(), shapeMode.get(), (int) 1.0f);
    }

    private void placeFullBlocks(PlayerEntity target) {

        BlockPos HeadPos = target.getBlockPos().up(2);

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

    private void placeTopBlocks(PlayerEntity target) {
        BlockPos HeadPos = target.getBlockPos().up(2);
        BlockUtils.place(HeadPos, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);
        BlockPos AntiStepHead = target.getBlockPos().up(3);
        if (AntiStep.get()) {
            BlockUtils.place(AntiStepHead, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);
        }
    }

    private void PlaceFaceBlocks(PlayerEntity target) {
        BlockPos FullPosSouth = target.getBlockPos().south(1).up(1);
        BlockPos FullPosNorth = target.getBlockPos().north(1).up(1);
        BlockPos FullPosWest = target.getBlockPos().west(1).up(1);
        BlockPos FullPosEast = target.getBlockPos().east(1).up(1);
        BlockPos FullPosSouth1 = target.getBlockPos().south(1).up(2);
        BlockPos FullPosNorth1 = target.getBlockPos().north(1).up(2);
        BlockPos FullPosWest1 = target.getBlockPos().west(1).up(2);
        BlockPos FullPosEast1 = target.getBlockPos().east(1).up(2);

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

    private void PlaceSupportBlocks(PlayerEntity target) {
        BlockPos supportPosNorth = target.getBlockPos().north(1);
        BlockPos supportPosNorthUpOne = target.getBlockPos().north(1).up(1);
        BlockPos supportPosNorthUpTwo = target.getBlockPos().north(1).up(2);


        BlockUtils.place(supportPosNorth, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);
        BlockUtils.place(supportPosNorthUpOne, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);
        BlockUtils.place(supportPosNorthUpTwo, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);
    }

    public enum TrapMode {
        full,
        head,
        face,
    }

    public enum PlaceMode {
        side,
        line,
    }

    public enum RenderMode {
        fading,
        normal,

    }
}