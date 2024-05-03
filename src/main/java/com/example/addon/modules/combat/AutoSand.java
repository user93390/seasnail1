package com.example.addon.modules.combat;

import com.example.addon.Addon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import meteordevelopment.meteorclient.utils.player.ChatUtils;

public class AutoSand extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("target-range")
            .description("The radius in which players get targeted.")
            .defaultValue(4)
            .min(0)
            .sliderMax(5)
            .build()
    );

    private final Setting<Integer> height = sgGeneral.add(new IntSetting.Builder()
            .name("height")
            .description("height to place sand blocks")
            .defaultValue(2)
            .min(1)
            .sliderMax(5)
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Automatically rotates towards the position where the sand is getting placed")
            .defaultValue(true)
            .build()
    );

    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
            .name("target-priority")
            .description("How to filter targets within range.")
            .defaultValue(SortPriority.LowestDistance)
            .build()
    );
    private final Setting<Boolean> support = sgGeneral.add(new BoolSetting.Builder()
            .name("support")
            .description("Support (Obsidian)")
            .defaultValue(false)
            .build()
    );
    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
            .name("Color")
            .description("The Color for positions to be placed.")
            .defaultValue(new SettingColor(255, 0, 0, 75))
            .build());

    private final Setting<Double> delay = sgGeneral.add(new DoubleSetting.Builder()
            .name("delay")
            .description("Delay in seconds between each block placement.")
            .defaultValue(1.0)
            .min(0)
            .sliderMax(5)
            .build()
    );

    private final Setting<Boolean> autoDisable = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-disable")
            .description("Disables the module when you have placed the sand")
            .defaultValue(true)
            .build()
    );

    private long lastPlaceTime = 0;

    public AutoSand() {
        super(Addon.COMBAT, "auto-sand", "Places sand two blocks above players' heads");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        long time = System.currentTimeMillis();

        if ((time - lastPlaceTime) < delay.get() * 1000) return;

        lastPlaceTime = time;
        PlayerEntity target = TargetUtils.getPlayerTarget(range.get(), priority.get());
        if (target == null || TargetUtils.isBadTarget(target, range.get())) return;

        BlockPos targetPos = target.getBlockPos().up(height.get());

        if (mc.world.getBlockState(targetPos).getBlock().equals(Blocks.AIR)) {
            if (InvUtils.findInHotbar(Items.SAND, Items.RED_SAND, Items.GRAVEL) == null) {
                ChatUtils.error("no sand in hotbar... disabling");
                toggle();
                return;
            }

            if (support.get())
            {
                BlockPos supportPosNorth = target.getBlockPos().north(1);
                BlockPos supportPosNorthUpOne = target.getBlockPos().north(1).up(1);
                BlockPos supportPosNorthUpTwo = target.getBlockPos().north(1).up(2);
                BlockPos supportPosEast = target.getBlockPos().east(1);
                BlockPos supportPosSouth = target.getBlockPos().south(1);
                BlockPos supportPosWest = target.getBlockPos().west(1);

                BlockUtils.place(supportPosNorth, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, false);
                BlockUtils.place(supportPosNorthUpOne, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, false);
                BlockUtils.place(supportPosNorthUpTwo, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, false);
                BlockUtils.place(supportPosEast, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, false);
                BlockUtils.place(supportPosSouth, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, false);
                BlockUtils.place(supportPosWest, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, false);

                RenderUtils.renderTickingBlock(supportPosNorth, color.get(), color.get(), ShapeMode.Both, 5, 10, true, false);
                RenderUtils.renderTickingBlock(supportPosNorthUpOne, color.get(), color.get(), ShapeMode.Both, 5, 10, true, false);
                RenderUtils.renderTickingBlock(supportPosNorthUpTwo, color.get(), color.get(), ShapeMode.Both,5 , 10, true, false);
                RenderUtils.renderTickingBlock(supportPosEast, color.get(), color.get(), ShapeMode.Both, 5, 10, true, false);
                RenderUtils.renderTickingBlock(supportPosSouth, color.get(), color.get(), ShapeMode.Both, 5, 10, true, false);
                RenderUtils.renderTickingBlock(supportPosWest, color.get(), color.get(), ShapeMode.Both, 5, 10, true, false);


            }

            BlockUtils.place(targetPos, InvUtils.findInHotbar(Items.SAND, Items.RED_SAND, Items.GRAVEL), rotate.get(), 0, false);
            RenderUtils.renderTickingBlock(targetPos, color.get(), color.get(), ShapeMode.Both, 5, 5, true, false);

            ChatUtils.sendMsg(Text.of(Formatting.GREEN + "Placing sand..."));



            if (autoDisable.get()) {
                this.toggle();
                ChatUtils.sendMsg(Text.of(Formatting.GREEN + "Auto-disabling because of auto-disable..."));
            }
        }
    }
}