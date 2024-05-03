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
            if (InvUtils.findInHotbar(Items.SAND, Items.RED_SAND) == null) {
                ChatUtils.error("no sand in hotbar... disabling");
                toggle();
                return;
            }

            BlockUtils.place(targetPos, InvUtils.findInHotbar(Items.SAND, Items.RED_SAND), rotate.get(), 0, false);
            RenderUtils.renderTickingBlock(targetPos, Color.CYAN, Color.CYAN, ShapeMode.Both, 5, 5, true, false);

            ChatUtils.sendMsg(Text.of(Formatting.GREEN + "Placing sand..."));

            if (autoDisable.get()) {
                this.toggle();
                ChatUtils.sendMsg(Text.of(Formatting.GREEN + "Auto-disabling because of auto-disable..."));
            }
        }
    }
}