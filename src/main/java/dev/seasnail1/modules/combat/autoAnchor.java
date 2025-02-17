package dev.seasnail1.modules.combat;

import dev.seasnail1.Addon;
import dev.seasnail1.utilities.CombatUtils;
import dev.seasnail1.utilities.MathHelper;
import dev.seasnail1.utilities.WorldUtils;
import dev.seasnail1.utilities.swapUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IBox;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;

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

    private final Setting<Integer> rotationSteps = sgAntiCheat.add(new IntSetting.Builder()
            .name("rotation steps")
            .description("The amount of steps to rotate.")
            .sliderRange(5, 25)
            .visible(rotate::get)
            .build());

    private final Setting<Boolean> airPlace = sgPlacement.add(new BoolSetting.Builder()
            .name("air place")
            .description("Allows placing anchors in the air.")
            .defaultValue(true)
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

    private final Setting<WorldUtils.DirectionMode> directionMode = sgPlacement.add(new EnumSetting.Builder<WorldUtils.DirectionMode>()
            .name("direction")
            .description("The mode used for direction.")
            .defaultValue(WorldUtils.DirectionMode.Down)
            .visible(() -> !strictDirection.get())
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

    private final Setting<Double> pauseHealth = sgDamage.add(new DoubleSetting.Builder()
            .name("pause health")
            .description("Pauses the module when your health is below this value.")
            .defaultValue(1)
            .sliderRange(0.0, 36.0)
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

    private final Setting<RenderMode> renderMode = sgRender.add(new EnumSetting.Builder<RenderMode>()
            .name("render mode")
            .description("The mode used for rendering the anchor box.")
            .defaultValue(RenderMode.smooth)
            .build());

    private final Setting<Integer> duration = sgRender.add(new IntSetting.Builder()
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

    protected ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(threads.get());

    Set<PlayerEntity> entities = new HashSet<>();
    Set<BlockPos> AnchorPos = new HashSet<>();
    Map<Float, Float> damages = new HashMap<>();

    Vec3d start;
    BlockPos pos;
    Box renderBoxOne, renderBoxTwo;

    long lastPlacedTime, lastUpdateTime;
    float selfDamage, targetDamage, bestDamage = 0;

    boolean broken = false;

    Runnable doBreak = () -> {
        if (rotate.get()) {
            Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), 100, () -> MathHelper.updateRotation(rotationSteps.get()));
            breakAnchor();
        } else {
            breakAnchor();
        }
    };

    Runnable resetDamageValues = () -> {
        damages.clear();
        selfDamage = -1;
        targetDamage = -1;
        bestDamage = -1;
    };

    Runnable reset = () -> {
        if (executor != null) {
            executor.shutdown();
        }

        start = null;
        pos = null;
        broken = false;

        resetDamageValues.run();
        AnchorPos.clear();

        if (entities != null && damages != null) {
            entities.clear();
            damages.clear();
        }
    };

    public autoAnchor() {
        super(Addon.CATEGORY, "Auto-anchor+", "Blows up respawn anchors to deal massive damage to targets");
    }

    @Override
    public void onActivate() {
        try {
            if (executor.isShutdown() || executor.isTerminated() || executor.isTerminating()) {
                executor = new ScheduledThreadPoolExecutor(threads.get());
            }

            executor.execute(reset);
        } catch (Exception e) {
            error("An error occurred while activating the module: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDeactivate() {
        try {
            executor.execute(reset);
        } catch (Exception e) {
            error("An error occurred while deactivating the module: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void calculateDamage(BlockPos pos) {
        entities.forEach(player -> {
            Vec3d vec = new Vec3d(pos.getX(), pos.getY(), pos.getZ());
            float anchordmg = DamageUtils.anchorDamage(player, vec);
            float selfAnchor = DamageUtils.anchorDamage(mc.player, vec);

            targetDamage = anchordmg;
            selfDamage = selfAnchor;
        });
    }

    private boolean dmgCheck() {
        if (targetDamage < bestDamage) return false;
        if (selfDamage > maxDamage.get()) return false;

        return !(targetDamage < minDamage.get());
    }

    private boolean validBlock(BlockPos pos) {
        return WorldUtils.intersectCheck(pos, true)
                && (airPlace.get() || !WorldUtils.isAir(pos.down(1), false))
                && WorldUtils.isAir(pos, false);
    }

    void calculate(Vec3d start) {
        resetDamageValues.run();

        int radius = (int) Math.sqrt(placeBreak.get());

        List<BlockPos> sphere = new ArrayList<>(MathHelper.getSphere(BlockPos.ofFloored(start), MathHelper.getRadius(radius, radius))
                .stream().toList());

        sphere.forEach(pos -> executor.schedule(() -> {
            if (validBlock(pos)) {
                sphere.remove(pos);
            }

            if (sphere.isEmpty()) {
                return;
            }

            calculateDamage(pos);

            damages.put(targetDamage, selfDamage);

            //remove if the damage is less than the max damage or the damage is less than the min damage
            damages.entrySet().removeIf(entry -> entry.getValue() > maxDamage.get() || entry.getValue() < minDamage.get());

            //best damage equals the highest damage value
            bestDamage = damages.values().stream()
                    .max(Float::compareTo)
                    .orElse(0f);

            if (dmgCheck()) {
                AnchorPos.add(pos);
                logDebug(String.valueOf(AnchorPos.size()));

                // set pos to the highest, closest damage position
                if (!AnchorPos.isEmpty()) {
                    // get the closest position to the target
                    for (PlayerEntity entity : entities) {
                        this.pos = AnchorPos.stream()
                                .min(Comparator.comparingDouble(value ->
                                        entity.getPos().distanceTo(new Vec3d(value.getX(), value.getY(), value.getZ())))).orElse(null);
                    }
                }
            }
        }, 1, java.util.concurrent.TimeUnit.MILLISECONDS));
        sphere.clear();
    }


    private void logDebug(String message, Object... args) {
        if (debugCalculations.get()) {
            info(message, args);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (broken) {
            resetDamageValues.run();
            AnchorPos.clear();
            this.pos = null;
            broken = false;
        }

        if (executor == null || executor.isShutdown() || executor.isTerminated()) {
            executor = new ScheduledThreadPoolExecutor(threads.get());
        }

        try {
            resetDamageValues.run();
            if (mc.world.getDimension().respawnAnchorWorks()) {
                error("You are in the wrong dimension!");
                toggle();
                return;
            }

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUpdateTime < (1000 / updateSpeed.get())) return;

            PlayerEntity player = CombatUtils.filter(mc.world.getPlayers(), targetMode.get(), targetRange.get());
            if (player == null) return;

            start = player.getPos();
            entities.add(player);

            calculate(start);
            if (!AnchorPos.isEmpty() && pos != null) {
                //set pos to best damage position
                doBreak.run();
            }
            lastUpdateTime = currentTime;
        } catch (Exception e) {
            error("An error occurred while updating the module: " + e.getMessage());
            Addon.Logger.error("An error occurred while updating the module: {}", Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        }
    }

    private void breakAnchor() {
        try {
            if (updateEat()) return;

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastPlacedTime < (1000 / anchorSpeed.get())) return;
            if (mc.player == null || mc.player.getHealth() <= pauseHealth.get()) return;

            FindItemResult stone = swap.get() == swapUtils.swapMode.silent || swap.get() == swapUtils.swapMode.normal
                    ? InvUtils.findInHotbar(Items.GLOWSTONE)
                    : InvUtils.find(Items.GLOWSTONE);

            FindItemResult anchor = swap.get() == swapUtils.swapMode.silent || swap.get() == swapUtils.swapMode.normal
                    ? InvUtils.findInHotbar(Items.RESPAWN_ANCHOR)
                    : InvUtils.find(Items.RESPAWN_ANCHOR);

            if (!stone.found() || !anchor.found()) {
                error("Invalid items in inventory");
                return;
            }

            if (debugBreak.get()) info("breaking anchor at: " + WorldUtils.getCoords(pos));
            if (!WorldUtils.isAir(pos, false)) return;

            WorldUtils.placeBlock(anchor, pos, swingMode.get(), directionMode.get(), packetPlace.get(), swap.get(), rotate.get());

            if (mc.world.getBlockState(pos).getBlock() == Blocks.RESPAWN_ANCHOR) {
                WorldUtils.placeBlock(stone, pos, swingMode.get(), directionMode.get(), packetPlace.get(), swap.get(), rotate.get());
            }

            if (!WorldUtils.isAir(pos, liquidPlace.get())) {
                if (mc.world.getBlockState(pos).get(RespawnAnchorBlock.CHARGES) == 1) {
                    WorldUtils.placeBlock(anchor, pos, swingMode.get(), directionMode.get(), packetPlace.get(), swap.get(), rotate.get());
                }
            }

            broken = true;
            lastPlacedTime = currentTime;
        } catch (Exception e) {
            error("An error occurred while breaking the anchor: " + e.getMessage());
        }
    }

    private boolean updateEat() {
        return pauseUse.get() && mc.player.isUsingItem();
    }

    @EventHandler
    public void render(Render3DEvent event) {
        try {
            for (PlayerEntity entity : entities) {
                if (entity == mc.player || Friends.get().isFriend(entity) || mc.player.distanceTo(entity) > targetRange.get()) {
                    continue;
                }

                if (pos != null) {
                    switch (renderMode.get()) {
                        case normal -> event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                        case fading ->
                                RenderUtils.renderTickingBlock(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0, duration.get(), true, false);
                        case smooth -> {
                            if (renderBoxOne == null) {
                                renderBoxOne = new Box(pos);
                            }
                            if (renderBoxTwo == null) {
                                renderBoxTwo = new Box(pos);
                            }
                            if (renderBoxTwo instanceof IBox) {
                                ((IBox) renderBoxTwo).meteor$set(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
                            }
                            double offsetX = (renderBoxTwo.minX - renderBoxOne.minX) / Smoothness.get();
                            double offsetY = (renderBoxTwo.minY - renderBoxOne.minY) / Smoothness.get();
                            double offsetZ = (renderBoxTwo.minZ - renderBoxOne.minZ) / Smoothness.get();
                            ((IBox) renderBoxOne).meteor$set(
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
            }
        } catch (Exception e) {
            error("An error occurred while rendering the anchor positions: " + e.getMessage());
        }
    }

    @Override
    public String getInfoString() {
        for (PlayerEntity entity1 : entities) {
            return entity1.getName().getString();
        }
        return null;
    }

    public enum RenderMode {
        fading,
        normal,
        smooth
    }
}