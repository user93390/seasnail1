package org.snail.plus.modules.misc;

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
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.snail.plus.Addon;
import org.snail.plus.utilities.MathUtils;

import java.util.ArrayList;
import java.util.List;

public class obsidianFarmer extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Double> mineRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("mine-range")
            .description("The range to mine ender chests.")
            .defaultValue(5)
            .sliderRange(1, 10)
            .build());

    private final Setting<mineMode> mode = sgGeneral.add(new EnumSetting.Builder<mineMode>()
            .name("mode")
            .description("The mining mode.")
            .defaultValue(mineMode.packet)
            .build());

    private final Setting<Boolean> instant = sgGeneral.add(new BoolSetting.Builder()
            .name("instant")
            .description("Instantly reMines ender chests.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-switch")
            .description("Automatically switches to a pickaxe.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> autoDisable = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-disable")
            .description("Disables the module when you have x amount of obsidian.")
            .defaultValue(true)
            .build());

    private final Setting<Integer> disableAmount = sgGeneral.add(new IntSetting.Builder()
            .name("disable-amount")
            .description("The amount of obsidian to disable the module.")
            .defaultValue(64)
            .visible(autoDisable::get)
            .build());

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("render")
            .description("Renders the block being mined.")
            .defaultValue(true)
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

    private BlockPos obsidianPosition;
    private List<BlockPos> posList;
    private final Runnable reset = () -> {
        synchronized (this) {
            mc.executeSync(() -> {
                obsidianPosition = null;
                posList = new ArrayList<>();
            });
        }
    };

    public obsidianFarmer() {
        super(Addon.Snail, "Obsidian Farmer", "Automatically mines obsidian.");
    }


    @Override
    public void onActivate() {
        reset.run();
    }

    @Override
    public void onDeactivate() {
        reset.run();
    }

    private List<BlockPos> getObsidianPositions(BlockPos starting, double radius) {
        return MathUtils.getSphere(starting, radius).stream()
                .filter(blockPos -> blockPos.isWithinDistance(mc.player.getBlockPos(), mineRange.get()) && isValid(blockPos))
                .findFirst()
                .map(List::of)
                .orElse(new ArrayList<>());
    }

    private boolean isValid(BlockPos pos) {
        return mc.world.getBlockState(pos).getBlock() == Blocks.ENDER_CHEST
                && mc.player.getBlockPos().getSquaredDistance(pos) <= mineRange.get() * mineRange.get();
    }

    void place() {
        int slot = InvUtils.findInHotbar(Items.ENDER_CHEST).slot();
        InvUtils.swap(slot, true);
        BlockUtils.place(obsidianPosition, InvUtils.findInHotbar(Items.ENDER_CHEST), true, 50, false);
        InvUtils.swapBack();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
            mc.execute(() -> {
                if (autoDisable.get() && mc.player.getInventory().count(Items.OBSIDIAN) >= disableAmount.get()) {
                    toggle();
                    return;
                }
                posList = getObsidianPositions(mc.player.getBlockPos(), 5);
                for (BlockPos pos : posList) {
                    obsidianPosition = pos;
                    if (mc.world.getBlockState(pos).isAir()) {
                        place();
                    } else if (mc.world.getBlockState(pos).getBlock() == Blocks.ENDER_CHEST) {
                        mineBlock();
                    }
                    place();
                }
            });
        }

    private void mineBlock() {
        FindItemResult pickaxe = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof PickaxeItem);
        if (pickaxe.found()) {
            if (autoSwitch.get()) {
                InvUtils.swap(pickaxe.slot(), false);
            }
            switch (mode.get()) {
                case packet -> {
                    if(!instant.get()) {
                        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, obsidianPosition, Direction.DOWN));
                    }
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, obsidianPosition, Direction.DOWN));
                }
                case vanilla -> BlockUtils.breakBlock(obsidianPosition, true);
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (render.get() && obsidianPosition != null) {
            event.renderer.box(obsidianPosition, sideColor.get(), lineColor.get(), ShapeMode.Both, 0);
        }
    }

    public enum mineMode {
        packet,
        vanilla
    }
}
