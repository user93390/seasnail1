package org.snail.plus.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.snail.plus.Addon;
import org.snail.plus.utils.WorldUtils;
import org.snail.plus.utils.swapUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class autoTrap extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgMisc = settings.createGroup("Misc");

    private final Setting<Double> updateSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("update-speed")
            .description("The speed to update calculations.")
            .defaultValue(1)
            .sliderRange(1, 10)
            .build());

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("The range in which to look for players.")
            .defaultValue(4)
            .sliderRange(1, 10)
            .build());

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Rotates towards the player.")
            .defaultValue(true)
            .build());

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
            .name("speed")
            .description("The speed to place blocks.")
            .defaultValue(1)
            .sliderRange(1, 10)
            .build());

    private final Setting<swapUtils.swapMode> swap = sgGeneral.add(new EnumSetting.Builder<swapUtils.swapMode>()
            .name("swap mode")
            .description("The mode used for swapping items when placing anchors.")
            .defaultValue(swapUtils.swapMode.Inventory)
            .build());

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("The mode to use.")
            .defaultValue(Mode.top)
            .build());

    private final Setting<WorldUtils.HandMode> hand = sgGeneral.add(new EnumSetting.Builder<WorldUtils.HandMode>()
            .name("hand")
            .description("The hand to use.")
            .defaultValue(WorldUtils.HandMode.MainHand)
            .build());

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The color of the side of the block.")
            .defaultValue(new SettingColor(255, 255, 255))
            .build());

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The color of the line.")
            .defaultValue(new SettingColor(255, 255, 255))
            .build());

    private final Setting<Boolean> pauseUse = sgMisc.add(new BoolSetting.Builder()
            .name("pause-use")
            .description("Pauses the module when you're using an item.")
            .defaultValue(true)
            .build());

    private final ReentrantLock lock = new ReentrantLock();
    ExecutorService executor = Executors.newSingleThreadExecutor();
    private Long lastPlaceTime = 0L;
    private Long lastUpdateTime = 0L;
    private Long currentTime = System.currentTimeMillis();

    public autoTrap() {
        super(Addon.Snail, "auto-trap+", "Automatically places blocks around players to trap them.");
    }

    @Override
    public void onActivate() {
        if (executor == null || executor.isShutdown() || executor.isTerminated()) {
            executor = Executors.newSingleThreadExecutor();
        }
        executor.submit(() -> onTick(null));
        lastPlaceTime = 0L;
        lastUpdateTime = 0L;
        currentTime = System.currentTimeMillis();
    }

    @Override
    public void onDeactivate() {
        if (executor != null) {
            executor.shutdown();
        }
        lastPlaceTime = 0L;
        lastUpdateTime = 0L;
        currentTime = System.currentTimeMillis();
    }

    public List<BlockPos> getFindingPositions(@NotNull PlayerEntity entity) {
        BlockPos basePos = entity.getBlockPos().up(1);
        return switch (mode.get()) {
            case top -> Collections.singletonList(basePos.up(1));
            case face -> List.of(
                    basePos.north(),
                    basePos.south(),
                    basePos.east(),
                    basePos.west()
            );
            case full -> List.of(
                    basePos.north(),
                    basePos.south(),
                    basePos.east(),
                    basePos.west(),
                    basePos.up(1)
            );
        };
    }
    @EventHandler
    public void onTick(TickEvent.Post event) {
        try {
            for (PlayerEntity entity : mc.world.getPlayers()) {
                if (mc.player.distanceTo(entity) > range.get()) continue;
                if (entity == mc.player || Friends.get().isFriend(entity)) continue;
                if (currentTime - lastUpdateTime < (1000 / updateSpeed.get())) return;
                List<BlockPos> positions = getFindingPositions(entity);

                lock.lock();
                try {
                    for (BlockPos pos : positions) {
                        if (rotate.get()) Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), 100);
                        doPlace(pos);
                    }
                } finally {
                    lock.unlock();
                }
                lastUpdateTime = currentTime;
            }
        } catch (Exception e) {
            Addon.LOG.error("An error occurred while running auto-trap+.", e);
            error("unknown error occurred" + String.format("Error: %s", e.getMessage()));
        }
    }

    public void doPlace(BlockPos pos) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPlaceTime < (1000 / speed.get())) return;
        if (pauseUse.get() && mc.player.isUsingItem()) return;
        FindItemResult itemResult = InvUtils.find(Items.OBSIDIAN);
        WorldUtils.placeBlock(itemResult, pos, hand.get(), WorldUtils.DirectionMode.Down, true, swap.get(), rotate.get());
        lastUpdateTime = currentTime;
    }

    public enum Mode {
        top,
        full,
        face
    }
}
