package org.snail.plus.modules.misc;

import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Vector3d;
import org.snail.plus.Addon;
import org.snail.plus.utils.CombatUtils;
import org.snail.plus.utils.WorldUtils;

import java.util.Objects;


public class stealthMine extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgMisc = settings.createGroup("Misc");
    private final SettingGroup sgAutoCity = settings.createGroup("Auto City");

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
    private final Setting<Boolean> MultiTask = sgMisc.add(new BoolSetting.Builder()
            .name("multi-task")
            .description("allows you to use different items when the module is interacting / placing")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> swingHand = sgMisc.add(new BoolSetting.Builder()
            .name("swing hand")
            .description("swings your hand when mining")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> pauseUse = sgMisc.add(new BoolSetting.Builder()
            .name("pause on use")
            .description("pauses the module when you use a item")
            .defaultValue(false)
            .visible(() -> !MultiTask.get())
            .build());

    private final Setting<SwapMode> swapMode = sgGeneral.add(new EnumSetting.Builder<SwapMode>()
            .name("swap mode")
            .description("how to swap")
            .defaultValue(SwapMode.silent)
            .build());

    private final Setting<Boolean> Autocity = sgAutoCity.add(new BoolSetting.Builder()
            .name("auto city")
            .description("Mines targets surrounds")
            .defaultValue(false)
            .build());
    private final Setting<Double> cityRange = sgAutoCity.add(new DoubleSetting.Builder()
            .name("City range")
            .description("auto city range")
            .visible(Autocity::get)
            .defaultValue(4.5)
            .build());
    private final Setting<Boolean> Burrow = sgAutoCity.add(new BoolSetting.Builder()
            .name("Anti Burrow")
            .description("mines players burrows")
            .visible(Autocity::get)
            .defaultValue(true)
            .build());
    private final Setting<Boolean> chatInfo = sgAutoCity.add(new BoolSetting.Builder()
            .name("chat info")
            .description("sends chat info about the module")
            .defaultValue(false)
            .visible(Autocity::get)
            .build());
    private final Setting<Boolean> onlySurrounded = sgAutoCity.add(new BoolSetting.Builder()
            .name("only surrounded")
            .description("only targets players in there surround")
            .defaultValue(false)
            .visible(Autocity::get)
            .build());
    private final Setting<Keybind> CityBind = sgAutoCity.add(new KeybindSetting.Builder()
            .name("bind")
            .description("Starts auto city when this button is pressed.")
            .defaultValue(Keybind.none())
            .visible(Autocity::get)
            .build());
    private final Setting<Boolean> supportPlace = sgAutoCity.add(new BoolSetting.Builder()
            .name("support place")
            .description("places support blocks if needed")
            .defaultValue(false)
            .visible(Autocity::get)
            .build());
    private BlockPos currentPos = BlockPos.ORIGIN;

    private double blockBreakingProgress;
    public static BlockPos.Mutable blockPos = new BlockPos.Mutable(0, Integer.MIN_VALUE, 0);
    BlockState blockState;
    private FindItemResult bestSlot;


    public stealthMine() {
        super(Addon.Snail, "stealth mine", "Mines blocks using packets");
    }

    @EventHandler
    public static void BreakBlock(StartBreakingBlockEvent event) {
        blockPos.set(event.blockPos);
    }

    @Override
    public void onActivate() {
        blockPos.set(0, -1, 0);
        blockBreakingProgress = 0;
    }

    @Override
    public void onDeactivate() {
        blockPos.set(0, -1.7, 0);
        blockBreakingProgress = 0;
        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, blockPos, Direction.UP));
    }

    public void syncSlot() {
        Objects.requireNonNull(mc.player).getInventory().selectedSlot = mc.player.getInventory().selectedSlot;
        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
    }

    private final Color cSides = new Color();
    private final Color cLines = new Color();
    @EventHandler
    private void Render(Render3DEvent event) {
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
                currentPos = blockPos;
                boolean shouldShrink = shrink.get() && (blockPos.getX() != currentPos.getX() || blockPos.getY() != currentPos.getY() || blockPos.getZ() != currentPos.getZ());

                RenderUtils.renderTickingBlock(
                        blockPos, sideColor.get(),
                        lineColor.get(), shapeMode.get(),
                        0, fadeSpeed.get(), true, shouldShrink
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
    private final Vector3d vec3 = new Vector3d();
    @EventHandler
    private void onRender2D(Render2DEvent event) {
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

    public BlockPos autoCity(PlayerEntity entity) {
        if (onlySurrounded.get() && !CombatUtils.isSurrounded(entity)) return null;

        BlockPos[] positions = {
                entity.getBlockPos().east(1),
                entity.getBlockPos().west(1),
                entity.getBlockPos().south(1),
                entity.getBlockPos().north(1)
        };

        for (BlockPos pos : positions) {
            if (WorldUtils.isAir(pos) || !WorldUtils.isBreakable(pos)) continue;

            if (supportPlace.get()) {
                InvUtils.swap(InvUtils.findInHotbar(Items.OBSIDIAN).slot(), true);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(pos.getX(), pos.getY() - 1, pos.getZ()), Direction.UP, pos, false));
                InvUtils.swapBack();
            }
            BreakBlock(StartBreakingBlockEvent.get(pos, Direction.UP));
            Switch();
            return pos;
        }
            return null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.world == null || mc.player == null || pauseUse.get() && mc.player.isUsingItem()) {
            return;
        }
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (Autocity.get() && player != null ) {
                if (CityBind.get().isPressed()) {
                        BlockPos newPos = autoCity(player);
                        if (newPos != null) {
                            blockPos.set(newPos);
                            if (chatInfo.get()) {
                                info("Auto City: Block at " + newPos + " mined");
                            }
                    }
                }
            }
        }
        BlockState blockState = mc.world.getBlockState(blockPos);
        bestSlot = InvUtils.findFastestTool(blockState);

        for (Direction dir : Direction.values()) {
            if (strictDirection.get() && WorldUtils.strictDirection(blockPos.offset(dir), dir.getOpposite())) {
                break;
            }
        }
        if (syncSlot.get()) {
            syncSlot();
        }
        if (BlockUtils.canBreak(blockPos, blockState) || blockPos.isWithinDistance(blockPos, Range.get()) || !WorldUtils.isAir(blockPos)) {
            breakBlock(blockPos);
            Switch();
            if (WorldUtils.isAir(blockPos)) {
                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, blockPos, Direction.UP));
            }

            if (bestSlot.found()) {
                blockBreakingProgress += BlockUtils.getBreakDelta(bestSlot.slot(), blockState);
                if (blockBreakingProgress >= 1) blockBreakingProgress = 1;
            }
            if(WorldUtils.isBreakable(blockPos)) blockBreakingProgress = 0;
        }
    }


    public void breakBlock(BlockPos blockPos) {
        switch (mineMode.get()) {
            case normal:
                if(rotate.get()) Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos));
                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));
                if(swingHand.get()) mc.player.swingHand(Hand.MAIN_HAND);
                break;
            case instant:
                if(rotate.get()) Rotations.rotate(Rotations.getYaw(blockPos),(Rotations.getPitch(blockPos)));
                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));
                if(swingHand.get()) mc.player.swingHand(Hand.MAIN_HAND);
                break;
            case bypass:
                if(rotate.get()) Rotations.rotate(Rotations.getYaw(blockPos),(Rotations.getPitch(blockPos)));
                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, blockPos, Direction.UP));
                if(swingHand.get()) mc.player.swingHand(Hand.MAIN_HAND);
                break;
        }
    }

    public void Switch() {
        switch (swapMode.get()) {
            case silent:
                if (!bestSlot.found() || mc.player.getInventory().selectedSlot == bestSlot.slot()) {
                    break;
                }
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(bestSlot.slot()));
                break;
            case normal:
                if (!bestSlot.found() || mc.player.getInventory().selectedSlot == bestSlot.slot()) {
                    break;
                }
                InvUtils.swap(bestSlot.slot(), false);
                break;
        }
    }

    private void renderBlock(Render3DEvent event, Box orig, double shrinkFactor) {
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

    public enum SwapMode {
        silent,
        normal,
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