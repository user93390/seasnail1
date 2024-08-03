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
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.Snail.Plus.Addon;
import org.Snail.Plus.utils.CombatUtils;
import org.Snail.Plus.utils.WorldUtils;

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
    private final Setting<Double> delay = sgGeneral.add(new DoubleSetting.Builder()
            .name("delay")
            .description("Delay in seconds between each block placement.")
            .defaultValue(1.0)
            .min(0)
            .sliderMax(5)
            .build());

    private final Setting<Boolean> strictDirection = sgGeneral.add(new BoolSetting.Builder()
            .name("strict direction")
            .description("uses strict direction")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> support = sgGeneral.add(new BoolSetting.Builder()
            .name("support")
            .description("Support (Obsidian)")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> autoDisable = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-disable")
            .description("Disables the module when you have placed the sand")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> onlySurrounded = sgGeneral.add(new BoolSetting.Builder()
            .name("only surrounded")
            .description("only targets players if they are surrounded")
            .defaultValue(true)
            .build());
    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder()
            .name("side color")
            .description("Side color")
            .defaultValue(new SettingColor(255, 0, 0, 75))
            .build());
    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
            .name("line color")
            .description("Line color")
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .build());


    private long lastPlaceTime = 0;
    private boolean sandPlaced;

    public AutoSand() {
        super(Addon.Snail, "auto-sand", "Places sand two blocks above players' heads");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        long time = System.currentTimeMillis();
        if ((time - lastPlaceTime) < delay.get() * 1000) return;
        lastPlaceTime = time;

        PlayerEntity target = TargetUtils.getPlayerTarget(range.get(), priority.get());
        if (TargetUtils.isBadTarget(target, range.get())) return;

        FindItemResult Support = InvUtils.findInHotbar(Items.OBSIDIAN);
        FindItemResult Blocks = InvUtils.findInHotbar(
                Items.SAND,
                Items.RED_SAND,
                Items.GRAVEL,
                Items.CYAN_CONCRETE_POWDER,
                Items.RED_CONCRETE_POWDER,
                Items.BLUE_CONCRETE_POWDER,
                Items.GRAY_CONCRETE_POWDER,
                Items.PURPLE_CONCRETE_POWDER,
                Items.PINK_CONCRETE_POWDER,
                Items.MAGENTA_CONCRETE_POWDER,
                Items.BLACK_CONCRETE_POWDER,
                Items.LIGHT_BLUE_CONCRETE_POWDER,
                Items.LIGHT_GRAY_CONCRETE_POWDER,
                Items.WHITE_CONCRETE_POWDER

        );
        BlockPos targetPos = target.getBlockPos().up(height.get());

        if (onlySurrounded.get() && CombatUtils.isSurrounded(target) && Objects.requireNonNull(mc.world).getBlockState(targetPos).isAir()) {

            for (Direction dir : Direction.values()) {
                if (strictDirection.get() && WorldUtils.strictDirection(targetPos.offset(dir), dir.getOpposite())) continue;
            }
            if (this.support.get()) {
                BlockPos supportPosNorth = target.getBlockPos().north(1);
                BlockPos supportPosNorthUpOne = target.getBlockPos().north(1).up(height.get());

                BlockUtils.place(supportPosNorth, Support, rotate.get(), 100, true);
                BlockUtils.place(supportPosNorthUpOne, Support, rotate.get(), 100, true);
            }

            BlockUtils.place(targetPos, Blocks, rotate.get(), 0, false);
            sandPlaced = true;
            if (this.autoDisable.get() && sandPlaced) {
                this.toggle();
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        PlayerEntity target = TargetUtils.getPlayerTarget(range.get(), priority.get());
        if (onlySurrounded.get() && !CombatUtils.isSurrounded(Objects.requireNonNull(target))) return;
        BlockPos supportPosNorth = Objects.requireNonNull(target).getBlockPos().north(1);
        BlockPos supportPosNorthUpOne = target.getBlockPos().north(1).up(1);
        BlockPos supportPosNorthUpTwo = target.getBlockPos().north(1).up(2);

        BlockPos targetPos = target.getBlockPos().up(height.get());
        event.renderer.box(targetPos, sideColor.get(), lineColor.get(), ShapeMode.Both, (int) 1.0f);
        if (support.get()) {
            event.renderer.box(supportPosNorth, sideColor.get(), lineColor.get(), ShapeMode.Both, 5);
            event.renderer.box(supportPosNorthUpOne, sideColor.get(), lineColor.get(), ShapeMode.Both, 5);
            event.renderer.box(supportPosNorthUpTwo, sideColor.get(), lineColor.get(), ShapeMode.Both, 5);
        }
    }
}
