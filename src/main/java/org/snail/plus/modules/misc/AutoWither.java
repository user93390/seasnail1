package org.snail.plus.modules.misc;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.BlockItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.snail.plus.Addon;
import org.snail.plus.utils.WorldUtils;
import org.snail.plus.utils.swapUtils;

import java.util.ArrayList;
import java.util.Arrays;



/**
 * Author: TurtleWithaBlock
 */

public class AutoWither extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final SettingGroup sgRender = settings.createGroup("Render");


    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("Delay")
        .description("How many ticks to wait in between placing blocks")
        .defaultValue(1)
        .sliderMin(0)
        .sliderMax(10)
        .build());

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("Rotate")
        .description("Rotate to place blocks")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> renderBlock = sgRender.add(new BoolSetting.Builder()
        .name("Render Block")
        .description("Render placed blocks")
        .defaultValue(true)
        .build());

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("Shape Mode")
        .description("Should it display the outline or fill")
        .defaultValue(ShapeMode.Both)
        .visible(() -> renderBlock.get())
        .build());

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("Side Color")
        .description("Color of the inside of the rendered block")
        .defaultValue(new SettingColor(0, 255, 255, 100))
        .visible(() -> renderBlock.get())
        .build());

    private final Setting<SettingColor> LineColor = sgRender.add(new ColorSetting.Builder()
        .name("Line Color")
        .description("Color of the outlines on the rendered block")
        .defaultValue(Color.CYAN)
        .visible(() -> renderBlock.get())
        .build());

    public AutoWither() {
        super(Addon.Snail, "Auto Wither", "Automatically builds a wither");
    }
    BlockPos currentBlockPos = null;
    int cooldown;

    ArrayList<BlockPos> soulSandOffsetsX = new ArrayList<>(Arrays.asList(
            new BlockPos(0, 0, 0),
            new BlockPos(0, 1, 0),
            new BlockPos(1, 1, 0),
            new BlockPos(-1, 1, 0)));

    ArrayList<BlockPos> soulSandOffsetsZ = new ArrayList<>(Arrays.asList(
            new BlockPos(0, 0, 0),
            new BlockPos(0, 1, 0),
            new BlockPos(0, 1, 1),
            new BlockPos(0, 1, -1)));

    ArrayList<BlockPos> witherSkullOffsetsX = new ArrayList<>(Arrays.asList(
            new BlockPos(0, 2, 0),
            new BlockPos(1, 2, 0),
            new BlockPos(-1, 2, 0)));

    ArrayList<BlockPos> witherSkullOffsetsZ = new ArrayList<>(Arrays.asList(
            new BlockPos(0, 2, 0),
            new BlockPos(0, 2, 1),
            new BlockPos(0, 2, -1)));

    private boolean canPlace(Block block, BlockPos pos) {
        return mc.world.canPlace(block.getDefaultState(), pos, ShapeContext.absent());
    }

    private void placeBlock(Direction placementSide, BlockPos blockPos, boolean rotate) {
        Vec3d hitPos = Vec3d.ofCenter(blockPos);
        hitPos=hitPos.add(new Vec3d(placementSide.getOffsetX() * 0.5, placementSide.getOffsetY() * 0.5, placementSide.getOffsetZ() * 0.5));
        if (rotate) {
            Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), 5);
        }

        BlockHitResult hitResult = new BlockHitResult(hitPos, placementSide.getOpposite(), blockPos.add(placementSide.getOffsetX(), placementSide.getOffsetY(), placementSide.getOffsetZ()), false);

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
    }

    private boolean attemptPlace(BlockPos target, FindItemResult block, Block type) {
        if (mc.world.getBlockState(target).isAir() && canPlace(type, target)) {

            InvUtils.swap(block.slot(), true);
            placeBlock(BlockUtils.getPlaceSide(target), target, rotate.get());
            InvUtils.swapBack();

            cooldown= delay.get();
            return delay.get() != 0;
        } else if (!mc.world.getBlockState(target).getBlock().equals(type)) {
            currentBlockPos=null;
            return true;
        }
        return false;
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (currentBlockPos!=null) {
            return;
        }

        if (!(event.packet instanceof PlayerInteractBlockC2SPacket p)) return;
        BlockPos placed = p.getBlockHitResult().getBlockPos().offset(p.getBlockHitResult().getSide());
        if (mc.world.getBlockState(placed).getBlock().equals(Blocks.SOUL_SAND)) {
            currentBlockPos = placed;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (cooldown>1) {
            cooldown-=1;
            return;
        }
        if (currentBlockPos!=null) {
            FindItemResult soulSand = InvUtils.findInHotbar(BlockItem.BLOCK_ITEMS.get(Blocks.SOUL_SAND));
            FindItemResult witherSkull = InvUtils.findInHotbar(BlockItem.BLOCK_ITEMS.get(Blocks.WITHER_SKELETON_SKULL));
            if (soulSand.found() && witherSkull.found()) {
                ArrayList<BlockPos> offsets = null;
                ArrayList<BlockPos> skullOffsets = null;
                if (Math.abs(mc.player.getBlockPos().getZ()-currentBlockPos.getZ())>Math.abs(mc.player.getBlockPos().getX()-currentBlockPos.getX())) {
                    offsets=soulSandOffsetsX;
                    skullOffsets=witherSkullOffsetsX;
                } else {
                    offsets=soulSandOffsetsZ;
                    skullOffsets=witherSkullOffsetsZ;
                }
                for (BlockPos offset : offsets) {
                    BlockPos target = currentBlockPos.add(offset);
                    if (attemptPlace(target, soulSand, Blocks.SOUL_SAND)) {return;}
                }
                for (BlockPos offset : skullOffsets) {
                    BlockPos target = currentBlockPos.add(offset);
                    if (attemptPlace(target, witherSkull, Blocks.WITHER_SKELETON_SKULL)) {return;}
                }
                currentBlockPos=null;
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (currentBlockPos != null && renderBlock.get()) {
            event.renderer.box(currentBlockPos, sideColor.get(), LineColor.get(), shapeMode.get(), 0);
        }
    }
}






