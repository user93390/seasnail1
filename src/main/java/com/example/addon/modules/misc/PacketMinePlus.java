package com.example.addon.modules.misc;

import com.example.addon.Addon;
import com.example.addon.utils.PlayerUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.utils.player.ChatUtils;

import java.util.ArrayList;
import java.util.List;

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

    private final List<BlockPos> blocksToMine = new ArrayList<>();

    public PacketMinePlus() {
        super(Addon.MISC, "PacketMine+", "Mines blocks using packets (in testing, may have bugs)");
    }

    @Override
    public void onActivate() {
        blocksToMine.clear();
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
            BlockPos blockToMine = blocksToMine.remove(0);
            breakBlock(blockToMine);
        }
    }

    public void addBlockToMine(BlockPos pos) {
        blocksToMine.add(pos);
    }

    private void breakBlock(BlockPos pos) {
        if (debug.get()) {
            ChatUtils.sendMsg(Text.of(Formatting.RED + "Breaking block at " + pos));
        }

        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, Direction.DOWN));

        // Simulate releasing the mouse button after a short delay
        new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.DOWN));
        }).start();

        InvUtils.swap(InvUtils.find(Items.NETHERITE_PICKAXE, Items.DIAMOND_PICKAXE).slot(), false);
        InvUtils.swapBack();
    }
}
