package com.example.addon.modules.combat;

import com.example.addon.Addon;
import com.example.addon.utils.PlayerUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class autoCity extends Module {

    public autoCity() {
        super(Addon.COMBAT, "Auto City+", "Auto city but better ");
    }

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

    private final Setting<SettingColor> supportColor = sgGeneral.add(new ColorSetting.Builder()
        .name("support-color")
        .description("The color for positions to be placed.")
        .defaultValue(new SettingColor(255, 0, 0, 75))
        .visible(support::get)
        .build());

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("color")
        .description("The color for positions cityed block")
        .defaultValue(new SettingColor(255, 0, 0, 75))
        .visible(support::get)
        .build());

    private final Setting<Boolean> onlyInHoles = sgGeneral.add(new BoolSetting.Builder()
        .name("only-in-holes")
        .description("Will not work right now but will soon.")
        .defaultValue(true)
        .build());

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        PlayerEntity target = TargetUtils.getPlayerTarget(range.get(), priority.get());
        if (target == null || TargetUtils.isBadTarget(target, range.get())) return;

        if (onlyInHoles.get() && !PlayerUtils.isSurrounded(target)) return;

        handleCity(target, rotate.get(), support.get(), color.get(), supportColor.get());
    }


    private void handleCity(PlayerEntity target, boolean rotate, boolean support, SettingColor color, SettingColor supportColor) {
        BlockPos cityPos = target.getBlockPos().offset(Direction.NORTH); // Adjust direction as needed

        if ((mc.world.getBlockState(cityPos).getBlock() == Blocks.OBSIDIAN) || (mc.world.getBlockState(cityPos).getBlock() == Blocks.BEDROCK)) { 
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, cityPos, Direction.DOWN));
            RenderUtils.renderTickingBlock(cityPos, color, color, ShapeMode.Both, 5, 5, true, true);
            if (support) {
                BlockPos supportPos = cityPos.down();
                BlockUtils.place(supportPos, InvUtils.findInHotbar(Items.OBSIDIAN), rotate, 0, true);
                RenderUtils.renderTickingBlock(supportPos, supportColor, supportColor, ShapeMode.Both, 5, 5, true, true);
            }
        }
    }
}
