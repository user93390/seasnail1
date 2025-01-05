package org.snail.plus.modules.combat;

import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import org.snail.plus.Addon;

public class packetMine extends Module {

    private static BlockPos position;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> instant = sgGeneral.add(new BoolSetting.Builder()
            .name("instant")
            .description("Instantly mines blocks.")
            .defaultValue(false)
            .build());
    private final Runnable sendPackets = () -> {
        if (!instant.get()) {
            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, position, Direction.DOWN));
        }
        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, position, Direction.DOWN));
    };
    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-switch")
            .description("Automatically switches to the best tool in your hotbar.")
            .defaultValue(true)
            .build());
    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The side color.")
            .defaultValue(new SettingColor(255, 255, 255, 75))
            .build());
    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The line color.")
            .defaultValue(new SettingColor(255, 255, 255, 75))
            .build());
    private double progress = 0;
    private boolean swapped, resync;
    private int slot = 0;
    private final Runnable reset = () -> {
        position = null;
        progress = 0;
        slot = 0;
        swapped = false;
        resync = true;
    };

    private final Runnable breakBlock = () -> {
        if (autoSwitch.get() && mc.player.getInventory().selectedSlot != slot && progress >= 0.9) {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }
        sendPackets.run();
    };

    public packetMine() {
        super(Addon.Snail, "packetMine", "Mines blocks better and faster using packets.");
    }

    public static void setBlock(BlockPos blockPos) {
        position = blockPos;
    }

    @Override
    public void onActivate() {
        reset.run();
    }

    @Override
    public void onDeactivate() {
        reset.run();
    }

    @EventHandler
    private void onMine(StartBreakingBlockEvent event) {
        position = event.blockPos;
        swapped = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (position != null && mc.world != null) {
            FindItemResult pickaxe = InvUtils.findFastestTool(mc.world.getBlockState(position));
            slot = pickaxe.slot();

            if (resync) {
                InvUtils.swap(mc.player.getInventory().selectedSlot, false);
                resync = false;
            }

            if (!pickaxe.found() || mc.player.getInventory().selectedSlot == slot) return;

            if (autoSwitch.get() && mc.player.getInventory().selectedSlot != slot && !swapped) {
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
                swapped = true;
                resync = true;
                return;
            }

            if (progress >= 0.95) {
                breakBlock.run();
                progress = 0; // Reset progress after breaking the block
                if(!instant.get()) {
                    swapped = false; // Reset swapped flag after breaking the block
                }
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot)); // Switch back to original slot
            } else {
                progress += BlockUtils.getBreakDelta(slot, mc.world.getBlockState(position));
            }
        }
    }

        @EventHandler
        private void onRender(Render3DEvent event){
            if (position != null && slot != -1 && progress != -1) {
                double prog = 1.0F - MathHelper.clamp((progress > 0.5F ? progress - 0.5F : 0.5F - progress) * 2.0F, 0.0F, 1.0F);
                VoxelShape shape = mc.world.getBlockState(position).getOutlineShape(mc.world, position);
                if (!shape.isEmpty()) {
                    Box box = shape.getBoundingBox();
                    double shrinkX = box.getLengthX() * prog * 0.5;
                    double shrinkY = box.getLengthY() * prog * 0.5;
                    double shrinkZ = box.getLengthZ() * prog * 0.5;
                    event.renderer.box(
                            position.getX() + box.minX + shrinkX,
                            position.getY() + box.minY + shrinkY,
                            position.getZ() + box.minZ + shrinkZ,
                            position.getX() + box.maxX - shrinkX,
                            position.getY() + box.maxY - shrinkY,
                            position.getZ() + box.maxZ - shrinkZ,
                            sideColor.get(), lineColor.get(), ShapeMode.Both, 0
                    );
                }
            }
        }
    }