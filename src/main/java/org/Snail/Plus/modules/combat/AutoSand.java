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
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import org.Snail.Plus.Addon;

import java.util.Objects;

public class AutoSand extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("target-range")
            .description("The radius in which players get targeted.")
            .defaultValue(4)
            .min(0)
            .sliderMax(5)
            .build());

    private final Setting<Integer> height = sgGeneral.add(new IntSetting.Builder()
            .name("height")
            .description("height to place sand blocks")
            .defaultValue(2)
            .min(1)
            .sliderMax(5)
            .build());

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Automatically rotates towards the position where the sand is getting placed")
            .defaultValue(true)
            .build());

    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
            .name("target-priority")
            .description("How to filter targets within range.")
            .defaultValue(SortPriority.LowestDistance)
            .build());

    private final Setting<Boolean> support = sgGeneral.add(new BoolSetting.Builder()
            .name("support")
            .description("Support (Obsidian)")
            .defaultValue(false)
            .build());

    private static Setting<SettingColor> SideColor;
    private static Setting<SettingColor> LineColor;

    private final Setting<Double> delay = sgGeneral.add(new DoubleSetting.Builder()
            .name("delay")
            .description("Delay in seconds between each block placement.")
            .defaultValue(1.0)
            .min(0)
            .sliderMax(5)
            .build());

    private final Setting<Boolean> autoDisable = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-disable")
            .description("Disables the module when you have placed the sand")
            .defaultValue(true)
            .build());

    private long lastPlaceTime = 0;

    public AutoSand() {
        super(Addon.Snail, "auto-sand", "Places sand two blocks above players' heads");

        // Initialize static variables
        SideColor = sgGeneral.add(new ColorSetting.Builder()
                .name("Side Color")
                .description("side color")
                .defaultValue(new SettingColor(255, 0, 0, 80))
                .build());

        LineColor = sgGeneral.add(new ColorSetting.Builder()
                .name("Line color")
                .description("line color")
                .defaultValue(new SettingColor(255, 0, 0, 255))
                .build());
    }
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        long time = System.currentTimeMillis();

        if ((time - lastPlaceTime) < delay.get() * 1000) return;

        lastPlaceTime = time;
        PlayerEntity target = TargetUtils.getPlayerTarget(range.get(), priority.get());
        if(TargetUtils.isBadTarget(target, range.get())) return;

        FindItemResult Sand = InvUtils.findInHotbar(Items.SAND, Items.RED_SAND, Items.GRAVEL);
        FindItemResult Support = InvUtils.findInHotbar(Items.OBSIDIAN);
        BlockPos targetPos = target.getBlockPos().up(height.get());

        if (Objects.requireNonNull(mc.world).getBlockState(targetPos).getBlock().equals(Blocks.AIR) && Sand != null && target != null) {

            if (this.support.get()) {
                BlockPos supportPosNorth = target.getBlockPos().north(1);
                BlockPos supportPosNorthUpOne = target.getBlockPos().north(1).up(1);
                BlockPos supportPosNorthUpTwo = target.getBlockPos().north(1).up(2);

                BlockUtils.place(supportPosNorth, Support, rotate.get(), 0, false);
                BlockUtils.place(supportPosNorthUpOne, Support, rotate.get(), 0, true);
                BlockUtils.place(supportPosNorthUpTwo, Support, rotate.get(), 0, true);
            }

            BlockUtils.place(targetPos, InvUtils.findInHotbar(Items.SAND, Items.RED_SAND, Items.GRAVEL), rotate.get(), 0, false);

            boolean sandPlaced = BlockUtils.place(targetPos, InvUtils.findInHotbar(Items.ANVIL), rotate.get(), 0, false);
            RenderUtils.renderTickingBlock(targetPos, SideColor.get(), LineColor.get(), ShapeMode.Both, 5, 5, true, false);

            if (this.autoDisable.get() && sandPlaced) {
                this.toggle();
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        PlayerEntity target = TargetUtils.getPlayerTarget(range.get(), priority.get());
        if (target == null) return;

        BlockPos targetPos = target.getBlockPos().up(height.get());
        event.renderer.box(targetPos, SideColor.get(), LineColor.get(), ShapeMode.Both, (int) 1.0f);

        BlockPos supportPosNorth = target.getBlockPos().north(1);
        BlockPos supportPosNorthUpOne = target.getBlockPos().north(1).up(1);
        BlockPos supportPosNorthUpTwo = target.getBlockPos().north(1).up(2);

        event.renderer.box(supportPosNorth, SideColor.get(), LineColor.get(), ShapeMode.Both, 5);
        event.renderer.box(supportPosNorthUpOne, SideColor.get(), LineColor.get(), ShapeMode.Both, 5);
        event.renderer.box(supportPosNorthUpTwo, SideColor.get(), LineColor.get(), ShapeMode.Both, 5);
    }
}
