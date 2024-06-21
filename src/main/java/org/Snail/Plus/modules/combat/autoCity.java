package org.Snail.Plus.modules.combat;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.Snail.Plus.Addon;
import org.Snail.Plus.utils.CombatUtils;

import java.util.Objects;

public class autoCity extends Module {



    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("target-range")
        .description("The radius in which players get targeted.")
        .defaultValue(4)
        .min(0)
            .sliderMax(10)
        .build());

    private final Setting<Boolean> AntiBurrow = sgGeneral.add(new BoolSetting.Builder()
            .name("burrow")
            .description("mines players burrows")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Automatically rotates towards the position where the block is being placed.")
        .defaultValue(true)
        .build());

    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
        .name("target-priority")
        .description("How to filter targets within range.")
        .defaultValue(SortPriority.LowestDistance)
        .build());

    private final Setting<Boolean> support = sgGeneral.add(new BoolSetting.Builder()
        .name("support-place")
        .description("Places support blocks (Obsidian).")
        .defaultValue(false)
        .build());

    private final Setting<Boolean> onlyInHoles = sgGeneral.add(new BoolSetting.Builder()
            .name("only holes")
            .description("only targets players in holes")
        .defaultValue(true)
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

    public autoCity() {
        super(Addon.Snail, "Auto City+", "Auto city but better ");
    }

    private BlockPos currentPos;
    @EventHandler
    private void onTick(TickEvent.Post event) {
        PlayerEntity target = TargetUtils.getPlayerTarget(range.get(), priority.get());
        if (target != null) {
            try {
                BlockPos East = target.getBlockPos().east(1);
                BlockPos West = target.getBlockPos().west(1);
                BlockPos North = target.getBlockPos().north(1);
                BlockPos South = target.getBlockPos().south(1);
                BlockPos BurrowPos = target.getBlockPos();

                BlockPos currentPos = null;

                if (onlyInHoles.get() && !CombatUtils.isSurrounded(target)) return;

                if (mc.world.getBlockState(East).getBlock() != Blocks.BEDROCK && !mc.world.getBlockState(East).isAir()) {
                    TryCityEast(target);
                    currentPos = East;
                } else {
                    if (mc.world.getBlockState(West).getBlock() != Blocks.BEDROCK && !mc.world.getBlockState(West).isAir()) {
                        TryCityWest(target);
                        currentPos = West;
                    } else {
                        if (mc.world.getBlockState(North).getBlock() != Blocks.BEDROCK && !mc.world.getBlockState(North).isAir()) {
                            TryCityNorth(target);
                            currentPos = North;
                        } else {
                            if (mc.world.getBlockState(South).getBlock() != Blocks.BEDROCK && !mc.world.getBlockState(South).isAir()) {
                                TryCitySouth(target);
                                currentPos = South;
                            }
                        }
                    }
                }
                if (AntiBurrow.get() && CombatUtils.isBurrowed(target) && mc.world.getBlockState(BurrowPos).getBlock() != Blocks.BEDROCK && !mc.world.getBlockState(BurrowPos).isAir()) {
                    TryCityBurrow(target);
                }
            } catch (Exception e) {
                System.out.println("unknown error... Please config autocity correctly ");
            }
        }
    }

    @EventHandler
    private void Onrender(Render3DEvent event) {
        if (currentPos != null) {
            event.renderer.box(currentPos, sideColor.get(), lineColor.get(), ShapeMode.Both, (int) 1.0f);
        }
    }

    private void TryCityEast(PlayerEntity target) {
        BlockPos East = target.getBlockPos().east(1);
        FindItemResult Pickaxe = InvUtils.findInHotbar(Items.NETHERITE_PICKAXE, Items.DIAMOND_PICKAXE);
        InvUtils.swap(Pickaxe.slot(), true);
        Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, East, Direction.DOWN));
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, East, Direction.DOWN));
        assert mc.player != null;
        mc.player.swingHand(Hand.MAIN_HAND);
        InvUtils.swapBack();
    }

    private void TryCityWest(PlayerEntity target) {
        BlockPos West = target.getBlockPos().west(1);
        FindItemResult Pickaxe = InvUtils.findInHotbar(Items.NETHERITE_PICKAXE, Items.DIAMOND_PICKAXE);
        InvUtils.swap(Pickaxe.slot(), true);
        Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, West, Direction.DOWN));
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, West, Direction.DOWN));
        assert mc.player != null;
        mc.player.swingHand(Hand.MAIN_HAND);
        InvUtils.swapBack();
    }

    private void TryCityNorth(PlayerEntity target) {
        BlockPos North = target.getBlockPos().north(1);
        FindItemResult Pickaxe = InvUtils.findInHotbar(Items.NETHERITE_PICKAXE, Items.DIAMOND_PICKAXE);
        InvUtils.swap(Pickaxe.slot(), true);
        Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, North, Direction.DOWN));
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, North, Direction.DOWN));
        assert mc.player != null;
        mc.player.swingHand(Hand.MAIN_HAND);
        InvUtils.swapBack();
    }

    private void TryCitySouth(PlayerEntity target) {
        BlockPos South = target.getBlockPos().south(1);
        FindItemResult Pickaxe = InvUtils.findInHotbar(Items.NETHERITE_PICKAXE, Items.DIAMOND_PICKAXE);
        InvUtils.swap(Pickaxe.slot(), true);
        Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, South, Direction.DOWN));
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, South, Direction.DOWN));
        assert mc.player != null;
        mc.player.swingHand(Hand.MAIN_HAND);
        InvUtils.swapBack();
    }

    private void TryCityBurrow(PlayerEntity target) {
        BlockPos PlayerPos = target.getBlockPos();
        FindItemResult Pickaxe = InvUtils.findInHotbar(Items.NETHERITE_PICKAXE, Items.DIAMOND_PICKAXE);
        InvUtils.swap(Pickaxe.slot(), true);
        Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, PlayerPos, Direction.DOWN));
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, PlayerPos, Direction.DOWN));
        assert mc.player != null;
        mc.player.swingHand(Hand.MAIN_HAND);
        InvUtils.swapBack();
    }
}
/* TODO: Fix rendering logic and add fading mode to onrender */