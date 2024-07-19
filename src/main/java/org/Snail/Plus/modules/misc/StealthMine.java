package org.Snail.Plus.modules.misc;

import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import org.Snail.Plus.Addon;

import java.util.Arrays;
import java.util.Objects;

public class StealthMine extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> Range = sgGeneral.add(new DoubleSetting.Builder()
            .name("packetmine range")
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
    private final Setting<Boolean> Remine = sgGeneral.add(new BoolSetting.Builder()
            .name("remine")
            .description("remines the broken block normally")
            .defaultValue(true)
            .build());
    private final Setting<Integer> MaxNormal = sgGeneral.add(new IntSetting.Builder()
            .name("remine attempts")
            .description("how many normal remine attempts")
            .defaultValue(5)
            .sliderMin(1)
            .sliderMax(10)
            .visible(Remine::get)
            .build());
    private final Setting<Boolean> instantMine = sgGeneral.add(new BoolSetting.Builder()
            .name("instant remine")
            .description("instantly remines the broken block")
            .defaultValue(true)
            .build());
    private final Setting<Integer> MaxInstant = sgGeneral.add(new IntSetting.Builder()
            .name("instant remine attempts")
            .description("how many instant remine attempts")
            .defaultValue(5)
            .sliderMin(1)
            .sliderMax(10)
            .visible(instantMine::get)
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
    private static final BlockPos.Mutable blockPos = new BlockPos.Mutable(0, Integer.MIN_VALUE, 0);
    private static Direction direction;
    private int InstantMinedTimes;
    private int NormalMinedTimes;
    private int InstantMax = MaxInstant.get();
    private int NormalMax;
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
        reset();
    }

    @Override
    public void onDeactivate() {
        reset();
    }

    private void reset() {
        blockPos.set(0, -1, 0);
        blockBreakingProgress = 0;
        InstantMinedTimes = 0;
        NormalMinedTimes = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        try {
            NormalMax = MaxNormal.get();
            InstantMax = MaxInstant.get();

            // Check if block at blockPos is air and deactivate the module if true
            if (Objects.requireNonNull(mc.world).getBlockState(blockPos).isAir()) {
                reset();
                return;
            }

            if (BlockUtils.canBreak(blockPos) && blockPos.isWithinDistance(Objects.requireNonNull(mc.player).getBlockPos(), Range.get())) {
                if (PauseOnUse.get() && (Objects.requireNonNull(mc.interactionManager).isBreakingBlock() || mc.player.isUsingItem())) {
                    return;
                }

                int pickaxeSlot = -1;
                for (int i = 0; i < mc.player.getInventory().size(); i++) {
                    ItemStack stack = mc.player.getInventory().getStack(i);
                    if (stack.getItem() instanceof PickaxeItem) {
                        pickaxeSlot = i;
                        break;
                    }
                }

                if (rotate.get()) {
                    Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), 100);
                }

                BlockState blockState = mc.world.getBlockState(blockPos);

                // Check if the block is valid
                if (blockState.isAir() || blockState.getBlock() == Blocks.BEDROCK) {
                    return;
                }

                if (instantMine.get()) {
                    if (InstantMinedTimes < InstantMax && isMiningBlock(blockPos)) {
                        if (pickaxeSlot != -1) {
                            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(pickaxeSlot));
                            System.out.println("Swapping to pickaxe slot: " + pickaxeSlot);
                        }
                        Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction));
                        System.out.println("Sending STOP_DESTROY_BLOCK packet for instant mine");
                        handleBlockBroken();
                    }
                } else {
                    if (NormalMinedTimes < NormalMax && isMiningBlock(blockPos)) {
                        if (pickaxeSlot != -1) {
                            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(pickaxeSlot));
                            System.out.println("Swapping to pickaxe slot: " + pickaxeSlot);
                        }
                        Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction));
                        System.out.println("Sending START_DESTROY_BLOCK packet");
                        Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction));
                        System.out.println("Sending STOP_DESTROY_BLOCK packet for normal mine");
                        handleBlockBroken();
                    }
                }

                blockBreakingProgress += BlockUtils.getBreakDelta(pickaxeSlot, blockState) * 1.5;
            }
        } catch (Exception e) {
            System.out.println(Arrays.toString(e.getStackTrace()) + " in StealthMine.java");
        }
    }

    private void handleBlockBroken() {
        if (Objects.requireNonNull(mc.world).getBlockState(blockPos).isAir()) {
            if (instantMine.get()) {
                InstantMinedTimes++;
                System.out.println("Block broken using instant mine, total attempts: " + InstantMinedTimes);
            } else {
                NormalMinedTimes++;
                System.out.println("Block broken using normal mine, total attempts: " + NormalMinedTimes);
            }
            reset(); // Reset the module after a block is broken
        }
    }

    public boolean isMiningBlock(BlockPos pos) {
        return blockPos.equals(pos);
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