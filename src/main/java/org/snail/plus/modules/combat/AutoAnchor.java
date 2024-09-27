package org.snail.plus.modules.combat;

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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.snail.plus.Addon;
import org.snail.plus.utils.CombatUtils;
import org.snail.plus.utils.WorldUtils;
import org.snail.plus.utils.extrapolationUtils;
import org.snail.plus.utils.swapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author seasnail1
 */

public class AutoAnchor extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlacement = settings.createGroup("Placement");
    private final SettingGroup sgAutomation = settings.createGroup("Automation");
    private final SettingGroup sgExtrapolation = settings.createGroup("Extrapolation");
    private final SettingGroup sgDamage = settings.createGroup("Damage");
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgMisc = settings.createGroup("Misc");
    private final SettingGroup sgDebug = settings.createGroup("Debug");

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("The maximum distance to target players for anchor placement.")
            .defaultValue(3.0)
            .sliderRange(1.0, 10.0)
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
            .sliderRange(0.1, 10.0)
            .build());

    private final Setting<Boolean> rotate = sgPlacement.add(new BoolSetting.Builder()
            .name("rotation")
            .description("Enables rotation towards the block when placing anchors.")
            .defaultValue(false)
            .build());

    private final Setting<Double> anchorSpeed = sgPlacement.add(new DoubleSetting.Builder()
            .name("anchor speed")
            .description("The speed at which anchors are placed, in anchors per second.")
            .defaultValue(1.0)
            .sliderRange(0.1, 10.0)
            .build());

    private final Setting<swapUtils.swapMode> swap = sgPlacement.add(new EnumSetting.Builder<swapUtils.swapMode>()
            .name("swap mode")
            .description("The mode used for swapping items when placing anchors.")
            .defaultValue(swapUtils.swapMode.Inventory)
            .build());

    private final Setting<Double> maxSelfDamage = sgDamage.add(new DoubleSetting.Builder()
            .name("max self damage")
            .description("The maximum amount of damage you can take from your own anchors.")
            .defaultValue(3.0)
            .sliderRange(0.0, 36.0)
            .build());

    private final Setting<Double> minDamage = sgDamage.add(new DoubleSetting.Builder()
            .name("min damage")
            .description("The minimum amount of damage that should be dealt to the target.")
            .defaultValue(3.0)
            .sliderRange(0.0, 36.0)
            .build());

    private final Setting<Double> pauseHealth = sgDamage.add(new DoubleSetting.Builder()
            .name("pause health")
            .description("Pauses the module when your health is below this value.")
            .defaultValue(0.0)
            .sliderRange(0.0, 36.0)
            .build());

    private final Setting<Double> lethalHealth = sgDamage.add(new DoubleSetting.Builder()
            .name("lethal health")
            .description("overrides pause health if you can pop the player")
            .defaultValue(0.0)
            .sliderRange(0.0, 36.0)
            .build());

    private final Setting<Boolean> strictDirection = sgPlacement.add(new BoolSetting.Builder()
            .name("strict direction")
            .description("Only places anchors in the direction you are facing.")
            .defaultValue(false)
            .build());

    private final Setting<WorldUtils.DirectionMode> directionMode = sgPlacement.add(new EnumSetting.Builder<WorldUtils.DirectionMode>()
            .name("direction")
            .description("The mode used for direction.")
            .defaultValue(WorldUtils.DirectionMode.Up)
            .visible(() -> !strictDirection.get())
            .build());

    //automation idea from https://github.com/AntiCope/orion/blob/master/src/main/java/me/ghosttypes/orion/modules/main/AnchorAura.java

    private final Setting<Boolean> breakSelfTrap = sgAutomation.add(new BoolSetting.Builder()
            .name("break self trap")
            .description("Breaks players selftrap")
            .defaultValue(false)
            .build());

    private final Setting<Double> fastPlaceDelay = sgAutomation.add(new DoubleSetting.Builder()
            .name("fast place speed")
            .description("the speed at which to replace the broken block with an anchor")
            .defaultValue(0.0)
            .sliderRange(0.0, 10.0)
            .visible(() -> breakSelfTrap.get())
            .build());

    private final Setting<Boolean> breakBurrow = sgAutomation.add(new BoolSetting.Builder()
            .name("break burrow")
            .description("Breaks players burrow")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> predictMovement = sgExtrapolation.add(new BoolSetting.Builder()
            .name("predict movement")
            .description("Predicts the movement of players for more accurate anchor placement.")
            .defaultValue(true)
            .build());

    private final Setting<Integer> extrapolationTicks = sgExtrapolation.add(new IntSetting.Builder()
            .name("extrapolation ticks")
            .description("The amount of ticks to extrapolate player movement.")
            .defaultValue(1)
            .sliderRange(0, 20)
            .visible(() -> predictMovement.get())
            .build());

    private final Setting<Integer> selfExtrapolateTicks = sgExtrapolation.add(new IntSetting.Builder()
            .name("self extrapolation ticks")
            .description("The amount of ticks to extrapolate your movement.")
            .defaultValue(1)
            .sliderRange(0, 20)
            .visible(() -> predictMovement.get())
            .build());

    private final Setting<Boolean> renderExtrapolation = sgRender.add(new BoolSetting.Builder()
            .name("render Extrapolation")
            .description("Renders the Extrapolation of target.")
            .defaultValue(true)
            .visible(() -> predictMovement.get())
            .build());

    private final Setting<Integer> RadiusX = sgPlacement.add(new IntSetting.Builder()
            .name("Radius X")
            .description("The radius in the X direction for anchor placement.")
            .defaultValue(1)
            .sliderRange(1, 5)
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

    private final Setting<Integer> RadiusZ = sgPlacement.add(new IntSetting.Builder()
            .name("Radius Z")
            .description("The radius in the Z direction for anchor placement.")
            .defaultValue(1)
            .sliderRange(1, 5)
            .build());

    private final Setting<Boolean> swing = sgPlacement.add(new BoolSetting.Builder()
            .name("swing")
            .description("Swings your hand.")
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

    private final Setting<WorldUtils.HandMode> swingMode = sgPlacement.add(new EnumSetting.Builder<WorldUtils.HandMode>()
            .name("swing mode")
            .description("The mode used for swinging your hand.")
            .defaultValue(WorldUtils.HandMode.MainHand)
            .visible(() -> swing.get())
            .build());

    private final Setting<Boolean> shrink = sgRender.add(new BoolSetting.Builder()
            .name("fade shrink")
            .description("Enables shrinking of the anchor box during fading render mode.")
            .defaultValue(true)
            .visible(() -> renderMode.get() == RenderMode.fading)
            .build());

    private final Setting<Integer> Smoothness = sgRender.add(new IntSetting.Builder()
            .name("smoothness")
            .description("The smoothness of the anchor box rendering in smooth mode.")
            .defaultValue(3)
            .sliderRange(1, 100)
            .visible(() -> renderMode.get() == RenderMode.smooth)
            .build());

    private final Setting<Boolean> pauseUse = sgMisc.add(new BoolSetting.Builder()
            .name("pause on use")
            .description("Pauses the module when you are using an item.")
            .defaultValue(false)
            .build());

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape mode")
            .description("The shape mode used for rendering the anchor box.")
            .defaultValue(ShapeMode.Both)
            .build());

    private final Setting<Boolean> debugExtrapolation = sgDebug.add(new BoolSetting.Builder()
            .name("debug extrapolation")
            .description("Enables debug information for extrapolation.")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> debugRender = sgDebug.add(new BoolSetting.Builder()
            .name("debug render")
            .description("Enables debug information for rendering.")
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

    private final ReentrantLock lock = new ReentrantLock();
    ExecutorService executor = Executors.newSingleThreadExecutor();
    private Box renderBoxOne, renderBoxTwo;
    private List<BlockPos> AnchorPos = new ArrayList<>();
    private long lastPlacedTime;
    private long lastUpdateTime;
    private double selfDamage;
    private double targetDamage;
    private Explosion explosion;

    public AutoAnchor() {
        super(Addon.Snail, "Anchor Aura+", "places and breaks respawn anchors around players");
    }

    @Override
    public void onActivate() {
        try {
            selfDamage = 0;
            targetDamage = 0;
            AnchorPos = new ArrayList<>();
            if (executor == null || executor.isShutdown() || executor.isTerminated()) {
                executor = Executors.newSingleThreadExecutor();
            }
            executor.submit(() -> onTick(null));
        } catch (Exception e) {
            error("An error occurred while activating the module: " + e.getMessage());
        }
    }

    @Override
    public void onDeactivate() {
        try {
            selfDamage = 0;
            targetDamage = 0;
            if (executor != null) {
                executor.shutdown();
            }
            if (AnchorPos != null) {
                AnchorPos = new ArrayList<>(AnchorPos);
                AnchorPos.clear();
            }
        } catch (Exception e) {
            error("An error occurred while deactivating the module: " + e.getMessage());
        }
        explosion = null;
    }

    public List<BlockPos> positions(PlayerEntity entity, double radius) {
        try {
            ArrayList<BlockPos> positions = new ArrayList<>();
            int Radius = (int) Math.ceil(radius);
            if(debugCalculations.get()) info("found radius " + Radius);
            for (int x = -Radius; x <= Radius; x++) {
                for (int y = -Radius; y <= Radius; y++) {
                    for (int z = -Radius; z <= Radius; z++) {
                        BlockPos pos = entity.getBlockPos().add(x, y, z);
                        if(debugCalculations.get()) info("found position " + pos + " checking damage...");
                        selfDamage = predictMovement.get() ? DamageUtils.anchorDamage(mc.player, predictMovement(entity, selfExtrapolateTicks.get())) : DamageUtils.anchorDamage(mc.player, Vec3d.of(pos));
                        targetDamage = predictMovement.get() ? DamageUtils.anchorDamage(entity, predictMovement(entity, extrapolationTicks.get())) : DamageUtils.anchorDamage(entity, Vec3d.of(pos));
                        if (WorldUtils.hitBoxCheck(entity, pos) && WorldUtils.isAir(pos) && selfDamage <= maxSelfDamage.get() && targetDamage >= minDamage.get() && !CombatUtils.willPop(entity, explosion)) {
                            positions.add(pos);
                            if(debugCalculations.get()) info("found position: " + pos);
                            if (debugCalculations.get()) {
                                info("found position: " + pos);
                                info("self damage: " + selfDamage);
                                info("target damage: " + targetDamage);
                            }
                        } else {
                            if (debugCalculations.get()) {
                                info("skipping position: " + pos);
                                info("self damage: " + selfDamage);
                                info("target damage: " + targetDamage);
                            }
                        }
                    }
                }
            }
            return positions.isEmpty() ? Collections.emptyList() : Collections.singletonList(positions.getFirst());
        } catch (Exception e) {
            error("An error occurred while finding positions: " + e.getMessage());
            return Collections.emptyList();
        }
    }


    /**
     * Predicts the future position of a player entity based on extrapolation ticks.
     *
     * @param player             The player entity whose movement is to be predicted.
     * @param extrapolationTicks The number of ticks to extrapolate the player's movement.
     * @return A Vec3d object representing the predicted position of the player.
     */
    private Vec3d predictMovement(PlayerEntity player, Integer extrapolationTicks) {
        if (debugExtrapolation.get()) info("predicting movement for: " + player);
        return extrapolationUtils.predictEntityPos(player, extrapolationTicks);
    }

    /**
     * Event handler for the tick event. This method is called every tick to perform the main logic of the AutoAnchor module.
     *
     * @param event The tick event.
     */
    @EventHandler
    private void onTick(TickEvent.Post event) {
        try {
            if (pauseUse.get() && (mc.player != null && mc.player.isUsingItem())) return;
            if (Objects.requireNonNull(mc.world).getDimension().respawnAnchorWorks()) {
                warning("You are in the wrong dimension!");
                return;
            }
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUpdateTime < (1000 / updateSpeed.get())) return;

            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player || Friends.get().isFriend(player) || mc.player.distanceTo(player) > range.get()) continue;

                AnchorPos = positions(player, Math.max(RadiusX.get(), RadiusZ.get()));
                for (BlockPos pos : AnchorPos) {
                    for (Direction dir : Direction.values()) {
                        if (strictDirection.get() && WorldUtils.strictDirection(pos.offset(dir), dir.getOpposite())) continue;
                    }
                }
                lock.lock();
                try {
                    for (BlockPos pos : AnchorPos) {
                        if (rotate.get()) Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), 100);
                        if(breakSelfTrap.get()) {
                            breakSelfTrap(player);
                        }
                        breakAnchor();
                    }
                } finally {
                    lock.unlock();
                }
                lastUpdateTime = currentTime;
            }
        } catch (Exception e) {
            error("An error occurred while processing the tick event: " + e.getMessage());
        }
    }

    public void breakAnchor() {
        lock.lock();
        try {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastPlacedTime < (1000 / anchorSpeed.get())) return;
            for (BlockPos pos : AnchorPos) {
                FindItemResult stone = InvUtils.findInHotbar(Items.GLOWSTONE);
                FindItemResult anchor = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
                if (!stone.found() || !anchor.found()) continue;
                if (mc.player.getHealth() <= pauseHealth.get()) continue;
                if (debugBreak.get()) info("breaking anchor at: " + pos);
                WorldUtils.placeBlock(anchor, pos, swingMode.get(), directionMode.get(), packetPlace.get(), swap.get(), rotate.get());
                WorldUtils.placeBlock(stone, pos, swingMode.get(), directionMode.get(), packetPlace.get(), swap.get(), rotate.get());
                WorldUtils.placeBlock(anchor, pos, swingMode.get(), directionMode.get(), packetPlace.get(), swap.get(), rotate.get());
            }
            lastPlacedTime = currentTime;
        } catch (Exception e) {
            error("An error occurred while breaking anchors: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    public void breakSelfTrap(PlayerEntity entity) {
        lock.lock();
        try {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastPlacedTime < (1000 / fastPlaceDelay.get())) return;
            if(CombatUtils.isTrapped(entity)) {
                FindItemResult pick = InvUtils.find(Items.DIAMOND_PICKAXE);
                if(pick.found() && mc.world.getBlockState(entity.getBlockPos().up(2)).getBlock() == Blocks.RESPAWN_ANCHOR) {
                    WorldUtils.breakBlock(entity.getBlockPos().up(2), swingMode.get(), directionMode.get(), packetPlace.get(), false, swap.get(), rotate.get());
                }
            }
        } catch (Exception e) {
            error("An error occurred while breaking anchors: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Event handler for rendering 3D events. This method is called to render the anchor positions.
     *
     * @param event The Render3DEvent that contains rendering information.
     */
    @EventHandler
    public void render(Render3DEvent event) {
        try {
        for (BlockPos pos : AnchorPos) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player || Friends.get().isFriend(player) || mc.player.distanceTo(player) > range.get())
                    continue;
                if (renderExtrapolation.get() && predictMovement.get()) {
                    Vec3d extrapolatedPos = predictMovement(player, extrapolationTicks.get());
                    Box playerBox = new Box(
                            extrapolatedPos.x - player.getWidth() / 2,
                            extrapolatedPos.y,
                            extrapolatedPos.z - player.getWidth() / 2,
                            extrapolatedPos.x + player.getWidth() / 2,
                            extrapolatedPos.y + player.getHeight(),
                            extrapolatedPos.z + player.getWidth() / 2
                    );
                    event.renderer.box(playerBox, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                }

                switch (renderMode.get()) {
                    case normal -> event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    case fading -> {
                        boolean shouldShrink = player.isDead() && shrink.get();
                        RenderUtils.renderTickingBlock(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0, rendertime.get(), true, shouldShrink);
                    }
                    case smooth -> {
                        if (renderBoxOne == null) renderBoxOne = new Box(pos);
                        if (renderBoxTwo == null) renderBoxTwo = new Box(pos);
                        if (debugRender.get()) info("rendering box: " + renderBoxOne);

                        if (renderBoxTwo instanceof IBox) {
                            if (debugRender.get()) info("setting render box to " + pos);
                            ((IBox) renderBoxTwo).set(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
                        }

                        double offsetX = (renderBoxTwo.minX - renderBoxOne.minX) / Smoothness.get();
                        double offsetY = (renderBoxTwo.minY - renderBoxOne.minY) / Smoothness.get();
                        double offsetZ = (renderBoxTwo.minZ - renderBoxOne.minZ) / Smoothness.get();
                        if (debugRender.get()) info("offsets: " + offsetX + " " + offsetY + " " + offsetZ);
                        ((IBox) renderBoxOne).set(
                                renderBoxOne.minX + offsetX,
                                renderBoxOne.minY + offsetY,
                                renderBoxOne.minZ + offsetZ,
                                renderBoxOne.maxX + offsetX,
                                renderBoxOne.maxY + offsetY,
                                renderBoxOne.maxZ + offsetZ
                        );
                        if (debugRender.get()) info("rendering box: " + renderBoxOne);
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
}