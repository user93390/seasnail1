package com.example.addon.modules.combat;

import com.example.addon.Addon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.loader.impl.util.log.Log;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.message.SentMessage.Chat;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class autoCity extends Module {

    public autoCity() {
        super(Addon.COMBAT, "Auto City+", "Uses packets to break blocks and place crystals automatically.");
    }

    public enum SwapMode {
        Silent,
        Normal,
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> place = sgGeneral.add(new BoolSetting.Builder()
        .name("pre-place")
        .description("Determines if the module should place crystals after mining the block.")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> breakAfter = sgGeneral.add(new BoolSetting.Builder()
        .name("break-crystal")
        .description("Breaks the crystal when we mine the block.")
        .defaultValue(true)
        .build());

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

    private final Setting<SettingColor> supportColor = sgGeneral.add(new ColorSetting.Builder()
        .name("support-color")
        .description("The color for positions to be placed.")
        .defaultValue(new SettingColor(255, 0, 0, 75))
        .visible(support::get)
        .build());

    private final Setting<SwapMode> swap = sgGeneral.add(new EnumSetting.Builder<SwapMode>()
        .name("swap-mode")
        .description("Swap mode.")
        .defaultValue(SwapMode.Silent)
        .build());

    private final Setting<Boolean> onlyInHoles = sgGeneral.add(new BoolSetting.Builder()
        .name("only-in-holes")
        .description("Will not work right now but will soon.")
        .defaultValue(true)
        .build());

        private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("color")
        .description("The color for positions cityed block")
        .defaultValue(new SettingColor(255, 0, 0, 75))
        .visible(support::get)
        .build());

    @Override
    public void onActivate() {
        boolean swapped = false;
        swapped = false;
    }

    @EventHandler
    private void onTick(TickEvent.Post event, boolean swapped) {
        if (mc.player == null || mc.world == null) return;

        PlayerEntity target = TargetUtils.getPlayerTarget(range.get(), priority.get());
        if (target == null || TargetUtils.isBadTarget(target, range.get())) return;

        BlockPos supportPosNorth = target.getBlockPos().north().down();
        BlockPos supportPosEast = target.getBlockPos().east().down();
        BlockPos supportPosSouth = target.getBlockPos().south().down();
        BlockPos supportPosWest = target.getBlockPos().west().down();

        BlockPos cityPosNorth = target.getBlockPos().north();
        BlockPos cityPosEast = target.getBlockPos().east();
        BlockPos cityPosSouth = target.getBlockPos().south();
        BlockPos cityPosWest = target.getBlockPos().west();
        FindItemResult pickaxe = InvUtils.findInHotbar(Items.NETHERITE_PICKAXE, Items.DIAMOND_PICKAXE);

        if (mc.world.getBlockState(cityPosNorth).getBlock() != Blocks.AIR && mc.world.getBlockState(cityPosNorth).getBlock() == Blocks.OBSIDIAN) {
            
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, cityPosNorth, Direction.DOWN));
            RenderUtils.renderTickingBlock(cityPosWest, color.get(), color.get(), ShapeMode.Both, 5, 5, true, true);
            if(support.get()) {
                BlockUtils.place(supportPosNorth, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);
            }
        }

        if (mc.world.getBlockState(cityPosEast).getBlock() != Blocks.AIR && mc.world.getBlockState(cityPosEast).getBlock() == Blocks.OBSIDIAN) {
            
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, cityPosEast, Direction.DOWN));
            RenderUtils.renderTickingBlock(cityPosWest, color.get(), color.get(), ShapeMode.Both, 5, 5, true, true);
            if(support.get()) {
                BlockUtils.place(supportPosEast, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);
            }
        }

        if (mc.world.getBlockState(cityPosSouth).getBlock() != Blocks.AIR && mc.world.getBlockState(cityPosSouth).getBlock() == Blocks.OBSIDIAN) {
            
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, cityPosSouth, Direction.DOWN));
            RenderUtils.renderTickingBlock(cityPosWest, color.get(), color.get(), ShapeMode.Both, 5, 5, true, true);
            if(support.get()) {
                BlockUtils.place(supportPosSouth, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);
            }
        }

        if (mc.world.getBlockState(cityPosWest).getBlock() != Blocks.AIR && mc.world.getBlockState(cityPosWest).getBlock() == Blocks.OBSIDIAN) {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, cityPosWest, Direction.DOWN));
            RenderUtils.renderTickingBlock(cityPosWest, color.get(), color.get(), ShapeMode.Both, 5, 5, true, true);
            if(support.get()) {
                BlockUtils.place(supportPosWest, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);
            }
        }
    }
}
