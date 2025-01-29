package org.seasnail1.modules.combat;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.seasnail1.Addon;
import org.seasnail1.utilities.CombatUtils;
import org.seasnail1.utilities.WorldUtils;
import org.seasnail1.utilities.swapUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class webAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlace = settings.createGroup("Place");
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

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("render")
            .description("Renders the webs.")
            .defaultValue(true)
            .build());

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The side color of the web.")
            .defaultValue(new SettingColor(0, 255, 0, 50))
            .visible(render::get)
            .build());

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The line color of the web.")
            .defaultValue(new SettingColor(0, 255, 0, 255))
            .visible(render::get)
            .build());

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shape is rendered.")
            .defaultValue(ShapeMode.Both)
            .visible(render::get)
            .build());

    private final Setting<WorldUtils.HandMode> hand = sgMisc.add(new EnumSetting.Builder<WorldUtils.HandMode>()
            .name("hand")
            .description("The hand to place webs with.")
            .defaultValue(WorldUtils.HandMode.MainHand)
            .build());

    private final ReentrantLock lock = new ReentrantLock();
    private boolean placed;
    private Box renderBoxOne, renderBoxTwo;
    private long lastPlacedTime;
    private long lastUpdateTime;
    private PlayerEntity BestTarget;

    public webAura() {
        super(Addon.CATEGORY, "web-aura", "Places cobwebs at players feet to slow them down");
    }

    @Override
    public void onActivate() {
        lastPlacedTime = 0;
        lastUpdateTime = 0;
    }

    @Override
    public void onDeactivate() {
        lastPlacedTime = 0;
        lastUpdateTime = 0;
    }

    protected List<BlockPos> positions(PlayerEntity entity) {
        return Stream.of(entity.getBlockPos())
                .filter(pos -> !CombatUtils.isBurrowed(entity) && (airPlace.get() || !WorldUtils.isAir(pos, false)))
                .collect(Collectors.toList());
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        lock.lock();
        try {
            try {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastUpdateTime < (1000 / updateTime.get())) return;
                for (PlayerEntity entity : mc.world.getPlayers()) {
                    if (entity == mc.player || entity.isDead() || entity.distanceTo(mc.player) > range.get() || Friends.get().isFriend(entity))
                        continue;
                    if (onlySurround.get() && !CombatUtils.isSurrounded(entity)) continue;

                    BestTarget = entity;
                    for (BlockPos blockPos : positions(BestTarget)) {
                        placed = !WorldUtils.isAir(blockPos, false);
                        placeWeb(blockPos);
                        if (doublePlace.get()) {
                            placeWeb(blockPos.up(1));
                        }
                    }
                }
                lastUpdateTime = currentTime;
            } finally {
                lock.unlock();
            }
        } catch (Exception e) {
            error("An error occurred while placing webs: " + e.getMessage());
            Addon.Logger.error("An error occurred while placing webs: {}", Arrays.toString(e.getStackTrace()));
        }
    }

    public void placeWeb(BlockPos pos) {
        lock.lock();
        try {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastPlacedTime < (1000 / speed.get())) return;
            if (!placed) {
                FindItemResult web = InvUtils.find(Items.COBWEB);
                if (!web.found()) {
                    toggle();
                    return;
                }
                WorldUtils.placeBlock(web, pos, hand.get(), direction.get(), true, swapMode.get(), rotate.get());
            }
            lastPlacedTime = currentTime;
        } finally {
            lock.unlock();
        }
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (render.get() && BestTarget != null) {
            for (BlockPos pos : positions(BestTarget)) {
                event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            }
        }
    }
}