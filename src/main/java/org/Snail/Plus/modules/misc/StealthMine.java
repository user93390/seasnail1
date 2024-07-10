package org.Snail.Plus.modules.misc;

import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
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
import org.Snail.Plus.Addon;

import java.util.Objects;

public class StealthMine extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Automatically rotates towards the position where the block is being broken.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> instantMine = sgGeneral.add(new BoolSetting.Builder()
            .name("instant remine")
            .description("instantly remines the broken block")
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
    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build());
    private double blockBreakingProgress;
    private long lastPlaceTime = 0;
    private FindItemResult item;
    private static final BlockPos.Mutable blockPos = new BlockPos.Mutable(0, Integer.MIN_VALUE, 0);
    private static Direction direction;

    private final Color cSides = new Color();
    private final Color cLines = new Color();
    BlockState blockState;
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
        public StealthMine() {
            super(Addon.Snail, "stealth mine+", "Mines blocks using pakets");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        item = InvUtils.findInHotbar(Items.NETHERITE_PICKAXE, Items.DIAMOND_PICKAXE);
        if (Objects.requireNonNull(mc.world).getBlockState(blockPos).getBlock() == Blocks.BEDROCK) {
            return;
        }
        if(!Objects.requireNonNull(mc.world).getBlockState(blockPos).isAir()) {
            int slot = Objects.requireNonNull(mc.player).getInventory().selectedSlot;
            InvUtils.move().fromId(slot).toId(item.slot());
            if (rotate.get()) Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), 100);
            blockBreakingProgress += BlockUtils.getBreakDelta(Objects.requireNonNull(mc.player).getInventory().selectedSlot, blockState) * 2;
            if(instantMine.get()) {
                Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction));
            } else if(!instantMine.get()) {
                Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction));
                Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction));
            }
        } else if (Objects.requireNonNull(mc.world).getBlockState(blockPos).isAir()) {
            blockBreakingProgress = 0;
        }
    }

    @EventHandler
    private void CityRender(Render3DEvent event) {
        if (blockBreakingProgress > 2) {
            blockBreakingProgress = 0;
        }
        this.blockState = Objects.requireNonNull(mc.world).getBlockState(blockPos);
        double ShrinkFactor = 1d - blockBreakingProgress;
        BlockState state = Objects.requireNonNull(mc.world).getBlockState(blockPos);
        VoxelShape shape = state.getOutlineShape(mc.world, blockPos);
        if (shape.isEmpty()) {
            return;

        }
        Box orig = shape.getBoundingBox();

        Box box = orig.shrink(
                orig.getLengthX() * ShrinkFactor,
                orig.getLengthY() * ShrinkFactor,
                orig.getLengthZ() * ShrinkFactor
        );

        renderBlock(event, orig, blockPos, ShrinkFactor, blockBreakingProgress);
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

        event.renderer.box(x1, y1, z1, x2, y2, z2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }
}
