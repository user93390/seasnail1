package org.snail.plus.modules.misc;

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
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import org.snail.plus.Addon;
import org.snail.plus.utils.SwapUtils;
import org.snail.plus.utils.WorldUtils;

import java.util.Objects;

import static net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK;

public class StealthMine extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
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
    private final Setting<Boolean> remine = sgGeneral.add(new BoolSetting.Builder()
            .name("remine")
            .description("remines the broken block normally")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> instantMine = sgGeneral.add(new BoolSetting.Builder()
            .name("instant remine")
            .description("instantly remines the broken block")
            .defaultValue(true)
            .visible(remine::get)
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
    
    private final Setting<Double> Speed = sgGeneral.add(new DoubleSetting.Builder()
            .name("render speed")
            .description("how much speed to multiply to the render")
            .defaultValue(1.5)
            .sliderMin(0)
            .sliderMax(10)
            .build());
    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build());
    private double blockBreakingProgress;
   public static final BlockPos.Mutable blockPos = new BlockPos.Mutable(0, Integer.MIN_VALUE, 0);
    private static Direction direction;
    BlockState blockState;

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
        shouldSwap = false;
        swapped = false;
    }

    @Override
    public void onDeactivate() {
        blockPos.set(0, -1, 0);
        blockBreakingProgress = 0;
        shouldSwap = false;
        swapped = false;
    }

    private FindItemResult pickaxeSlot;
    private  boolean shouldSwap;
    private  boolean swapped;
    int selectedSlot;
    private long lastPlaceTime = 0;
    @EventHandler
    public void onTick(TickEvent.Pre event) {
        try {
            pickaxeSlot = InvUtils.findInHotbar(Items.NETHERITE_PICKAXE, Items.DIAMOND_PICKAXE);
            selectedSlot = Objects.requireNonNull(mc.player).getInventory().selectedSlot;
            if(WorldUtils.isAir(blockPos)) {
                blockBreakingProgress = 0;
                return;
            }
            if(shouldSwap) {
                SwapUtils.SilentSwap(pickaxeSlot.slot(), 0.1);
            }
            if(swapped) {
                if(WorldUtils.isAir(blockPos)) {
                    blockBreakingProgress = 0;
                    return;
                }
                if(shouldSwap) {
                    InvUtils.swapBack();
                }
            }
            BlockState blockState = Objects.requireNonNull(mc.world).getBlockState(blockPos);
            if (blockState.isAir() || blockState.getBlock() == Blocks.BEDROCK || PauseOnUse.get() && (Objects.requireNonNull(mc.interactionManager).isBreakingBlock() || mc.player.isUsingItem())) {
                return;
            }
            if (BlockUtils.canBreak(blockPos) && blockPos.isWithinDistance(Objects.requireNonNull(mc.player).getBlockPos(), Range.get())) {
                BreakAndSwitch(blockPos);
                blockBreakingProgress += BlockUtils.getBreakDelta(pickaxeSlot.slot(), blockState);
                if(blockBreakingProgress > 1) blockBreakingProgress = 0;
            }
        } catch (Exception e) {
            System.out.println("Exception caught -> " + e.getCause() + ". Message -> " + e.getMessage());
        }
    }

    public void BreakAndSwitch(BlockPos pos) {
        pickaxeSlot = InvUtils.findInHotbar(Items.NETHERITE_PICKAXE, Items.DIAMOND_PICKAXE);
        Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(StealthMine.blockPos), 100, () -> {
            if(blockBreakingProgress > 0.9 && blockBreakingProgress < 1){
                shouldSwap = true;
                Objects.requireNonNull(this.mc.player).networkHandler.sendPacket(new PlayerActionC2SPacket(STOP_DESTROY_BLOCK, pos, direction));
                swapped = true;
            }
        });
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

    @Override
    public String getInfoString() {
        if (blockPos.getY() == -1) return null;
        return " %s".formatted(blockBreakingProgress);
    }
}