package dev.seasnail1.modules.render;

import dev.seasnail1.Addon;
import dev.seasnail1.utilities.CombatUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

public class BurrowEsp extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder().name("ignore-friends").description("Ignores friends.").defaultValue(true).build());

    private final Setting<Integer> maxPlayers = sgGeneral.add(new IntSetting.Builder().name("max-players").description("The maximum amount of burrowed players to render.").sliderRange(1, 25).min(1).defaultValue(5).build());
    private final Setting<Integer> threadCount = sgGeneral.add(new IntSetting.Builder().name("thread-count").description("The amount of threads to use for finding burrowed players.").defaultValue(1).sliderRange(1, 10).build());
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder().name("range").description("The range to render burrowed players.").defaultValue(4.0).sliderRange(0.0, 35.0).build());
    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("The shape mode of the box.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder().name("side-color").description("Side color.").defaultValue(new SettingColor(255, 0, 0, 75)).build());
    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder().name("line-color").description("Line color.").defaultValue(new SettingColor(255, 0, 0, 255)).build());

    public BurrowEsp() {
        super(Addon.CATEGORY, "Burrow-ESP", "Highlights players that are in burrows (inside blocks)");
    }

    private final ConcurrentHashMap<Integer, PlayerEntity> burrowedPlayers = new ConcurrentHashMap<>();
    private ScheduledExecutorService executorService;

    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL = 100;

    @Override
    public void onActivate() {
        burrowedPlayers.clear();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        executorService = Executors.newScheduledThreadPool(threadCount.get());
    }

    @Override
    public void onDeactivate() {
        burrowedPlayers.clear();
        if (executorService != null) {
            executorService.shutdown();
            executorService = null;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime < UPDATE_INTERVAL) return;
        lastUpdateTime = currentTime;

        try {
            if (executorService == null || executorService.isShutdown()) {
                executorService = Executors.newScheduledThreadPool(threadCount.get());
            }

            executorService.submit(() -> {
                Set<Integer> playersToRemove = new HashSet<>(burrowedPlayers.keySet());
                int count = 0;

                for (PlayerEntity player : mc.world.getPlayers()) {
                    if (count >= maxPlayers.get()) break;

                    int id = player.getId();

                    if (player == mc.player) continue;
                    if (ignoreFriends.get() && Friends.get().isFriend(player)) continue;
                    if (!CombatUtils.isBurrowed(player)) continue;
                    if (player.squaredDistanceTo(mc.player) > range.get() * range.get()) continue;

                    playersToRemove.remove(id);

                    if (!burrowedPlayers.containsKey(id)) {
                        burrowedPlayers.put(id, player);
                        count++;
                    }
                }

                playersToRemove.forEach(burrowedPlayers::remove);
            });
        } catch (Exception e) {
            error("BurrowESP error: " + e.getMessage());
        }
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        for (PlayerEntity player : burrowedPlayers.values()) {
            if (player == null || player.isRemoved()) continue;
            event.renderer.box(player.getBlockPos(), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }
}
