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
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
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
            .description("Automatically rotates towards the position where the block is being broken.")
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


    private final Setting<SettingColor> StartColor = sgGeneral.add(new ColorSetting.Builder()
            .name("start line color")
            .description("Line color")
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .build());

    private final Setting<SettingColor> EndColor = sgGeneral.add(new ColorSetting.Builder()
            .name("end side color")
            .description("Side color")
            .defaultValue(new SettingColor(255, 0, 0, 75))
            .build());

    public autoCity() {
        super(Addon.Snail, "Auto City+", "Auto city but better ");
    }

    private BlockPos currentPos;
    private final Color cSides = new Color();
    private final Color cLines = new Color();
    private final double blockBreakingProgress = 1;

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

                BlockPos SupportPosNorth = target.getBlockPos().north(1).down(1);
                BlockPos SupportPosSouth = target.getBlockPos().south(1).down(1);
                BlockPos SupportPosEast = target.getBlockPos().east(1).down(1);
                BlockPos SupportPosWest = target.getBlockPos().west(1).down(1);

                if (onlyInHoles.get() && !CombatUtils.isSurrounded(target)) return;

                if (Objects.requireNonNull(mc.world).getBlockState(East).getBlock() != Blocks.BEDROCK && !mc.world.getBlockState(East).isAir()) {
                    TryCityEast(target);
                    currentPos = East;
                    if (support.get() && mc.world.getBlockState(SupportPosEast).isAir()) {
                        SupportPlace(target, SupportPosEast);
                    }
                } else {
                    if (mc.world.getBlockState(West).getBlock() != Blocks.BEDROCK && !mc.world.getBlockState(West).isAir()) {
                        TryCityWest(target);
                        currentPos = West;
                        if (support.get() && mc.world.getBlockState(SupportPosWest).isAir()) {
                            SupportPlace(target, SupportPosWest);
                        }

                    } else {
                        if (mc.world.getBlockState(North).getBlock() != Blocks.BEDROCK && !mc.world.getBlockState(North).isAir()) {
                            TryCityNorth(target);
                            currentPos = North;
                            if (support.get() && mc.world.getBlockState(SupportPosNorth).isAir()) {
                                SupportPlace(target, SupportPosNorth);
                            }

                        } else {
                            if (mc.world.getBlockState(South).getBlock() != Blocks.BEDROCK && !mc.world.getBlockState(South).isAir()) {
                                TryCitySouth(target);
                                currentPos = South;
                                if (support.get() && mc.world.getBlockState(SupportPosSouth).isAir()) {
                                    SupportPlace(target, SupportPosSouth);
                                }
                            }
                        }
                    }
                }
                if (AntiBurrow.get() && CombatUtils.isBurrowed(target) && mc.world.getBlockState(BurrowPos).getBlock() != Blocks.BEDROCK && !mc.world.getBlockState(BurrowPos).isAir()) {
                    TryCityBurrow(target);
                }
            } catch (Exception e) {
                System.out.println("unknown error... Please config auto-city correctly ");
            }
        }
    }


    @EventHandler
    private void CityRender(Render3DEvent event) {
        if (currentPos != null) {
            double ShrinkFactor = 1d - blockBreakingProgress / 10.0;
            BlockState state = Objects.requireNonNull(mc.world).getBlockState(currentPos);
            VoxelShape shape = state.getOutlineShape(mc.world, currentPos);
            if (shape.isEmpty()) {
                return;
            }
            Box orig = shape.getBoundingBox();

            Box box = orig.shrink(
                    orig.getLengthX() * ShrinkFactor,
                    orig.getLengthY() * ShrinkFactor,
                    orig.getLengthZ() * ShrinkFactor
            );

            renderBlock(event, orig, currentPos, ShrinkFactor, blockBreakingProgress);
        }
    }

    private void renderBlock(Render3DEvent event, Box orig, BlockPos pos, double shrinkFactor, double progress) {
        Box box = orig.shrink(
                orig.getLengthX() * shrinkFactor,
                orig.getLengthY() * shrinkFactor,
                orig.getLengthZ() * shrinkFactor
        );

        double xShrink = (orig.getLengthX() * shrinkFactor) / 2;
        double yShrink = (orig.getLengthY() * shrinkFactor) / 2;
        double zShrink = (orig.getLengthZ() * shrinkFactor) / 2;

        double x1 = pos.getX() + box.minX + xShrink;
        double y1 = pos.getY() + box.minY + yShrink;
        double z1 = pos.getZ() + box.minZ + zShrink;
        double x2 = pos.getX() + box.maxX + xShrink;
        double y2 = pos.getY() + box.maxY + yShrink;
        double z2 = pos.getZ() + box.maxZ + zShrink;

        Color c1Sides = StartColor.get().copy().a(StartColor.get().a / 2);
        Color c2Sides = EndColor.get().copy().a(EndColor.get().a / 2);

        cSides.set(
                (int) Math.round(c1Sides.r + (c2Sides.r - c1Sides.r) * progress),
                (int) Math.round(c1Sides.g + (c2Sides.g - c1Sides.g) * progress),
                (int) Math.round(c1Sides.b + (c2Sides.b - c1Sides.b) * progress),
                (int) Math.round(c1Sides.a + (c2Sides.a - c1Sides.a) * progress)
        );

        Color c1Lines = StartColor.get();
        Color c2Lines = EndColor.get();

        cLines.set(
                (int) Math.round(c1Lines.r + (c2Lines.r - c1Lines.r) * progress),
                (int) Math.round(c1Lines.g + (c2Lines.g - c1Lines.g) * progress),
                (int) Math.round(c1Lines.b + (c2Lines.b - c1Lines.b) * progress),
                (int) Math.round(c1Lines.a + (c2Lines.a - c1Lines.a) * progress)
        );

        event.renderer.box(x1, y1, z1, x2, y2, z2, cSides, cLines, ShapeMode.Both, 0);
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

    private void SupportPlace(PlayerEntity target, BlockPos pos) {
        FindItemResult Block = InvUtils.findInHotbar(Items.OBSIDIAN);
        BlockUtils.place(pos, Block, rotate.get(), 0, true);
    }
}