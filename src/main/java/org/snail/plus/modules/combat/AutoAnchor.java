package org.snail.plus.modules.combat;


import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IBox;
import meteordevelopment.meteorclient.renderer.*;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.meteorclient.utils.render.*;
import meteordevelopment.meteorclient.utils.render.color.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.NotNull;
import org.snail.plus.Addon;
import org.snail.plus.utils.*;

import java.util.ArrayList;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author seasnail1
 */

public class AutoAnchor extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgDamage = settings.createGroup("Damage");
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgMisc = settings.createGroup("Misc");
    private final SettingGroup sgDebug = settings.createGroup("Debug");

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("Range to target player(s)")
            .defaultValue(3.0)
            .sliderRange(1.0, 10.0)
            .build());

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotation")
            .description("Rotates towards the block when placing.")
            .defaultValue(false)
            .build());

    private final Setting<Double> anchorSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("anchor speed")
            .description("Speed at which anchors are placed.")
            .defaultValue(1.0)
            .sliderRange(0.1, 10.0)
            .build());

    private final Setting<swapMode> swap = sgGeneral.add(new EnumSetting.Builder<swapMode>()
            .name("swap mode")
            .description("swap mode")
            .defaultValue(swapMode.inventory)
            .build());

    private final Setting<Double> maxSelfDamage = sgDamage.add(new DoubleSetting.Builder()
            .name("max self damage score")
            .description("the max amount to deal to you")
            .defaultValue(3.0)
            .sliderRange(0.0, 36.0)
            .build());

    private final Setting<Double> minDamage = sgDamage.add(new DoubleSetting.Builder()
            .name("min damage score")
            .description("the lowest amount of damage you should deal to the target (higher = less targets | lower = more targets)")
            .defaultValue(3.0)
            .sliderRange(0.0, 36.0)
            .build());
    private final Setting<Double> pauseHealth = sgDamage.add(new DoubleSetting.Builder()
            .name("pause health")
            .description("pauses the module when you are below this health")
            .defaultValue(0.0)
            .sliderRange(0.0, 36.0)
            .build());

    private final Setting<Boolean> strictDirection = sgGeneral.add(new BoolSetting.Builder()
            .name("strict direction")
            .description("Only places anchors in the direction you are facing")
            .defaultValue(false)
            .build());

    private final Setting<Integer> RadiusZ = sgGeneral.add(new IntSetting.Builder()
            .name("Radius Z")
            .description("the radius for Z")
            .defaultValue(1)
            .sliderRange(1, 5)
            .build());

    private final Setting<Integer> RadiusX = sgGeneral.add(new IntSetting.Builder()
            .name("Radius X")
            .description("the radius for X")
            .defaultValue(1)
            .sliderRange(1, 5)
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

    private final Setting<RenderMode> renderMode = sgRender.add(new EnumSetting.Builder<RenderMode>()
            .name("render mode")
            .description("render mode")
            .defaultValue(RenderMode.smooth)
            .build());

    private final Setting<Integer> rendertime = sgRender.add(new IntSetting.Builder()
            .name("render time")
            .description("render time")
            .defaultValue(3)
            .sliderRange(1, 100)
            .visible(() -> renderMode.get() == RenderMode.fading)
            .build());

    private final Setting<Boolean> shrink = sgRender.add(new BoolSetting.Builder()
            .name("fade shrink")
            .description("shrink fading render")
            .defaultValue(true)
            .visible(() -> renderMode.get() == RenderMode.fading)
            .build());

    private final Setting<Integer> Smoothness = sgRender.add(new IntSetting.Builder()
            .name("smoothness")
            .description("the smoothness")
            .defaultValue(3)
            .sliderRange(1, 100)
            .visible(() -> renderMode.get() == RenderMode.smooth)
            .build());

    private final Setting<Boolean> pauseUse = sgMisc.add(new BoolSetting.Builder()
            .name("pause on use")
            .description("pauses the module when you use a item")
            .defaultValue(false)
            .build());

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build());
    private final Setting<Boolean> debugRender = sgDebug.add(new BoolSetting.Builder()
            .name("debug render")
            .description("debug render")
            .defaultValue(false)
            .build());
    private final Setting<Boolean> debugCalculations = sgDebug.add(new BoolSetting.Builder()
            .name("debug calculations")
            .description("debug calculations")
            .defaultValue(false)
            .build());
    private final Setting<Boolean> debugPlace = sgDebug.add(new BoolSetting.Builder()
            .name("debug place")
            .description("debug place")
            .defaultValue(false)
            .build());
    private final Setting<Boolean> debugBreak = sgDebug.add(new BoolSetting.Builder()
            .name("debug break")
            .description("debug break")
            .defaultValue(false)
            .build());

    ExecutorService executor = Executors.newSingleThreadExecutor();
    private Box renderBoxOne, renderBoxTwo;
    private final ReentrantLock lock = new ReentrantLock();
    private List<BlockPos> AnchorPos = new ArrayList<>();
    private long lastPlacedTime;
    public AutoAnchor() {
        super(Addon.Snail, "Anchor Aura+", "places and breaks respawn anchors around players");
    }

    @Override
    public void onActivate() {
        try {
            AnchorPos = new ArrayList<>();
            if (executor == null || executor.isShutdown() || executor.isTerminated()) {
                executor = Executors.newSingleThreadExecutor();
            }
            executor.submit(() -> onTick(null));
        } catch (Exception e) {
            Addon.LOG.error(Arrays.toString(e.getStackTrace()));
        }
    }

    @Override
    public void onDeactivate() {
        try {
            if (executor != null) {
                executor.shutdown();
            }
            if (AnchorPos != null) {
                AnchorPos = new ArrayList<>(AnchorPos);
                AnchorPos.clear();
            }
        } catch (Exception e) {
            Addon.LOG.error(Arrays.toString(e.getStackTrace()));
        }
    }
    /**
     * Finds the best positions around a given player entity for placing anchors.
     *
     * @param entity The player entity around which to find positions.
     * @return An array of BlockPos representing the best positions for placing anchors.
     */
    public List<BlockPos> positions(PlayerEntity entity) {
        try {
            ArrayList<BlockPos> posList = new ArrayList<>();
            int radiusSquared = RadiusX.get() * RadiusX.get();

            for (int x = -RadiusX.get(); x <= RadiusX.get(); x++) {
                for (int z = -RadiusZ.get(); z <= RadiusZ.get(); z++) {
                    int distanceSquared = x * x + z * z;
                    if(debugCalculations.get()) info("found squared distance: " + distanceSquared);

                    if (distanceSquared <= radiusSquared) {
                        BlockPos pos = entity.getBlockPos().add(x, 0, z);
                        BlockPos mcPos = mc.player.getBlockPos().add(x, 0, z);
                        if (WorldUtils.isAir(pos) && !entity.getBoundingBox().intersects(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1) ||
                            WorldUtils.isAir(pos) && !mc.player.getBoundingBox().intersects(mcPos.getX(), mcPos.getY(), mcPos.getZ(), mcPos.getX() + 1, mcPos.getY() + 1, mcPos.getZ() + 1)) {
                            if(debugCalculations.get()) info("found pos: " + pos);
                            posList.add(pos);
                            return calculate(pos, entity);
                        } else {

                            for (int yOffset = -1; yOffset <= 1; yOffset++) {
                                BlockPos nearbyPos = pos.add(0, yOffset, 0);
                                if(debugCalculations.get()) info("fallback to " + nearbyPos);
                                if (WorldUtils.isAir(nearbyPos) && !entity.getBoundingBox().intersects(nearbyPos.getX(), nearbyPos.getY(), nearbyPos.getZ(), nearbyPos.getX() + 1, nearbyPos.getY() + 1, nearbyPos.getZ() + 1)) {
                                    posList.add(nearbyPos);
                                    return calculate(nearbyPos, entity);
                                }
                            }
                        }
                    }
                }
            }
            if (WorldUtils.isAir(entity.getBlockPos().up(2)) && !entity.getBoundingBox().intersects(entity.getBlockPos().up(2).getX(), entity.getBlockPos().up(2).getY(), entity.getBlockPos().up(2).getZ(), entity.getBlockPos().up(2).getX() + 1, entity.getBlockPos().up(2).getY() + 1, entity.getBlockPos().up(2).getZ() + 1)) {
                posList.add(entity.getBlockPos().up(2));
                calculate(entity.getBlockPos().up(2), entity);
                return calculate(entity.getBlockPos().up(2), entity);
            }
            if (WorldUtils.isAir(entity.getBlockPos().up(2)) && !entity.getBoundingBox().intersects(entity.getBlockPos().down(1).getX(), entity.getBlockPos().down(1).getY(), entity.getBlockPos().down(1).getZ(), entity.getBlockPos().down(1).getX() + 1, entity.getBlockPos().down(1).getY() + 1, entity.getBlockPos().down(1).getZ() + 1)) {
                posList.add(entity.getBlockPos().down(1));
                calculate(entity.getBlockPos().down(1), entity);
                return calculate(entity.getBlockPos().down(1), entity);
            }
            return posList;
        } catch (Exception e) {
            Addon.LOG.error(Arrays.toString(e.getStackTrace()));
            return new ArrayList<>();
        }
    }

    /**
     * Calculates the best positions around a given block position for placing anchors.
     * This method checks the initial position, searches for new positions within a specified radius,
     * and also checks head positions.
     *
     * @param pos The initial block position to check.
     * @param entity The player entity around which to find positions.
     * @return A list of BlockPos representing the best positions for placing anchors.
     */
    public List<BlockPos> calculate(@NotNull BlockPos pos, PlayerEntity entity) {
        double dmg = DamageUtils.anchorDamage(entity, new Vec3d(pos.getX(), pos.getY(), pos.getZ()));
        double selfDmg = DamageUtils.anchorDamage(mc.player, new Vec3d(pos.getX(), pos.getY(), pos.getZ()));
        if (dmg >= minDamage.get() || selfDmg < maxSelfDamage.get()) {
            if (!entity.getBoundingBox().intersects(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1)) return List.of(pos);
            if(debugCalculations.get()) info("filtered pos: " + pos);
            return List.of(pos);
        } else {
            if(debugCalculations.get()) info("couldn't filter pos, fallback to other positions " + pos);
            for (int i = -RadiusX.get(); i < RadiusX.get(); i++) {
                for (int j = -RadiusZ.get(); j < RadiusZ.get(); j++) {
                    for (int yOffset = -1; yOffset <= 1; yOffset++) {
                        BlockPos newPos = pos.add(i, yOffset, j);
                        if(debugCalculations.get()) info("found new pos: " + newPos);
                        dmg = DamageUtils.anchorDamage(entity, new Vec3d(newPos.getX(), newPos.getY(), newPos.getZ()));
                        selfDmg = DamageUtils.anchorDamage(mc.player, new Vec3d(newPos.getX(), newPos.getY(), newPos.getZ()));
                        if (dmg >= minDamage.get() && selfDmg <= maxSelfDamage.get()) {
                            if ((!entity.getBoundingBox().intersects(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1)) && !WorldUtils.isAir(pos)) {
                                if(debugCalculations.get()) info("filtered new pos: " + newPos);
                                return List.of(newPos);
                            }
                        }
                    }
                }
            }
        }

        BlockPos headPos = pos.up();
        if(debugCalculations.get()) info("checking head pos: " + headPos);
        dmg = DamageUtils.anchorDamage(entity, new Vec3d(headPos.getX(), headPos.getY(), headPos.getZ()));
        selfDmg = DamageUtils.anchorDamage(mc.player, new Vec3d(headPos.getX(), headPos.getY(), headPos.getZ()));
        if (dmg >= minDamage.get() && selfDmg <= maxSelfDamage.get()) {
            if (!entity.getBoundingBox().intersects(headPos.getX(), headPos.getY(), headPos.getZ(), headPos.getX() + 1, headPos.getY() + 1, headPos.getZ() + 1)) {
                if(debugCalculations.get()) info("filtered head pos: " + headPos);
                return List.of(headPos);
            }
        }
        return List.of(pos);
    }
    /**
     * Event handler for the tick event. This method is called every tick to perform the main logic of the AutoAnchor module.
     *
     * @param event The tick event.
     */
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (pauseUse.get() && mc.player.isUsingItem()) return;
        if(mc.player.getHealth() + mc.player.getAbsorptionAmount() <= pauseHealth.get()) return;
        if (Objects.requireNonNull(mc.world).getDimension().respawnAnchorWorks()) {
            toggle();
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPlacedTime < (1000 / anchorSpeed.get())) return;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || Friends.get().isFriend(player) || mc.player.distanceTo(player) > range.get()) continue;

            AnchorPos = positions(player);
            lock.lock();
            try {
                if (rotate.get()) Rotations.rotate(Rotations.getYaw(AnchorPos.getFirst()), Rotations.getPitch(AnchorPos.getFirst()));

                breakAnchor();
            } finally {
                lock.unlock();
            }

            for (Direction dir : Direction.values()) {
                if (strictDirection.get() && WorldUtils.strictDirection(AnchorPos.getFirst().offset(dir), dir.getOpposite())) continue;
            }

            lastPlacedTime = currentTime;
        }
    }

    public void breakAnchor() {
        lock.lock();
        try {
            for (BlockPos pos : AnchorPos) {
                FindItemResult stone = InvUtils.findInHotbar(Items.GLOWSTONE);
                FindItemResult anchor = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
                if (!stone.found() || !anchor.found()) continue;

                switch (swap.get()) {
                    case silent -> {
                        swapAndInteract(anchor, pos, false);
                        swapAndInteract(stone, pos, true);
                        swapAndInteract(anchor, pos, false);
                    }
                    case inventory -> {
                        swapUtils.pickSwitch(anchor.slot());
                        interactBlock(pos, false);
                        swapUtils.pickSwapBack();

                        swapUtils.pickSwitch(stone.slot());
                        interactBlock(pos, true);
                        swapUtils.pickSwapBack();

                        swapUtils.pickSwitch(anchor.slot());
                        interactBlock(pos, false);
                        swapUtils.pickSwapBack();
                    }
                    case normal -> {
                        swapAndInteract(anchor, pos, false);
                        swapAndInteract(stone, pos, true);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    private void swapAndInteract(FindItemResult item, BlockPos pos, boolean isGlowstone) {
        if(debugBreak.get()) info("swapping and interacting with block at " + pos + "is Glowstone: " + isGlowstone);
        InvUtils.swap(item.slot(), true);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(pos.getX(), pos.getY(), pos.getZ()), Direction.DOWN, pos, isGlowstone));
        InvUtils.swapBack();
    }

    private void interactBlock(BlockPos pos, boolean isGlowstone) {
        if(debugPlace.get()) info("interacting with block at " + pos);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(pos.getX(), pos.getY(), pos.getZ()), Direction.DOWN, pos, isGlowstone));
    }

    @EventHandler
    public void render(Render3DEvent event) {
        if (AnchorPos == null) return;

        for (BlockPos pos : AnchorPos) {
            switch (renderMode.get()) {
                case normal -> event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                case fading -> RenderUtils.renderTickingBlock(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0, rendertime.get(), true, shrink.get());
                case smooth -> {
                    if (renderBoxOne == null) renderBoxOne = new Box(pos);
                    if (renderBoxTwo == null) renderBoxTwo = new Box(pos);

                    if (renderBoxTwo instanceof IBox) {
                        ((IBox) renderBoxTwo).set(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
                    }

                    double offsetX = (renderBoxTwo.minX - renderBoxOne.minX) / Smoothness.get();
                    double offsetY = (renderBoxTwo.minY - renderBoxOne.minY) / Smoothness.get();
                    double offsetZ = (renderBoxTwo.minZ - renderBoxOne.minZ) / Smoothness.get();
                    if(debugRender.get()) info("offsets: " + offsetX + " " + offsetY + " " + offsetZ);
                    ((IBox) renderBoxOne).set(
                            renderBoxOne.minX + offsetX,
                            renderBoxOne.minY + offsetY,
                            renderBoxOne.minZ + offsetZ,
                            renderBoxOne.maxX + offsetX,
                            renderBoxOne.maxY + offsetY,
                            renderBoxOne.maxZ + offsetZ
                    );
                    if(debugRender.get()) info("rendering box: " + renderBoxOne);
                    event.renderer.box(renderBoxOne, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                }
            }
        }
    }

    @Override
    public String getInfoString() {
        if (mc.world == null) return null;
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player != mc.player && !Friends.get().isFriend(player) && mc.player.distanceTo(player) < range.get()) {
                return player.getDisplayName().getString();
            }
        }
        return null;
    }
    public enum RenderMode {
        fading,
        normal,
        smooth
    }
    public enum swapMode {
        inventory,
        silent,
        normal
    }
}