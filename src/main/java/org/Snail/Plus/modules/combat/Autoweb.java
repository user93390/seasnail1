package org.Snail.Plus.modules.combat;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IBox;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.Snail.Plus.Addon;
import org.Snail.Plus.utils.CombatUtils;
import org.Snail.Plus.utils.TPSSyncUtil;

import java.util.Objects;

public class Autoweb extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("Range to target player(s)")
            .defaultValue(3.0)
            .sliderMax(10.0)
            .sliderMin(1.0)
            .build());
    private final Setting<Double> delay = sgGeneral.add(new DoubleSetting.Builder()
            .name("delay")
            .description("the delay")
            .defaultValue(0.500)
            .sliderMax(10.0)
            .sliderMin(0.0)
            .build());
    private final Setting<Boolean> TpsSync = sgGeneral.add(new BoolSetting.Builder()
            .name("tps sync")
            .description("syncs delay to current server tps")
            .defaultValue(false)
            .build());
    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
            .name("target-priority")
            .description("How to filter targets within range.")
            .defaultValue(SortPriority.LowestDistance)
            .build());
    private final Setting<SwapMode> SwapMethod = sgGeneral.add(new EnumSetting.Builder<SwapMode>()
            .name("swap method")
            .description("the swap method. Silent is more consistent while invswitch is more reliable ")
            .defaultValue(SwapMode.silent)
            .build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotation")
            .description("Rotates towards the block when placing.")
            .defaultValue(false)
            .build());
    private final Setting<Boolean> AutoDisable = sgGeneral.add(new BoolSetting.Builder()
            .name("auto disable")
            .description("auto disables the module after webbing the target")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> Double = sgGeneral.add(new BoolSetting.Builder()
            .name("double web")
            .description("places 2 webs instead of 1")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> strictDirection = sgGeneral.add(new BoolSetting.Builder()
            .name("strict direction")
            .description("Only places anchors in the direction you are facing. Will crash if you are falling")
            .defaultValue(false)
            .build());
    private final Setting<Boolean> smart = sgGeneral.add(new BoolSetting.Builder()
            .name("smart")
            .description("more calculations")
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
    private final Setting<Boolean> shrink = sgGeneral.add(new BoolSetting.Builder()
            .name("shrink")
            .description("shrinks the render")
            .defaultValue(true)
            .build());
    private final Setting<RenderMode> renderMode = sgGeneral.add(new EnumSetting.Builder<RenderMode>()
            .name("render mode")
            .description("render mode. Smooth is cool")
            .defaultValue(RenderMode.smooth)
            .build());
    private final Setting<Integer> rendertime = sgGeneral.add(new IntSetting.Builder()
            .name("render time")
            .description("render time")
            .defaultValue(3)
            .sliderMax(100)
            .sliderMin(1)
            .visible(() -> renderMode.get() == RenderMode.fading)
            .build());
    private final Setting<Integer> Smoothness = sgGeneral.add(new IntSetting.Builder()
            .name("smoothness")
            .description("the smoothness")
            .defaultValue(3)
            .sliderMax(100)
            .sliderMin(1)
            .visible(() -> renderMode.get() == RenderMode.smooth)
            .build());
    private final Setting<SwingMode> swingMode = sgGeneral.add(new EnumSetting.Builder<SwingMode>()
            .name("swing type")
            .description("swing type")
            .defaultValue(SwingMode.mainhand)
            .build());
    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build());
    PlayerEntity target;
    private long lastWebPlaceTime = 0;
    private boolean Placed;
    private BlockPos targetPos;
    private BlockPos DoubleWeb;
    private BlockPos currentPos;
    private Box renderBoxOne, renderBoxTwo;

    public Autoweb() {
        super(Addon.Snail, "Auto web+", "webs players but better");
    }

    @EventHandler
    public void OnTick(TickEvent.Pre event) {

        double SyncDelay = TpsSync.get() ? 1.0 / TPSSyncUtil.getCurrentTPS() : delay.get();
        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastWebPlaceTime) < delay.get() * 1000) return;
        lastWebPlaceTime = currentTime;
        target = TargetUtils.getPlayerTarget(range.get(), priority.get());
        if (TargetUtils.isBadTarget(target, range.get())) return;
        targetPos = target.getBlockPos();
        DoubleWeb = target.getBlockPos().up(1);

        if (smart.get() && CombatUtils.isBurrowed(target)) {
            return;
        }
        for (CardinalDirection dir : CardinalDirection.values()) {
            if (this.strictDirection.get()
                    && dir.toDirection() != Objects.requireNonNull(mc.player).getHorizontalFacing()
                    && dir.toDirection().getOpposite() != mc.player.getHorizontalFacing()) continue;
        }

        if (Double.get() && !smart.get()) {
            BlockUtils.place(targetPos, InvUtils.findInHotbar(Items.COBWEB), rotate.get(), 100, true);
            currentPos = targetPos;
            BlockUtils.place(DoubleWeb, InvUtils.findInHotbar(Items.COBWEB), rotate.get(), 100, true);
            currentPos = DoubleWeb;
            Placed = true;
        }
        if (Double.get() && smart.get()) {
            if(CombatUtils.isSurrounded(target)) {
                BlockUtils.place(targetPos, InvUtils.findInHotbar(Items.COBWEB), rotate.get(), 100, true);
                currentPos = targetPos;
                Placed = true;
            } else if(Double.get() && smart.get() && !CombatUtils.isSurrounded(target)) {
                BlockUtils.place(targetPos, InvUtils.findInHotbar(Items.COBWEB), rotate.get(), 100, true);
                currentPos = targetPos;
                BlockUtils.place(DoubleWeb, InvUtils.findInHotbar(Items.COBWEB), rotate.get(), 100, true);
                currentPos = DoubleWeb;
                Placed = true;
            }
        }
        if(!Double.get()) {
            BlockUtils.place(targetPos, InvUtils.findInHotbar(Items.COBWEB), rotate.get(), 100, true);
            currentPos = targetPos;
            Placed = true;
        }
        if (AutoDisable.get() && Placed) {
            toggle();
            return;
        }
        if (Placed) {
            switch (swingMode.get()) {
                case mainhand:
                    mc.player.swingHand(Hand.MAIN_HAND);
                    break;
                case offhand:
                    mc.player.swingHand(Hand.OFF_HAND);
                    break;
                case packet:
                    Objects.requireNonNull(mc.interactionManager).interactItem(mc.player, Hand.MAIN_HAND);
                case none:
                    break;
            }
        } else {
            return;
        }
    }

    @EventHandler
    public void AnchorRender(Render3DEvent event) {
        target = TargetUtils.getPlayerTarget(range.get(), priority.get());
        boolean ShrinkNow = shrink.get() && TargetUtils.isBadTarget(target, range.get()) || currentPos == null;
        if (TargetUtils.isBadTarget(target, range.get())) return;
        if (currentPos != null) {
            if (renderMode.get() == RenderMode.normal) {
                event.renderer.box(currentPos, sideColor.get(), lineColor.get(), shapeMode.get(), (int) 1.0f);
            } else if (renderMode.get() == RenderMode.fading) {
                RenderUtils.renderTickingBlock(
                        currentPos, sideColor.get(),
                        lineColor.get(), shapeMode.get(),
                        0, rendertime.get(), true, ShrinkNow
                );

            } else if (renderMode.get() == RenderMode.smooth) {
                if (renderBoxOne == null) renderBoxOne = new Box(currentPos);
                if (renderBoxTwo == null) renderBoxTwo = new Box(currentPos);


                ((IBox) renderBoxTwo).set(
                        currentPos.getX(), currentPos.getY(), currentPos.getZ(),
                        currentPos.getX() + 1, currentPos.getY() + 1, currentPos.getZ() + 1
                );


                double offsetX = (renderBoxTwo.minX - renderBoxOne.minX) / Smoothness.get();
                double offsetY = (renderBoxTwo.minY - renderBoxOne.minY) / Smoothness.get();
                double offsetZ = (renderBoxTwo.minZ - renderBoxOne.minZ) / Smoothness.get();


                ((IBox) renderBoxOne).set(
                        renderBoxOne.minX + offsetX,
                        renderBoxOne.minY + offsetY,
                        renderBoxOne.minZ + offsetZ,
                        renderBoxOne.maxX + offsetX,
                        renderBoxOne.maxY + offsetY,
                        renderBoxOne.maxZ + offsetZ
                );

                event.renderer.box(renderBoxOne, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            }
        }
    }

    public enum RenderMode {
        fading,
        normal,
        smooth
    }

    public enum SwingMode {
        offhand,
        mainhand,
        packet,
        none
    }

    public enum SwapMode {
        silent,
        invswitch,
        normal,
    }
}
