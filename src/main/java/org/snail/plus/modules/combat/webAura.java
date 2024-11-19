package org.snail.plus.modules.combat;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IBox;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.snail.plus.Addon;
import org.snail.plus.utils.CombatUtils;
import org.snail.plus.utils.WorldUtils;
import org.snail.plus.utils.extrapolationUtils;
import org.snail.plus.utils.swapUtils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class webAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final SettingGroup sgExtrapolation = settings.createGroup("Extrapolation");
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgMisc = settings.createGroup("Misc");

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("The radius in which to target players.")
            .sliderRange(0, 10)
            .defaultValue(4.5)
            .build());

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Automatically faces the target.")
            .defaultValue(true)
            .build());

    private final Setting<Double> updateTime = sgGeneral.add(new DoubleSetting.Builder()
            .name("update-time")
            .description("The time in seconds between each update. Higher values may cause lag.")
            .defaultValue(1)
            .sliderRange(0, 100)
            .build());

    private final Setting<Boolean> doublePlace = sgPlace.add(new BoolSetting.Builder()
            .name("double-place")
            .description("Places two webs instead of one.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> airPlace = sgPlace.add(new BoolSetting.Builder()
            .name("air-place")
            .description("Places webs in the air.")
            .defaultValue(false)
            .build());

    private final Setting<swapUtils.swapMode> swapMode = sgPlace.add(new EnumSetting.Builder<swapUtils.swapMode>()
            .name("swap-mode")
            .description("The mode to use when swapping items.")
            .defaultValue(swapUtils.swapMode.silent)
            .build());

    private final Setting<Double> speed = sgPlace.add(new DoubleSetting.Builder()
            .name("speed")
            .description("The speed at which to place webs.")
            .defaultValue(10)
            .sliderRange(0, 100)
            .build());

    private final Setting<Boolean> strictDirection = sgPlace.add(new BoolSetting.Builder()
            .name("strict-direction")
            .description("Only places webs in the direction you're facing.")
            .defaultValue(false)
            .build());

    private final Setting<WorldUtils.DirectionMode> direction = sgPlace.add(new EnumSetting.Builder<WorldUtils.DirectionMode>()
            .name("direction")
            .description("The direction to place webs.")
            .defaultValue(WorldUtils.DirectionMode.Down)
            .visible(() -> !strictDirection.get())
            .build());

    private final Setting<Boolean> onlySurround = sgPlace.add(new BoolSetting.Builder()
            .name("only surrounded")
            .description("Only targets players that are surrounded by blocks.")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> predictMovement = sgExtrapolation.add(new BoolSetting.Builder()
            .name("predict movement")
            .description("Predicts the movement of players for more accurate web placement.")
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
            .name("render extrapolation")
            .description("Renders the extrapolated position of the player.")
            .defaultValue(true)
            .visible(() -> predictMovement.get())
            .build());

    private final Setting<renderMode> mode = sgRender.add(new EnumSetting.Builder<renderMode>()
            .name("mode")
            .description("The render mode of the web.")
            .defaultValue(renderMode.smooth)
            .build());

    private final Setting<Integer> Smoothness = sgRender.add(new IntSetting.Builder()
            .name("smoothness")
            .description("The smoothness of the web.")
            .defaultValue(10)
            .sliderRange(0, 20)
            .visible(() -> mode.get() == renderMode.smooth)
            .build());

    private final Setting<Integer> fadeTimer = sgRender.add(new IntSetting.Builder()
            .name("fade-timer")
            .description("The time in ticks before the web fades.")
            .defaultValue(10)
            .sliderRange(0, 20)
            .visible(() -> mode.get() == renderMode.fade)
            .build());

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The side color of the web.")
            .defaultValue(new SettingColor(0, 255, 0, 50))
            .build());

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The line color of the web.")
            .defaultValue(new SettingColor(0, 255, 0, 255))
            .build());

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shape is rendered.")
            .defaultValue(ShapeMode.Both)
            .build());

    private final Setting<WorldUtils.HandMode> hand = sgMisc.add(new EnumSetting.Builder<WorldUtils.HandMode>()
            .name("hand")
            .description("The hand to place webs with.")
            .defaultValue(WorldUtils.HandMode.MainHand)
            .build());

    private final ReentrantLock lock = new ReentrantLock();
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private List<BlockPos> pos = new ArrayList<>();
    private boolean placed;
    private Box renderBoxOne, renderBoxTwo;
    private long lastPlacedTime;
    private long lastUpdateTime;

    public webAura() {
        super(Addon.Snail, "web Aura+", "uses webs to slow down enemies");
    }

    @Override
    public void onActivate() {
        try {
            if (executor == null || executor.isShutdown() || executor.isTerminated()) {
                executor = Executors.newSingleThreadExecutor();
            }
            executor.submit(() -> onTick(null));
        } catch (Exception e) {
            Addon.LOG.error(List.of(e.getStackTrace()).toString());
        }
    }

    @Override
    public void onDeactivate() {
        try {
            if (executor != null && !executor.isShutdown()) {
                executor.shutdown();
            }
            lastPlacedTime = 0;
            lastUpdateTime = 0;
        } catch (Exception e) {
            Addon.LOG.error(List.of(e.getStackTrace()).toString());
        }
    }

    protected List<BlockPos> positions(PlayerEntity entity) {
        lock.lock();
        try {
            if (predictMovement.get()) {
                pos = Collections.singletonList(BlockPos.ofFloored(extrapolationUtils.predictEntityVe3d(entity, selfExtrapolateTicks.get())));
                Box box = new Box(pos.getFirst());
                List<VoxelShape> count = new ArrayList<>();
                mc.world.getBlockCollisions(entity, box).forEach(count::add);
                if (!count.isEmpty()) {
                    return Collections.singletonList(entity.getBlockPos());
                } else {
                    return pos;
                }
            } else {
                return Collections.singletonList(entity.getBlockPos());
            }
        } catch (Exception e) {
            Addon.LOG.error(List.of(e.getStackTrace()).toString());
            return Collections.emptyList();
        } finally {
            lock.unlock();
        }
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        lock.lock();
        try {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUpdateTime < (1000 / updateTime.get())) return;
            for (PlayerEntity entity : mc.world.getPlayers()) {
                if (entity == mc.player || entity.isDead() || entity.distanceTo(mc.player) > range.get() || Friends.get().isFriend(entity)) continue;
                if(onlySurround.get() && !CombatUtils.isSurrounded(entity)) continue;
                List<BlockPos> positions = positions(entity);
                for (BlockPos blockPos : positions) {
                    if(CombatUtils.isBurrowed(entity)) continue;
                    placed = !WorldUtils.isAir(blockPos);

                    if (WorldUtils.isAir(blockPos.down(1)) && !airPlace.get()) continue;

                    placeWeb(blockPos);

                    if (doublePlace.get()) {
                        placeWeb(blockPos.up(1));
                    }
                }
            }
            lastUpdateTime = currentTime;
        } catch (Exception e) {
            Addon.LOG.error(List.of(e.getStackTrace()).toString());
        } finally {
            lock.unlock();
        }
    }

    public void placeWeb(BlockPos pos) {
        lock.lock();
        try {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastPlacedTime < (1000 / speed.get())) return;
            if (!placed ) {
                FindItemResult web = InvUtils.findInHotbar(Items.COBWEB);
                if(web.found()) {
                    WorldUtils.placeBlock(web, pos, hand.get(), direction.get(), true, swapMode.get(), rotate.get());
                }
            }
            lastPlacedTime = currentTime;
        } catch (Exception e) {
            Addon.LOG.error(List.of(e.getStackTrace()).toString());
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings({"DuplicatedCode"})
    @EventHandler
    public void onRender(Render3DEvent event) {
        try {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (CombatUtils.isBurrowed(player)) continue;
                for (BlockPos pos : positions(player)) {
                    if (player == mc.player || player.isDead() || Friends.get().isFriend(player)) continue;
                    if (predictMovement.get() && renderExtrapolation.get()) {
                        Box playerBox = getBox(player, pos);
                        event.renderer.box(playerBox, sideColor.get(), lineColor.get(), ShapeMode.Both, 0);
                    }

                    switch (mode.get()) {
                        case normal -> event.renderer.box(pos, sideColor.get(), lineColor.get(), ShapeMode.Both, 0);

                        case smooth -> {
                            if (renderBoxOne == null) renderBoxOne = new Box(pos);
                            if (renderBoxTwo == null) renderBoxTwo = new Box(pos);

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
                                    renderBoxOne.maxZ + offsetZ
                            );
                            event.renderer.box(renderBoxOne, sideColor.get(), lineColor.get(), shapeMode.get(), 0);

                        }
                        case fade -> {
                            boolean shouldFade = player.isDead() || WorldUtils.isAir(pos);
                            RenderUtils.renderTickingBlock(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0, fadeTimer.get(), shouldFade, false);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Addon.LOG.error(List.of(e.getStackTrace()).toString());
        }
    }

    private static @NotNull Box getBox(PlayerEntity player, BlockPos pos) {
        Vec3d extrapolatedPos = new Vec3d(pos.getX(), pos.getY(), pos.getZ());
        return  new Box(
                extrapolatedPos.x - player.getWidth() / 2,
                extrapolatedPos.y,
                extrapolatedPos.z - player.getWidth() / 2,
                extrapolatedPos.x + player.getWidth() / 2,
                extrapolatedPos.y + player.getHeight(),
                extrapolatedPos.z + player.getWidth() / 2
        );
    }

    public enum renderMode {
        normal,
        smooth,
        fade
    }
}