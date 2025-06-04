package dev.seasnail1.modules.render;

import dev.seasnail1.Addon;
import dev.seasnail1.utilities.CombatUtils;
import dev.seasnail1.utilities.MathHelper;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class BurrowEsp extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-friends")
            .description("Ignores friends.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> performance = sgGeneral.add(new BoolSetting.Builder()
            .name("performance")
            .description("Improves performance")
            .defaultValue(true)
            .build());

    private final Setting<Integer> threadCount = sgGeneral.add(new IntSetting.Builder()
            .name("thread-count")
            .description("The amount of threads to use for finding burrowed players.")
            .defaultValue(1)
            .sliderRange(1, 10)
            .build());

    private final Setting<Integer> maxPlayers = sgGeneral.add(new IntSetting.Builder()
            .name("max-players")
            .description("The maximum amount of burrowed players to render.")
            .sliderRange(1, 10)
            .min(1)
            .defaultValue(5)
            .visible(performance::get)
            .build());

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("The range to render burrowed players.")
            .defaultValue(4.0)
            .sliderRange(0.0, 35.0)
            .build());

    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("The shape mode of the box.")
            .defaultValue(ShapeMode.Both)
            .build());

    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder()
            .name("side-color")
            .description("Side color.")
            .defaultValue(new SettingColor(255, 0, 0, 75))
            .build());

    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
            .name("line-color")
            .description("Line color.")
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .build());

    public BurrowEsp() {
        super(Addon.CATEGORY, "Burrow-ESP", "Highlights players that are in burrows (inside blocks)");
    }

    Set<PlayerEntity> burrowedPlayers = new HashSet<>();
    ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(threadCount.get());

    Runnable reset = () -> {
        burrowedPlayers.clear();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    };

    @Override
    public void onActivate() {
        reset.run();
    }

    @Override
    public void onDeactivate() {
        reset.run();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        try {
            if (executorService == null || executorService.isShutdown() || executorService.isTerminated()) {
                executorService = new ScheduledThreadPoolExecutor(threadCount.get());
            }

            executorService.submit(() -> {
                burrowedPlayers.clear();
                if (mc.world == null || mc.player == null) return;

                mc.world.getPlayers().stream()
                        .filter(player -> CombatUtils.isBurrowed(player) && mc.player.distanceTo(player) <= range.get() &&
                                burrowedPlayers.stream().noneMatch(p -> p.getBlockPos().equals(player.getBlockPos())) &&
                                !ignoreFriends.get() || !Friends.get().isFriend(player))
                        .limit(performance.get() ? maxPlayers.get() : Long.MAX_VALUE)
                        .forEach(burrowedPlayers::add);
            });
        } catch (Exception e) {
            error("An error occurred while finding burrowed players: " + e.getMessage());
            Addon.Logger.error("An error occurred while finding burrowed players: ", e);
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        synchronized (this) {
            burrowedPlayers.forEach(player -> {
                BlockPos blockPos = BlockPos.ofFloored(player.getX(), player.getY() + 0.4, player.getZ());
                if (performance.get() && MathHelper.rayCast(Vec3d.of(blockPos.up(1)))) return;
                event.renderer.box(blockPos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            });
        }
    }

    @Override
    public String getInfoString() {
        return String.valueOf(burrowedPlayers.size());
    }
}
