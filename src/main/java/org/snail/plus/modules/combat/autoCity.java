package org.snail.plus.modules.combat;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.snail.plus.Addon;
import org.snail.plus.utils.WorldUtils;
import org.snail.plus.utils.swapUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class autoCity extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgBehavior = settings.createGroup("Behavior");
    private final SettingGroup sgAntiSurround = settings.createGroup("Anti-Surround");
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgMisc = settings.createGroup("Misc");

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
            .name("range")
            .description("The range in which to attack players.")
            .defaultValue(5)
            .sliderRange(1, 10)
            .build());

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Rotates to the player.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> chatInfo = sgGeneral.add(new BoolSetting.Builder()
            .name("chat-info")
            .description("Sends a message to chat when a player is attacked.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> strictDirection = sgGeneral.add(new BoolSetting.Builder()
            .name("strict-direction")
            .description("Only attacks players in the direction you are looking.")
            .defaultValue(false)
            .build());

    private final Setting<swapMode> SwitchMode = sgGeneral.add(new EnumSetting.Builder<swapMode>()
            .name("swap-mode")
            .description("how to switch to pickaxe when mining")
            .defaultValue(swapMode.Inventory)
            .build());

    private final Setting<behavior> behaviorMode = sgBehavior.add(new EnumSetting.Builder<behavior>()
            .name("behavior")
            .description("how to attack players")
            .defaultValue(behavior.smart)
            .build());

    private final Setting<Boolean> randomBlock = sgAntiSurround.add(new BoolSetting.Builder()
            .name("random-block")
            .description("Randomly breaks blocks around target.")
            .defaultValue(true)
            .visible(() -> behaviorMode.get() == behavior.smart)
            .build());

    private final Setting<Boolean> autoBlock = sgAntiSurround.add(new BoolSetting.Builder()
            .name("auto-block")
            .description("Automatically places blocks to block future surround attempts.")
            .defaultValue(true)
            .visible(() -> behaviorMode.get() == behavior.smart)
            .build());

    private final Setting<mineMode> MineMode = sgBehavior.add(new EnumSetting.Builder<mineMode>()
            .name("mine-mode")
            .description("how to mine blocks")
            .defaultValue(mineMode.instant)
            .build());

    private final Setting<Double> mineCooldown = sgBehavior.add(new DoubleSetting.Builder()
            .name("mine-cooldown")
            .description("The time to wait before instant mining the same block again.")
            .defaultValue(0.5)
            .sliderRange(0.0, 10.0)
            .visible(() -> MineMode.get() == mineMode.instant)
            .build());

    private final Setting<renderMode> RenderMode = sgRender.add(new EnumSetting.Builder<renderMode>()
            .name("render-mode")
            .description("how to render the target")
            .defaultValue(renderMode.normal)
            .build());

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The color of the sides.")
            .defaultValue(new Color(255, 255, 255, 255))
            .build());

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The color of the lines.")
            .defaultValue(new Color(255, 255, 255, 255))
            .build());

    private final Setting<Integer> rendertime = sgRender.add(new IntSetting.Builder()
            .name("render-time")
            .description("The time in seconds before the render disappears.")
            .defaultValue(1)
            .sliderRange(0, 10)
            .build());

    private final Setting<Boolean> supportPlace = sgMisc.add(new BoolSetting.Builder()
            .name("support-place")
            .description("Automatically places blocks under city blocks.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> pauseEat = sgMisc.add(new BoolSetting.Builder()
            .name("pause-eat")
            .description("Pause eating when attacking players.")
            .defaultValue(true)
            .build());

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private List<BlockPos> possiblePositions = new ArrayList<>();
    private BlockPos currentPos;

    public autoCity() {
        super(Addon.Snail, "auto-city+", "Automatically attacks players surrounds");
    }

    @Override
    public void onActivate() {
        possiblePositions = new ArrayList<>();
        if (executor == null || executor.isShutdown() || executor.isTerminated()) {
            executor = Executors.newSingleThreadExecutor();
        }
        executor.submit(() -> onTick(null));
        currentPos = null;
    }

    @Override
    public void onDeactivate() {
        possiblePositions = new ArrayList<>();
        possiblePositions.clear();
        currentPos = null;
    }

    public List<BlockPos> getPossiblePositions(PlayerEntity entity) {
        possiblePositions.clear();
        Random random = new Random();

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos pos = entity.getBlockPos().add(x, 0, z);
                if (!WorldUtils.isAir(pos)) {
                    possiblePositions.add(pos);
                }
            }
        }

        if (!possiblePositions.isEmpty()) {
            currentPos = randomBlock.get() ? possiblePositions.get(random.nextInt(possiblePositions.size())) : possiblePositions.get(0);
        }

        return possiblePositions;
    }

    public BlockPos[] getPossibleSupportBlocks() {
        for(BlockPos pos : possiblePositions) {
            for (int y = 0; y > -1; y--) {
                BlockPos supportPos = pos.add(0, y, 0);
                if (WorldUtils.isAir(supportPos)) {
                    WorldUtils.placeBlock(InvUtils.find(Items.OBSIDIAN), supportPos, true, true, rotate.get());
                    return new BlockPos[]{supportPos};
                }
            }
        }
        return null;
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (mc.world != null && mc.player != null && mc.interactionManager != null) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player || Friends.get().isFriend(player)) continue;
                if(pauseEat.get() && mc.player.isUsingItem()) continue;
                if (player.distanceTo(mc.player) > range.get()) continue;

                for (BlockPos blockPos : getPossiblePositions(player)) {
                    BlockState blockState = mc.world.getBlockState(blockPos);
                    FindItemResult bestSlot = InvUtils.findFastestTool(blockState);
                    if (rotate.get()) {
                        Rotations.rotate(Rotations.getYaw(player), Rotations.getPitch(player));
                    }
                    if (strictDirection.get() && WorldUtils.strictDirection(blockPos, Direction.DOWN)) continue;
                    if(supportPlace.get()) getPossibleSupportBlocks();

                    attack(blockPos);
                    swap(bestSlot);
                }
                if (chatInfo.get()) {
                    info("Attacking ->" + WorldUtils.getName(player));
                }
            }
        }
    }

    public void swap(FindItemResult item) {
        switch (SwitchMode.get()) {
            case Inventory:
                swapUtils.pickSwitch(item.slot());
                return;
            case Silent:
                if (mc.player.getInventory().selectedSlot == item.slot() || mc.player.getInventory().selectedSlot == item.slot()) {
                    mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(item.slot()));
                    break;
                }
                return;
            case Normal:
                InvUtils.swap(item.slot(), false);
                return;
            case None:
        }
    }

    public Boolean attack(BlockPos blockPos) {
        switch (MineMode.get()) {
            case normal:
                if (rotate.get()) Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos));
                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));
                break;
            case instant:
                if (rotate.get()) Rotations.rotate(Rotations.getYaw(blockPos), (Rotations.getPitch(blockPos)));
                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));
                break;
        }
        return true;
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        switch (RenderMode.get()) {
            case normal:
                    event.renderer.box(currentPos, sideColor.get(), lineColor.get(), ShapeMode.Both, 0);
                break;
            case fade:
                for (BlockPos blockPos : possiblePositions) {
                    boolean shouldShrink = attack(blockPos);
                    if (shouldShrink) {
                        RenderUtils.renderTickingBlock(currentPos, sideColor.get(), lineColor.get(), ShapeMode.Both, 0, rendertime.get(), true, false);
                    }
                }
                break;
        }
    }

    public enum swapMode {
        Inventory,
        Silent,
        Normal,
        None
    }

    public enum behavior {
        smart,
        fast,
    }

    public enum mineMode {
        instant,
        normal,
        normalRemine,
    }

    public enum renderMode {
        normal,
        fade,
        shrink,
    }
}
