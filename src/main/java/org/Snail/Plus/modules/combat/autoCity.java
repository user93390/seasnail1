package org.Snail.Plus.modules.combat;

import org.Snail.Plus.Addon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class autoCity extends Module {



    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("target-range")
        .description("The radius in which players get targeted.")
        .defaultValue(4)
        .min(0)
        .sliderMax(5)
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

    public autoCity() {
        super(Addon.Snail, "Auto City+", "Auto city but better ");
    }
    @EventHandler
    private void onTick(TickEvent.Post event) {
        PlayerEntity target = TargetUtils.getPlayerTarget(range.get(), priority.get());
        if (mc.player != null && mc.world != null && target != null) {
            City(target, rotate.get(), false, onlyInHoles.get());
        }
    }
    private void City(PlayerEntity target, boolean rotate, boolean support, Boolean onlyInHoles) {
        BlockPos cityposOne = target.getBlockPos().north(1);
        BlockPos cityposTwo = target.getBlockPos().south(1);
        BlockPos cityposThree = target.getBlockPos().east(1);
        BlockPos cityposFour = target.getBlockPos().west(1);
        FindItemResult Pickaxe = InvUtils.findInHotbar(itemStack -> itemStack.getItem() == Items.NETHERITE_PICKAXE || itemStack.getItem() == Items.DIAMOND_PICKAXE);

        if (mc.world.getBlockState(cityposThree).getBlock() != Blocks.BEDROCK) {
            InvUtils.swap(Pickaxe.slot(), false);
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, cityposThree, Direction.DOWN));
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, cityposThree, Direction.DOWN));
            mc.player.swingHand(Hand.MAIN_HAND);

        } else if (mc.world.getBlockState(cityposThree).getBlock() == Blocks.BEDROCK) {
            InvUtils.swap(Pickaxe.slot(), false);
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, cityposOne, Direction.DOWN));
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, cityposOne, Direction.DOWN));
            mc.player.swingHand(Hand.MAIN_HAND);

        } else if (mc.world.getBlockState(cityposOne).getBlock() == Blocks.BEDROCK) {
            InvUtils.swap(Pickaxe.slot(), false);
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, cityposTwo, Direction.DOWN));
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, cityposTwo, Direction.DOWN));
            mc.player.swingHand(Hand.MAIN_HAND);

        } else if (mc.world.getBlockState(cityposTwo).getBlock() == Blocks.BEDROCK) {
            InvUtils.swap(Pickaxe.slot(), false);
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, cityposFour, Direction.DOWN));
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, cityposFour, Direction.DOWN));
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }
}