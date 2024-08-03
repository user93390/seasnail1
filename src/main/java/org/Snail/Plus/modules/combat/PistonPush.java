package org.Snail.Plus.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.Snail.Plus.Addon;

import java.util.Objects;

public class PistonPush extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> Range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("The range to place pistons and redstone.")
            .defaultValue(4.0)
            .sliderMin(1.0)
            .sliderMax(10.0)
            .build());

    private final Setting<Double> PistonDelay = sgGeneral.add(new DoubleSetting.Builder()
            .name("piston delay")
            .description("Delay for placing pistons.")
            .defaultValue(1.0)
            .min(0)
            .sliderMax(5)
            .build());

    private final Setting<Double> RedStoneDelay = sgGeneral.add(new DoubleSetting.Builder()
            .name("redstone delay")
            .description("Delay for placing redstone.")
            .defaultValue(1.0)
            .min(0)
            .sliderMax(5)
            .build());

    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
            .name("target-priority")
            .description("How to filter targets within range.")
            .defaultValue(SortPriority.LowestDistance)
            .build());

    private final Setting<Boolean> MinePiston = sgGeneral.add(new BoolSetting.Builder()
            .name("mine piston")
            .description("Mines the piston to reset the cycle.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> MineRedstone = sgGeneral.add(new BoolSetting.Builder()
            .name("mine redstone")
            .description("Mines the redstone torch/block to reset the cycle.")
            .defaultValue(true)
            .build());

    private final Setting<Double> RedstoneMineDelay = sgGeneral.add(new DoubleSetting.Builder()
            .name("redstone mine delay")
            .description("Delay to mine redstone blocks.")
            .defaultValue(4.0)
            .sliderMin(1.0)
            .sliderMax(10.0)
            .visible(MineRedstone::get)
            .build());

    private final Setting<Double> PistonMineDelay = sgGeneral.add(new DoubleSetting.Builder()
            .name("piston mine delay")
            .description("Delay to mine pistons.")
            .defaultValue(1.0)
            .min(0)
            .sliderMax(5)
            .visible(MinePiston::get)
            .build());

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Automatically rotates towards the position where pistons are getting placed.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> strictDirection = sgGeneral.add(new BoolSetting.Builder()
            .name("strict direction")
            .description("Only places anchors in the direction you are facing. Will crash if you are falling.")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> FillHole = sgGeneral.add(new BoolSetting.Builder()
            .name("Fill hole")
            .description("Fills the hole when the player is pushed out.")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> Support = sgGeneral.add(new BoolSetting.Builder()
            .name("support")
            .description("Whether to place support blocks.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> Smart = sgGeneral.add(new BoolSetting.Builder()
            .name("smart")
            .description("Does more calculations.")
            .defaultValue(true)
            .build());

    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder()
            .name("side color")
            .description("Side color.")
            .defaultValue(new SettingColor(255, 0, 0, 75))
            .build());

    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
            .name("line color")
            .description("Line color.")
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .build());

    private final Setting<Boolean> crouch = sgGeneral.add(new BoolSetting.Builder()
            .name("crouch")
            .description("Makes you crouch when you are pushing players out.")
            .defaultValue(true)
            .build());

    public PistonPush() {
        super(Addon.Snail, "Piston Push+", "Pushes players out of their holes using pistons.");
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        try {
            PlayerEntity target = TargetUtils.getPlayerTarget(Range.get(), priority.get());
            if(target == null) return;
            BlockPos Piston = Objects.requireNonNull(target).getBlockPos().north(1).up(2);
                BlockPos Redstone = target.getBlockPos().north(1).up(3);
                FindItemResult Pickaxe = InvUtils.findInHotbar(Items.NETHERITE_PICKAXE, Items.DIAMOND_PICKAXE);
                TryNorthPiston(target);
                    if (MinePiston.get()) {
                        InvUtils.swap(Pickaxe.slot(), true);
                        Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, Piston, Direction.DOWN));
                        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, Piston, Direction.DOWN));
                        Objects.requireNonNull(mc.player).swingHand(Hand.MAIN_HAND);
                        InvUtils.swapBack();

                    }
                    if (MineRedstone.get()) {
                        InvUtils.swap(Pickaxe.slot(), true);
                        Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, Redstone, Direction.DOWN));
                        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, Redstone, Direction.DOWN));
                        Objects.requireNonNull(mc.player).swingHand(Hand.MAIN_HAND);
                        InvUtils.swapBack();
                    }
        } catch (Exception e) {
            System.out.println("Exception caught -> " + e.getCause() + ". Message -> " + e.getMessage());
        }
    }
    private Direction revert(Direction direction) {
        return switch (direction) {
            case NORTH -> Direction.SOUTH;
            case SOUTH -> Direction.NORTH;
            case WEST -> Direction.EAST;
            case EAST -> Direction.WEST;
            default -> throw new IllegalStateException("Unexpected value: " + direction);
        };
    }

    private void TryNorthPiston(PlayerEntity target) {

        BlockPos Piston = target.getBlockPos().north(1).up(1);
        BlockPos Redstone = target.getBlockPos().north(1).up(2);
        FindItemResult ItemPiston = InvUtils.findInHotbar(Items.PISTON);
        FindItemResult ItemRedstone = InvUtils.findInHotbar(Items.REDSTONE_BLOCK, Items.REDSTONE_TORCH);
        BlockUtils.place(Piston, ItemPiston, rotate.get(), 0, true);
        BlockUtils.place(Redstone, ItemRedstone, rotate.get(), 0, true);
    }
    private void TrySouthPiston(PlayerEntity target, Boolean crouch) {
        BlockPos Piston = target.getBlockPos().south(1).up(1);
        BlockPos Redstone = target.getBlockPos().south(1).up(2);

        FindItemResult ItemPiston = InvUtils.findInHotbar(Items.PISTON);
        FindItemResult ItemRedstone = InvUtils.findInHotbar(Items.REDSTONE_BLOCK, Items.REDSTONE_TORCH);

        Direction pistonFacing = target.getHorizontalFacing().getOpposite();

        InvUtils.swap(ItemPiston.slot(), true);
        (Objects.requireNonNull(mc.getNetworkHandler())).sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(Piston), pistonFacing, Piston, false), ItemPiston.slot()));
        InvUtils.swapBack();

        InvUtils.swap(ItemRedstone.slot(), true);
        (mc.getNetworkHandler()).sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(Redstone), Direction.DOWN, Redstone, false), ItemRedstone.slot()));
        InvUtils.swapBack();

        if (crouch.equals(true)) {
            Objects.requireNonNull(mc.player).setSneaking(true);
        }
    }
    private void TryEastPiston(PlayerEntity target, Boolean crouch) {
        BlockPos Piston = target.getBlockPos().east(1).up(1);
        BlockPos Redstone = target.getBlockPos().east(1).up(2);

        FindItemResult ItemPiston = InvUtils.findInHotbar(Items.PISTON);
        FindItemResult ItemRedstone = InvUtils.findInHotbar(Items.REDSTONE_BLOCK, Items.REDSTONE_TORCH);

        BlockUtils.place(Piston, ItemPiston, rotate.get(), 0, true);
        BlockUtils.place(Redstone, ItemRedstone, rotate.get(), 0, true);
        if (crouch.equals(true)) {
            mc.player.setSneaking(true);
        }
    }

    private void TryWestPiston(PlayerEntity target, Boolean crouch) {
        BlockPos Piston = target.getBlockPos().west(1).up(1);
        BlockPos Redstone = target.getBlockPos().west(1).up(2);

        FindItemResult ItemPiston = InvUtils.findInHotbar(Items.PISTON);
        FindItemResult ItemRedstone = InvUtils.findInHotbar(Items.REDSTONE_BLOCK, Items.REDSTONE_TORCH);

        BlockUtils.place(Piston, ItemPiston, rotate.get(), 0, true);
        BlockUtils.place(Redstone, ItemRedstone, rotate.get(), 0, true);
        if (crouch.equals(true)) {
            mc.player.setSneaking(true);
        }
    }
}
