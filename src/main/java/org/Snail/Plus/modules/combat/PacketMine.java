package org.Snail.Plus.modules.combat;

import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.combat.Offhand;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationCalculator;
import org.Snail.Plus.Addon;
import org.Snail.Plus.utils.CombatUtils;

import java.util.Objects;

public class PacketMine extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
            .name("target-priority")
            .description("How to filter targets within range.")
            .defaultValue(SortPriority.LowestDistance)
            .build());

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("target-range")
            .description("The radius in which players get targeted.")
            .defaultValue(4)
            .min(0)
            .sliderMax(10)
            .build());

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Automatically rotates towards the position where the block is being broken.")
            .defaultValue(true)
            .build());

    private final Setting<Double> delay = sgGeneral.add(new DoubleSetting.Builder()
            .name("delay")
            .description("Delay in seconds between each block placement.")
            .defaultValue(1.0)
            .min(0)
            .sliderMax(5)
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

    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build());

    private BlockPos targetPos;
    private PlayerEntity target;
    public PacketMine() {
        super(Addon.Snail, "PacketMine+", "better/worse autocrystal bro");
    }

    private long lastPlaceTime = 0;

    private final BlockPos.Mutable blockPos = new BlockPos.Mutable(0, Integer.MIN_VALUE, 0);

    private Direction direction;

    private final Color cSides = new Color();
    private final Color cLines = new Color();

    @Override
    public void onActivate() {
        blockPos.set(0, -1, 0);
        int ticks = 0;
    }

    @EventHandler
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        direction = event.direction;
        blockPos.set(event.blockPos);
    }
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if(!Objects.requireNonNull(mc.world).getBlockState(blockPos).isAir()) {
            Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.DOWN));
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.DOWN));
            System.out.println("broken block");
        } else {
            return;
        }
    }
    private void renderBlock(Render3DEvent event, Box orig, BlockPos pos, double shrinkFactor, double progress) {
        Box box = orig.shrink(
                orig.getLengthX() * shrinkFactor,
                orig.getLengthY() * shrinkFactor,
                orig.getLengthZ() * shrinkFactor
        );

        double xShrink = (orig.getLengthX() * shrinkFactor) / 2;
        double yShrink = (orig.getLengthY() * shrinkFactor) / 2;
        double zShrink = (orig.getLengthZ() * shrinkFactor) / 2;

        double x1 = pos.getX() + box.minX + xShrink;
        double y1 = pos.getY() + box.minY + yShrink;
        double z1 = pos.getZ() + box.minZ + zShrink;
        double x2 = pos.getX() + box.maxX + xShrink;
        double y2 = pos.getY() + box.maxY + yShrink;
        double z2 = pos.getZ() + box.maxZ + zShrink;

        Color c1Sides = sideColor.get().copy().a(sideColor.get().a / 2);
        Color c2Sides = sideColor.get().copy().a(sideColor.get().a / 2);

        cSides.set(
                (int) Math.round(c1Sides.r + (c2Sides.r - c1Sides.r) * progress),
                (int) Math.round(c1Sides.g + (c2Sides.g - c1Sides.g) * progress),
                (int) Math.round(c1Sides.b + (c2Sides.b - c1Sides.b) * progress),
                (int) Math.round(c1Sides.a + (c2Sides.a - c1Sides.a) * progress)
        );

        Color c1Lines = sideColor.get();
        Color c2Lines = sideColor.get();

        cLines.set(
                (int) Math.round(c1Lines.r + (c2Lines.r - c1Lines.r) * progress),
                (int) Math.round(c1Lines.g + (c2Lines.g - c1Lines.g) * progress),
                (int) Math.round(c1Lines.b + (c2Lines.b - c1Lines.b) * progress),
                (int) Math.round(c1Lines.a + (c2Lines.a - c1Lines.a) * progress)
        );

        event.renderer.box(x1, y1, z1, x2, y2, z2, cSides, cLines, ShapeMode.Both, 0);
    }
}