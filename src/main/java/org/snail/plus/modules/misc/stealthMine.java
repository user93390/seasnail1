package org.snail.plus.modules.misc;

import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Vector3d;
import org.snail.plus.Addon;
import org.snail.plus.utils.WorldUtils;
import org.snail.plus.utils.swapUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class stealthMine extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgMisc = settings.createGroup("Misc");

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Automatically rotates towards the position where the block is being broken.")
            .defaultValue(true)
            .build());

    private final Setting<Double> Range = sgGeneral.add(new DoubleSetting.Builder()
            .name("packet mine range")
            .description("the range")
            .defaultValue(4.5)
            .sliderMin(1)
            .sliderMax(10)
            .build());

    private final Setting<Boolean> strictDirection = sgMisc.add(new BoolSetting.Builder()
            .name("strict direction")
            .description("Only breaks the block in the direction you are facing")
            .defaultValue(true)
            .build());

    private final Setting<breakMode> mineMode = sgGeneral.add(new EnumSetting.Builder<breakMode>()
            .name("Mine mode")
            .description("how blocks are mined")
            .defaultValue(breakMode.instant)
            .build());

    private final Setting<Boolean> PauseOnUse = sgGeneral.add(new BoolSetting.Builder()
            .name("Pause on use")
            .description("pauses the module when using another item")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> syncSlot = sgGeneral.add(new BoolSetting.Builder()
            .name("sync slot")
            .description("prevents desync")
            .defaultValue(true)
            .build());

    private final Setting<renderMode> RenderMode = sgRender.add(new EnumSetting.Builder<renderMode>()
            .name("render mode")
            .description("how blocks are rendered")
            .defaultValue(renderMode.VH)
            .build());

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side color")
            .description("Side color")
            .defaultValue(new SettingColor(255, 0, 0, 75))
            .visible(() -> RenderMode.get() != renderMode.gradient)
            .build());

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line color")
            .description("Line color")
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .visible(() -> RenderMode.get() != renderMode.gradient)
            .build());

    private final Setting<SettingColor> startColor = sgRender.add(new ColorSetting.Builder()
            .name("start color")
            .description("the start color for the block.")
            .defaultValue(new SettingColor(25, 252, 25, 150))
            .visible(() -> RenderMode.get() == renderMode.gradient)
            .build());

    private final Setting<SettingColor> endColor = sgRender.add(new ColorSetting.Builder()
            .name("end color")
            .description("The end color for the block.")
            .defaultValue(new SettingColor(255, 25, 25, 150))
            .visible(() -> RenderMode.get() == renderMode.gradient)
            .build());

    private final Setting<Double> Speed = sgRender.add(new DoubleSetting.Builder()
            .name("render speed")
            .description("how much speed to multiply to the render")
            .defaultValue(1.5)
            .sliderMin(0)
            .sliderMax(10)
            .visible(() -> RenderMode.get() == renderMode.VH)
            .build());

    private final Setting<Integer> fadeSpeed = sgRender.add(new IntSetting.Builder()
            .name("fade speed")
            .description("the fade speed")
            .defaultValue(10)
            .sliderMin(0)
            .sliderMax(100)
            .visible(() -> RenderMode.get() == renderMode.fade)
            .build());

    private final Setting<Boolean> shrink  = sgGeneral.add(new BoolSetting.Builder()
            .name("fade shrink")
            .description("shrinks when fading out")
            .defaultValue(true)
            .visible(() -> RenderMode.get() == renderMode.fade)
            .build());

    private final Setting<Boolean> nametags = sgRender.add(new BoolSetting.Builder()
            .name("show progress")
            .description("shows the progress when breaking using nametags")
            .defaultValue(true)
            .build());
    private final Setting<Double> scale = sgRender.add(new DoubleSetting.Builder()
            .name("nametag scale")
            .description("how big should the nametag be")
            .defaultValue(1.0)
            .visible(nametags::get)
            .build());
    private final Setting<Boolean> shadow = sgRender.add(new BoolSetting.Builder()
            .name("shadow")
            .description("shows a shadow")
            .defaultValue(true)
            .build());
    private final Setting<SettingColor> nametagColor = sgRender.add(new ColorSetting.Builder()
            .name("nametag color")
            .description("nametag color")
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .visible(nametags::get)
            .build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build());


    private final Setting<WorldUtils.DirectionMode> directionMode = sgGeneral.add(new EnumSetting.Builder<WorldUtils.DirectionMode>()
            .name("direction mode")
            .description("how to face the block")
            .defaultValue(WorldUtils.DirectionMode.Down)
            .build());
    private final Setting<swapUtils.swapMode> swapMode = sgGeneral.add(new EnumSetting.Builder<swapUtils.swapMode>()
            .name("swap mode")
            .description("how to swap items")
            .defaultValue(swapUtils.swapMode.silent)
            .build());

    private final Setting<Boolean> MultiTask = sgMisc.add(new BoolSetting.Builder()
            .name("multi-task")
            .description("allows you to use different items when the module is interacting / placing")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> pauseUse = sgMisc.add(new BoolSetting.Builder()
            .name("pause on use")
            .description("pauses the module when you use a item")
            .defaultValue(false)
            .visible(() -> !MultiTask.get())
            .build());

    private final Setting<WorldUtils.HandMode> hand = sgGeneral.add(new EnumSetting.Builder<WorldUtils.HandMode>()
            .name("hand mode")
            .description("the hand to swing when mining")
            .defaultValue(WorldUtils.HandMode.MainHand)
            .build());

    private double blockBreakingProgress;
    BlockState blockState;
    private FindItemResult bestSlot;


    public stealthMine() {
        super(Addon.Snail, "stealth mine", "Mines blocks using packets");
    }
    private final Vector3d vec3 = new Vector3d();
    private List<BlockPos> BlockPositions = new ArrayList<>();

    private final Color cSides = new Color();
    private final Color cLines = new Color();

    @Override
    public void onActivate() {
        BlockPositions = new ArrayList<>();
        blockBreakingProgress = 0;
    }

    @Override
    public void onDeactivate() {
        if (BlockPositions != null) {
            BlockPositions.clear();
        }
        blockBreakingProgress = 0;
    }

    @EventHandler
    public void onBlockBreak(StartBreakingBlockEvent event) {
        BlockPositions.add(event.blockPos);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        try {
            if (BlockPositions.isEmpty()) return;
            for (BlockPos pos : BlockPositions) {
                BlockState blockState = mc.world.getBlockState(pos);
                bestSlot = InvUtils.findFastestTool(blockState);
                int slot = bestSlot.slot();
                    blockBreakingProgress += BlockUtils.getBreakDelta(slot, blockState);
                    info("Block Breaking Progress: " + blockBreakingProgress);
                    if (blockBreakingProgress >= 0.9) {
                        InvUtils.swap(slot, true);
                        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.DOWN));
                        InvUtils.swapBack();
                    }
            }
        } catch (Exception e) {
            StackTraceElement[] stackTrace = e.getStackTrace();
            if (stackTrace.length > 0) {
                StackTraceElement element = stackTrace[0];
            }
        }
    }

    @EventHandler
    private void Render(Render3DEvent event) {
        if (BlockPositions.isEmpty()) return;
        for (BlockPos blockPos : BlockPositions) {
            switch (RenderMode.get()) {
                case VH -> {
                    this.blockState = Objects.requireNonNull(mc.world).getBlockState(blockPos);
                    double ShrinkFactor = 1d - blockBreakingProgress * Speed.get();
                    BlockState state = Objects.requireNonNull(mc.world).getBlockState(blockPos);
                    VoxelShape shape = state.getOutlineShape(mc.world, blockPos);
                    if (shape.isEmpty()) {
                        return;
                    }
                    Box orig = shape.getBoundingBox();
                    orig.shrink(
                            orig.getLengthX() * ShrinkFactor,
                            orig.getLengthY() * ShrinkFactor,
                            orig.getLengthZ() * ShrinkFactor
                    );
                    renderBlock(event, orig, ShrinkFactor);
                }
                case normal -> event.renderer.box(blockPos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                case fade -> {

                    RenderUtils.renderTickingBlock(
                            blockPos, sideColor.get(),
                            lineColor.get(), shapeMode.get(),
                            0, fadeSpeed.get(), true, false
                    );
                }
                case gradient -> {
                    Color c1Sides = startColor.get().copy().a(startColor.get().a / 2);
                    Color c2Sides = endColor.get().copy().a(endColor.get().a / 2);
                    Color c1Lines = startColor.get().copy().a(startColor.get().a / 2);
                    Color c2Lines = endColor.get().copy().a(endColor.get().a / 2);
                    cSides.set(
                            (int) Math.round(c1Sides.r + (c2Sides.r - c1Sides.r) * blockBreakingProgress),
                            (int) Math.round(c1Sides.g + (c2Sides.g - c1Sides.g) * blockBreakingProgress),
                            (int) Math.round(c1Sides.b + (c2Sides.b - c1Sides.b) * blockBreakingProgress),
                            (int) Math.round(c1Sides.a + (c2Sides.a - c1Sides.a) * blockBreakingProgress)
                    );

                    cLines.set(
                            (int) Math.round(c1Lines.r + (c2Lines.r - c1Lines.r) * blockBreakingProgress),
                            (int) Math.round(c1Lines.g + (c2Lines.g - c1Lines.g) * blockBreakingProgress),
                            (int) Math.round(c1Lines.b + (c2Lines.b - c1Lines.b) * blockBreakingProgress),
                            (int) Math.round(c1Lines.a + (c2Lines.a - c1Lines.a) * blockBreakingProgress)
                    );
                    event.renderer.box(blockPos, cSides, cLines, shapeMode.get(), 0);
                }
            }
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (BlockPositions.isEmpty()) return;
        for (BlockPos blockPos : BlockPositions) {
            if (nametags.get()) {
                if (blockPos.getY() == -1) return;
                vec3.set(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
                if (NametagUtils.to2D(vec3, scale.get())) {
                    NametagUtils.begin(vec3);
                    TextRenderer.get().begin(1, false, true);
                    String text = String.format("%.1f", blockBreakingProgress * 1.5);
                    double w = TextRenderer.get().getWidth(text) / 2;
                    TextRenderer.get().render(text, -w, 0, nametagColor.get(), shadow.get());

                    TextRenderer.get().end();
                    NametagUtils.end();
                }
            }
        }
    }

    private void renderBlock(Render3DEvent event, Box orig, double shrinkFactor) {
        for (BlockPos blockPos : BlockPositions) {
            Box box = orig.shrink(
                    orig.getLengthX() * shrinkFactor,
                    orig.getLengthY() * shrinkFactor,
                    orig.getLengthZ() * shrinkFactor
            );

            double xShrink = (orig.getLengthX() * shrinkFactor) / 2;
            double yShrink = (orig.getLengthY() * shrinkFactor) / 2;
            double zShrink = (orig.getLengthZ() * shrinkFactor) / 2;

            double x1 = blockPos.getX() + box.minX + xShrink;
            double y1 = blockPos.getY() + box.minY + yShrink;
            double z1 = blockPos.getZ() + box.minZ + zShrink;
            double x2 = blockPos.getX() + box.maxX + xShrink;
            double y2 = blockPos.getY() + box.maxY + yShrink;
            double z2 = blockPos.getZ() + box.maxZ + zShrink;

            event.renderer.box(x1, y1, z1, x2, y2, z2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }
    public enum SwapMode {
        silent,
        normal,
        Inventory,
    }

    public enum breakMode {
        bypass,
        normal,
        instant,
    }
    public enum renderMode {
        VH,
        normal,
        fade,
        gradient,
    }
}