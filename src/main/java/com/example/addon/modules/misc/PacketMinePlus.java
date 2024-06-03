package com.example.addon.modules.misc;

import java.util.ArrayList;
import java.util.List;

import com.example.addon.Addon;

import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class PacketMinePlus extends Module {

    public enum AutoSwitchMode {
        Silent,
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> instant = sgGeneral.add(new BoolSetting.Builder()
            .name("instant")
            .description("Instantly remines the block")
            .defaultValue(false)
            .build());

    private final Setting<AutoSwitchMode> swap = sgGeneral.add(new EnumSetting.Builder<AutoSwitchMode>()
            .name("swap-mode")
            .description("Swapping method")
            .defaultValue(AutoSwitchMode.Silent)
            .build());

    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder()
            .name("debug")
            .description("Shows more info about the module")
            .defaultValue(false)
            .build());

    private final Setting<SettingColor> gradientColor1 = sgGeneral.add(new ColorSetting.Builder()
            .name("gradient-color-1")
            .description("The first color of the gradient.")
            .defaultValue(new SettingColor(255, 0, 0, 204)) // Default to red with 80% opacity
            .build());

    private final Setting<SettingColor> gradientColor2 = sgGeneral.add(new ColorSetting.Builder()
            .name("gradient-color-2")
            .description("The second color of the gradient.")
            .defaultValue(new SettingColor(0, 255, 0, 204)) // Default to green with 80% opacity
            .build());

    private final List<BlockPos> blocksToMine = new ArrayList<>();
    private BlockPos currentBlock;
    private int mineProgress;

    public PacketMinePlus() {
        super(Addon.Snail, "PacketMine+", "Mines blocks using packets (in testing, may have bugs)");
    }

    @Override
    public void onActivate() {
        blocksToMine.clear();
        currentBlock = null;
        mineProgress = 0;
    }

    @EventHandler
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        if (BlockUtils.canBreak(event.blockPos)) {
            blocksToMine.add(event.blockPos);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (debug.get() && !blocksToMine.isEmpty()) {
            ChatUtils.sendMsg(Text.of(Formatting.RED + "Breaking block"));
        }

        if (!blocksToMine.isEmpty()) {
            if (currentBlock == null || mineProgress >= 10) { // Reset after full mine or no block
                currentBlock = blocksToMine.remove(0);
                mineProgress = 0;
            } else {
                mineProgress++;
            }
            breakBlock(currentBlock);
        }
    }

    public void addBlockToMine(BlockPos pos) {
        blocksToMine.add(pos);
    }

    private void breakBlock(BlockPos pos) {
        if (debug.get()) {
            ChatUtils.sendMsg(Text.of(Formatting.RED + "Breaking block at " + pos));
        }

        // Find the best available pickaxe in the hotbar
        FindItemResult pickaxe = InvUtils.findInHotbar(itemStack -> itemStack.getItem() == Items.NETHERITE_PICKAXE || itemStack.getItem() == Items.DIAMOND_PICKAXE);

        if (!pickaxe.found()) {
            if (debug.get()) {
                ChatUtils.sendMsg(Text.of(Formatting.RED + "No suitable pickaxe found in hotbar"));
            }
            return;
        }
        int itemSlot = pickaxe.slot();
        int selectedSlot = mc.player.getInventory().selectedSlot;

        InvUtils.swap(itemSlot, false);
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, Direction.DOWN));
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.DOWN));
        mc.player.swingHand(Hand.MAIN_HAND);
    }
}
