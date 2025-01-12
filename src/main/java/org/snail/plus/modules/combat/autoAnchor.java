package org.snail.plus.modules.combat;

import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IBox;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import org.joml.Vector3d;
import org.snail.plus.Addon;
import org.snail.plus.utilities.CombatUtils;
import org.snail.plus.utilities.MathUtils;
import org.snail.plus.utilities.WorldUtils;
import org.snail.plus.utilities.swapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.locks.ReentrantLock;

import static meteordevelopment.meteorclient.utils.entity.DamageUtils.HIT_FACTORY;

/**
 * Author: seasnail1
 */
public class autoAnchor extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlacement = settings.createGroup("Placement");
    private final SettingGroup sgDamage = settings.createGroup("Damage");
    private final SettingGroup sgAntiCheat = settings.createGroup("Anti-Cheat");
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgMisc = settings.createGroup("Misc");
    private final SettingGroup sgDebug = settings.createGroup("Debug");

    // General Settings
    private final Setting<Double> placeBreak = sgGeneral.add(new DoubleSetting.Builder()
            .name("place-break range")
            .description("The maximum distance to place and break anchors.")
            .defaultValue(3.0)
            .sliderRange(1.0, 10.0)
            .build());

    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("target range")
            .description("The maximum distance to target players.")
            .defaultValue(3.0)
            .sliderRange(1.0, 20)
            .build());

    private final Setting<CombatUtils.filterMode> targetMode = sgGeneral.add(new EnumSetting.Builder<CombatUtils.filterMode>()
            .name("filter mode")
            .description("The mode used for targeting players.")
            .defaultValue(CombatUtils.filterMode.LowestHealth)
            .build());

    private final Setting<Boolean> packetPlace = sgGeneral.add(new BoolSetting.Builder()
            .name("packet place")
            .description("Uses packets to place the anchors.")
            .defaultValue(true)
            .build());

    private final Setting<Double> updateSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("update speed")
            .description("The speed at which the module updates, in ticks. (Higher values may cause lag)")
            .defaultValue(1.0)
            .sliderRange(7.5, 10.0)
            .build());

    // Placement Settings
    private final Setting<Boolean> rotate = sgPlacement.add(new BoolSetting.Builder()
            .name("rotation")
            .description("Enables rotation towards the block when placing anchors.")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> airPlace = sgPlacement.add(new BoolSetting.Builder()
            .name("air place")
            .description("Allows placing anchors in the air.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> predictMovement = sgPlacement.add(new BoolSetting.Builder()
            .name("predict movement")
            .description("Predicts the movement of the target.")
            .defaultValue(false)
            .build());

    private final Setting<Integer> steps = sgPlacement.add(new IntSetting.Builder()
            .name("steps")
            .description("The amount of steps to predict.")
            .defaultValue(1)
            .sliderRange(1, 10)
            .visible(predictMovement::get)
            .build());

    private final Setting<Double> anchorSpeed = sgPlacement.add(new DoubleSetting.Builder()
            .name("anchor speed")
            .description("The speed at which anchors are placed, in anchors per second.")
            .defaultValue(1.75)
            .sliderRange(0.1, 10.0)
            .build());

    private final Setting<Boolean> strictDirection = sgAntiCheat.add(new BoolSetting.Builder()
            .name("strict direction")
            .description("Only places anchors in the direction you are facing.")
            .defaultValue(false)
            .build());

    private final Setting<Integer> threads = sgPlacement.add(new IntSetting.Builder()
            .name("threads")
            .description("The amount of threads to use for calculations.")
            .defaultValue(1)
            .sliderRange(1, 10)
            .build());

    private final Setting<Boolean> liquidPlace = sgPlacement.add(new BoolSetting.Builder()
            .name("liquid place")
            .description("Allows placing anchors in liquids.")
            .defaultValue(false)
            .build());

    private final Setting<swapUtils.swapMode> swap = sgPlacement.add(new EnumSetting.Builder<swapUtils.swapMode>()
            .name("swap mode")
            .description("The mode used for swapping items when placing anchors.")
            .defaultValue(swapUtils.swapMode.Move)
            .build());

    private final Setting<WorldUtils.DirectionMode> directionMode = sgPlacement.add(new EnumSetting.Builder<WorldUtils.DirectionMode>()
            .name("direction")
            .description("The mode used for direction.")
            .defaultValue(WorldUtils.DirectionMode.Down)
            .visible(strictDirection::get)
            .build());

    private final Setting<Boolean> swing = sgPlacement.add(new BoolSetting.Builder()
            .name("swing")
            .description("Swings your hand.")
            .defaultValue(true)
            .build());

    private final Setting<WorldUtils.HandMode> swingMode = sgPlacement.add(new EnumSetting.Builder<WorldUtils.HandMode>()
            .name("swing mode")
            .description("The mode used for swinging your hand.")
            .defaultValue(WorldUtils.HandMode.MainHand)
            .visible(swing::get)
            .build());

    private final Setting<Double> minDamage = sgDamage.add(new DoubleSetting.Builder()
            .name("min damage")
            .description("The minimum damage required to place an anchor.")
            .defaultValue(0.5)
            .sliderRange(0.0, 36.0)
            .build());

    private final Setting<Double> maxDamage = sgDamage.add(new DoubleSetting.Builder()
            .name("max damage")
            .description("The maximum damage towards you")
            .defaultValue(5.0)
            .sliderRange(0.0, 36.0)
            .build());

    private final Setting<Double> maxSelfError = sgDamage.add(new DoubleSetting.Builder()
            .name("max-self dropoff")
            .description("The maximum error in self damage calculations.")
            .defaultValue(0.5)
            .sliderRange(0.0, 1)
            .build());

    private final Setting<Double> maxDamageError = sgDamage.add(new DoubleSetting.Builder()
            .name("max-damage dropoff")
            .description("The maximum error in damage calculations.")
            .defaultValue(0.5)
            .sliderRange(0.0, 1)
            .build());

    private final Setting<Double> pauseHealth = sgDamage.add(new DoubleSetting.Builder()
            .name("pause health")
            .description("Pauses the module when your health is below this value.")
            .defaultValue(1)
            .sliderRange(0.0, 36.0)
            .build());

    private final Setting<Integer> rotationSteps = sgAntiCheat.add(new IntSetting.Builder()
            .name("rotation steps")
            .description("The amount of steps to rotate.")
            .sliderRange(5, 25)
            .visible(rotate::get)
            .build());

    private final Setting<Boolean> rayCast = sgAntiCheat.add(new BoolSetting.Builder()
            .name("raytrace")
            .defaultValue(false)
            .build());

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side color")
            .description("The color of the sides of the rendered anchor box.")
            .defaultValue(new SettingColor(255, 0, 0, 75))
            .build());

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line color")
            .description("The color of the lines of the rendered anchor box.")
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .build());

    private final Setting<SettingColor> damageColor = sgRender.add(new ColorSetting.Builder()
            .name("damage color")
            .description("The color of the damage text.")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .build());

    private final Setting<Double> damageTextScale = sgRender.add(new DoubleSetting.Builder()
            .name("damage text scale")
            .description("The scale of the damage text.")
            .defaultValue(1.0)
            .sliderRange(0.1, 2.0)
            .build());

    private final Setting<Boolean> renderOutline = sgRender.add(new BoolSetting.Builder()
            .name("render outline")
            .description("Renders an outline around the anchor box.")
            .defaultValue(true)
            .build());

    private final Setting<RenderMode> renderMode = sgRender.add(new EnumSetting.Builder<RenderMode>()
            .name("render mode")
            .description("The mode used for rendering the anchor box.")
            .defaultValue(RenderMode.smooth)
            .build());

    private final Setting<Integer> rendertime = sgRender.add(new IntSetting.Builder()
            .name("render time")
            .description("The duration for which the anchor box is rendered, in ticks.")
            .defaultValue(3)
            .sliderRange(1, 100)
            .visible(() -> renderMode.get() == RenderMode.fading)
            .build());

    private final Setting<Integer> Smoothness = sgRender.add(new IntSetting.Builder()
            .name("smoothness")
            .description("The smoothness of the anchor box rendering in smooth mode.")
            .defaultValue(3)
            .sliderRange(1, 100)
            .visible(() -> renderMode.get() == RenderMode.smooth)
            .build());

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape mode")
            .description("The shape mode used for rendering the anchor box.")
            .defaultValue(ShapeMode.Both)
            .build());

    private final Setting<Boolean> pauseUse = sgMisc.add(new BoolSetting.Builder()
            .name("pause on use")
            .description("Pauses the module when you are using an item.")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> debugCalculations = sgDebug.add(new BoolSetting.Builder()
            .name("debug calculations")
            .description("Enables debug information for calculations.")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> debugBreak = sgDebug.add(new BoolSetting.Builder()
            .name("debug break")
            .description("Enables debug information for breaking anchors.")
            .defaultValue(false)
            .build());

    private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(threads.get());
    private final ReentrantLock lock = new ReentrantLock();
    private Box renderBoxOne, renderBoxTwo;
    private List<BlockPos> AnchorPos = new ArrayList<>();
    private long lastPlacedTime;
    private PlayerEntity entity;
    Vec3d start;
    private double damageValue;
    private double selfDamageValue;
    private long lastUpdateTime;
    private double selfDamage;
    private double targetDamage;
    boolean placedAnchor;

    Runnable doBreak = () -> {
        for (BlockPos pos : AnchorPos) {
            if (rotate.get()) {
                Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), 100, () -> MathUtils.updateRotation(rotationSteps.get()));
                breakAnchor();
            } else {
                breakAnchor();
            }
        }
    };

    Runnable resetDamageValues = () -> {
        selfDamage = 0;
        targetDamage = 0;
    };

    Runnable reset = () -> {
        if(executor != null) {
            executor.shutdown();
        }
        start = null;
        entity = null;
        placedAnchor = false;
        selfDamage = 0;
        targetDamage = 0;
        damageValue = 0;
        selfDamageValue = 0;
        AnchorPos = new ArrayList<>();
    };

    public autoAnchor() {
        super(Addon.Snail, "Anchor aura+", "blows up respawn anchors to damage enemies");
    }

    @Override
    public void onActivate() {
        try {
           if (executor.isShutdown() || executor.isTerminated() || executor.isTerminating()) {
                executor = new ScheduledThreadPoolExecutor(threads.get());
            }
            reset.run();
        } catch (Exception e) {
            error("An error occurred while activating the module: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDeactivate() {
        try {
            reset.run();
        } catch (Exception e) {
            error("An error occurred while deactivating the module: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private List<BlockPos> positions(PlayerEntity entity, Vec3d start) {
        return MathUtils.getSphere(BlockPos.ofFloored(start), MathUtils.getRadius((int) Math.sqrt(placeBreak.get()), (int) Math.sqrt(placeBreak.get())))
                .stream()
                .sorted(Comparator.comparingDouble(pos -> pos.getSquaredDistance(start)))
                .filter(pos -> {
                    try {
                        if (pos.getSquaredDistance(mc.player.getBlockPos()) > placeBreak.get() * placeBreak.get()) return false;

                        Vec3d vec = new Vec3d(pos.getX(), pos.getY(), pos.getZ());

                        targetDamage = DamageUtils.explosionDamage(entity, entity.getPos(), entity.getBoundingBox(), vec, 10f, HIT_FACTORY);
                        selfDamage = DamageUtils.explosionDamage(mc.player, mc.player.getPos(), mc.player.getBoundingBox(), vec, 10f, HIT_FACTORY);

                        damageValue = targetDamage;
                        selfDamageValue = selfDamage;

                        double selfDropoff = (selfDamage - maxSelfError.get()) / selfDamage;
                        double targetDropoff = (targetDamage - maxDamageError.get()) / targetDamage;

                        if (rayCast.get() && MathUtils.rayCast(vec)) {
                            if (debugCalculations.get()) info("failed raycast check");
                            return false;
                        }

                        if (strictDirection.get() && !WorldUtils.strictDirection(pos, directionMode.get())) {
                            if (debugCalculations.get()) info("failed direction check");
                            return false;
                        }

                        if (!WorldUtils.isAir(pos, false)) return false;
                        if (!airPlace.get() && WorldUtils.isAir(pos.down(1), false)) return false;

                        if (selfDropoff < maxDamageError.get() && targetDropoff < maxDamageError.get()) {
                            if (selfDamage < maxDamage.get() && targetDamage > minDamage.get() && WorldUtils.hitBoxCheck(pos, true)) {
                                if (debugCalculations.get()) info("passed damage check %s %s", Math.round(selfDamage), Math.round(targetDamage));
                                damageValue = targetDamage;
                                selfDamageValue = selfDamage;
                                return true;
                            } else {
                                if (debugCalculations.get()) info("failed damage check %s %s", Math.round(selfDamage), Math.round(targetDamage));
                            }
                        }
                    } catch (Exception e) {
                        error("An error occurred while calculating the positions: " + e.getMessage());
                        throw new RuntimeException(e);
                    }

                    return false;
                })
                .findFirst()
                .map(Collections::singletonList)
                .orElse(new ArrayList<>());
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (executor == null || executor.isShutdown() || executor.isTerminated()) {
            executor = new ScheduledThreadPoolExecutor(threads.get());
        }

        try {
            executor.execute(resetDamageValues);
            if (updateEat()) return;
            targetDamage = 0;
            selfDamage = 0;

            if (mc.world.getDimension().respawnAnchorWorks()) {
                error("You are in the wrong dimension!");
                return;
            }

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUpdateTime < (1000 / updateSpeed.get())) return;

            PlayerEntity player = CombatUtils.filter(mc.world.getPlayers(), targetMode.get(), targetRange.get());
            if (debugCalculations.get()) info("found target: %s", player.getName().getString().toLowerCase());
            if (player == null) return;
            executor.execute(() -> {
                //should we extrapolate the player's position? if so, we do it here
                start = predictMovement.get() ? MathUtils.extrapolatePos(player, steps.get()) : player.getPos();
                List<BlockPos> positions = positions(player, start);
                lock.lock();
                try {
                    AnchorPos = positions;
                    entity = player;
                    doBreak.run();
                } finally {
                    lock.unlock();
                }
            });

            lastUpdateTime = currentTime;
        } catch (Exception e) {
            error("An error occurred while updating the module: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void breakAnchor() {
        lock.lock();
        try {

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastPlacedTime < (1000 / anchorSpeed.get())) return;
            if (mc.player == null || mc.player.getHealth() <= pauseHealth.get()) return;

            FindItemResult stone = InvUtils.find(Items.GLOWSTONE);
            FindItemResult anchor = InvUtils.find(Items.RESPAWN_ANCHOR);

            if(swap.get() == swapUtils.swapMode.silent || swap.get() == swapUtils.swapMode.normal) {
                stone = InvUtils.findInHotbar(Items.GLOWSTONE);
                anchor = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
            }

            if (!stone.found() || !anchor.found()) {
                error("Invalid items in inventory");
                return;
            }

            for (BlockPos pos : AnchorPos) {
                    if (pos.getSquaredDistance(mc.player.getPos()) < placeBreak.get() * placeBreak.get()) {
                    if (debugBreak.get()) info("breaking anchor at: " + WorldUtils.getCoords(pos));
                    WorldUtils.placeBlock(anchor, pos, swingMode.get(), directionMode.get(), packetPlace.get(), swap.get(), rotate.get());
                        WorldUtils.placeBlock(stone, pos, swingMode.get(), directionMode.get(), true, swap.get(), rotate.get());

                    if (!packetPlace.get()) {
                        BlockUtils.interact(new BlockHitResult(new Vec3d(pos.getX(), pos.getY(), pos.getZ()), Direction.DOWN, pos, false), Hand.MAIN_HAND, swing.get());
                    }

                    WorldUtils.placeBlock(anchor, pos, swingMode.get(), directionMode.get(), packetPlace.get(), swap.get(), rotate.get());
                }
            }
            lastPlacedTime = currentTime;
        } catch (Exception e) {
            error("An error occurred while breaking the anchor: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    private boolean updateEat() {
        return pauseUse.get() && mc.player.isUsingItem();
    }

    @EventHandler
    public void render(Render3DEvent event) {
            try {
                for (BlockPos pos : AnchorPos) {
                    if (entity == mc.player || Friends.get().isFriend(entity) || mc.player.distanceTo(entity) > targetRange.get()) {
                        continue;
                    }

                    if (renderOutline.get()) {
                        event.renderer.box(MathUtils.extrapolateBox(entity, steps.get()), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }

                    switch (renderMode.get()) {
                        case normal -> event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);

                        case fading ->
                                RenderUtils.renderTickingBlock(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0, rendertime.get(), true, false);

                        case smooth -> {
                            if (renderBoxOne == null) {
                                renderBoxOne = new Box(pos);
                            }

                            if (renderBoxTwo == null) {
                                renderBoxTwo = new Box(pos);
                            }

                            if (renderBoxTwo instanceof IBox) {
                                ((IBox) renderBoxTwo).set(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
                            }

                            double offsetX = (renderBoxTwo.minX - renderBoxOne.minX) / Smoothness.get();
                            double offsetY = (renderBoxTwo.minY - renderBoxOne.minY) / Smoothness.get();
                            double offsetZ = (renderBoxTwo.minZ - renderBoxOne.minZ) / Smoothness.get();

                            ((IBox) renderBoxOne).set(
                                    renderBoxOne.minX + offsetX,
                                    renderBoxOne.minY + offsetY,
                                    renderBoxOne.minZ + offsetZ,
                                    renderBoxOne.maxX + offsetX,
                                    renderBoxOne.maxY + offsetY,
                                    renderBoxOne.maxZ + offsetZ);
                            event.renderer.box(renderBoxOne, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                        }
                    }
                }
            } catch (Exception e) {
                error("An error occurred while rendering the anchor positions: " + e.getMessage());
            }
    }

    @EventHandler
    public void render2D(Render2DEvent event) {
        for (BlockPos pos : AnchorPos) {
            if (entity == mc.player || Friends.get().isFriend(entity) || mc.player.distanceTo(entity) > targetRange.get()) {
                continue;
            }

            Vector3d vec = new Vector3d(pos.getX(), pos.getY(), pos.getZ());
            if (renderMode.get() == RenderMode.smooth && renderBoxOne != null) {
                vec.set(renderBoxOne.minX + 0.5, renderBoxOne.minY + 0.5, renderBoxOne.minZ + 0.5);
            } else {
                vec.set(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            }
            if (NametagUtils.to2D(vec, damageTextScale.get())) {
                NametagUtils.begin(vec);
                TextRenderer.get().begin(1, false, true);

                String text = String.format("%.1f / %.1f", damageValue, selfDamageValue);
                double w = TextRenderer.get().getWidth(text) / 2;
                TextRenderer.get().render(text, -w, 0, damageColor.get(), false);

                TextRenderer.get().end();
                NametagUtils.end();
            }
        }
    }

    @Override
    public String getInfoString() {
        return entity != null ? entity.getName().getString() : null;
    }

    public enum RenderMode {
        fading,
        normal,
        smooth
    }
}