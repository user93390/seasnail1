package org.snail.plus.modules.misc;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.snail.plus.Addon;
import org.snail.plus.utils.SwapUtils;
import org.snail.plus.utils.WorldUtils;

import java.util.Objects;

public class StealthMine extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgMisc = settings.createGroup("Misc");

    private final Setting<Double> Range = sgGeneral.add(new DoubleSetting.Builder()
            .name("packet mine range")
            .description("the range")
            .defaultValue(4.5)
            .sliderMin(1)
            .sliderMax(10)
            .build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Automatically rotates towards the position where the block is being broken.")
            .defaultValue(true)
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
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side color")
            .description("Side color")
            .defaultValue(new SettingColor(255, 0, 0, 75))
            .build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line color")
            .description("Line color")
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .build());
    private final Setting<breakMode > mineMode = sgRender.add(new EnumSetting.Builder<breakMode >()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(breakMode.instant)
            .build());

    private final Setting<Double> Speed = sgGeneral.add(new DoubleSetting.Builder()
            .name("render speed")
            .description("how much speed to multiply to the render")
            .defaultValue(1.5)
            .sliderMin(0)
            .sliderMax(10)
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
            .description("swings you're hand when mining")
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

    private double blockBreakingProgress;
    public static final BlockPos.Mutable blockPos = new BlockPos.Mutable(0, Integer.MIN_VALUE, 0);
    private static Direction direction;
    BlockState blockState;
    private FindItemResult bestSlot;


    public StealthMine() {
        super(Addon.Snail, "stealth mine+", "Mines blocks using packets");
    }

    @EventHandler
    public static void BreakBlock(StartBreakingBlockEvent event) {
        direction = event.direction;
        blockPos.set(event.blockPos);
    }

    @Override
    public void onActivate() {
        blockPos.set(0, -1, 0);
        blockBreakingProgress = 0;
    }

    @Override
    public void onDeactivate() {
        blockPos.set(0, -1, 0);
        blockBreakingProgress = 0;
        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, blockPos, Direction.UP));
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.world == null || mc.player == null) return;

        BlockState blockState = mc.world.getBlockState(blockPos);
        bestSlot = InvUtils.findFastestTool(blockState);

        // Check if there's a block to break and if it's within range
        if (!BlockUtils.canBreak(blockPos, blockState) || !blockPos.isWithinDistance(blockPos, Range.get()) || WorldUtils.isAir(blockPos)) {
            // If no block to break, abort any previous breaking process
            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, blockPos, Direction.UP));
            return;
        }


        // Swap to the pickaxe slot
        switch (swapMode.get()) {
            case silent:
            if (!bestSlot.found() || mc.player.getInventory().selectedSlot == bestSlot.slot()) return;
              mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(bestSlot.slot()));
             break;
            case normal:
                // Use InvUtils.swap for normal swapping
                InvUtils.swap(bestSlot.slot(), false);
                break;
        }

        // Sync slot if enabled
        if (syncSlot.get()) {
            syncSlot();
        }
        switch (mineMode.get()) {
            case normal:
                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));

            case instant:
                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));
        }

        if (WorldUtils.isAir(blockPos)) {
            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, blockPos, Direction.UP));
        }
        blockBreakingProgress += BlockUtils.getBreakDelta(bestSlot.slot(), blockState);
    }

    public void syncSlot() {
        Objects.requireNonNull(mc.player).getInventory().selectedSlot = mc.player.getInventory().selectedSlot;
        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
    }

    @EventHandler
    private void Render(Render3DEvent event) {
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

    private void renderBlock(Render3DEvent event, Box orig, double shrinkFactor) {
        Box box = orig.shrink(
                orig.getLengthX() * shrinkFactor,
                orig.getLengthY() * shrinkFactor,
                orig.getLengthZ() * shrinkFactor
        );

        double xShrink = (orig.getLengthX() * shrinkFactor) / 2;
        double yShrink = (orig.getLengthY() * shrinkFactor) / 2;
        double zShrink = (orig.getLengthZ() * shrinkFactor) / 2;

        double x1 = StealthMine.blockPos.getX() + box.minX + xShrink;
        double y1 = StealthMine.blockPos.getY() + box.minY + yShrink;
        double z1 = StealthMine.blockPos.getZ() + box.minZ + zShrink;
        double x2 = StealthMine.blockPos.getX() + box.maxX + xShrink;
        double y2 = StealthMine.blockPos.getY() + box.maxY + yShrink;
        double z2 = StealthMine.blockPos.getZ() + box.maxZ + zShrink;

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
}