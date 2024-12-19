package org.snail.plus.modules.combat;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.BowSpam;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.RailBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.snail.plus.Addon;
import org.snail.plus.utilities.CombatUtils;
import org.snail.plus.utilities.MathUtils;
import org.snail.plus.utilities.WorldUtils;
import org.snail.plus.utilities.swapUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class minecartAura extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<CombatUtils.filterMode> filterMode = sgGeneral.add(new EnumSetting.Builder<CombatUtils.filterMode>()
            .name("filter-mode")
            .description("The filter mode.")
            .defaultValue(CombatUtils.filterMode.Closet)
            .build());

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Automatically rotates towards the position where the anvil is placed.")
            .defaultValue(true)
            .build());

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
            .name("speed")
            .description("The speed to place blocks")
            .defaultValue(10)
            .build());

    private final Setting<Integer> bowDelay = sgGeneral.add(new IntSetting.Builder()
            .name("bow-delay")
            .description("The delay between each bow shot.")
            .defaultValue(500)
            .sliderRange(0, 1000)
            .build());

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("The range to place blocks")
            .defaultValue(5)
            .sliderRange(1, 10)
            .build());

    private final Setting<swapUtils.swapMode> swapMode = sgGeneral.add(new EnumSetting.Builder<swapUtils.swapMode>()
            .name("swap-mode")
            .description("The mining mode.")
            .defaultValue(swapUtils.swapMode.silent)
            .build());

    private final Setting<Integer> maxMinecarts = sgGeneral.add(new IntSetting.Builder()
            .name("max-minecarts")
            .defaultValue(4)
            .sliderRange(1, 100)
            .build());

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("render")
            .description("Renders the block being mined.")
            .defaultValue(true)
            .build());

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The side color.")
            .defaultValue(new SettingColor(255, 255, 255, 75))
            .build());

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The line color.")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .build());
    long lastPlacedTime = 0;
    //super easy way to shoot arrows
    Module bowSpam = Modules.get().get(BowSpam.class);
    private BlockPos position;
    private int minecartCount = 0;
    private boolean isPlacing;
    private boolean maxMinecartsReached;
    private final Runnable reset = () -> {
        if (bowSpam.isActive()) {
            bowSpam.toggle();
        }
        position = null;
        minecartCount = 0;
        isPlacing = false;
        maxMinecartsReached = false;
    };

    public minecartAura() {
        super(Addon.Snail, "minecart-aura", "Automatically blows up minecarts around targets.");
    }

    @Override
    public void onActivate() {
        reset.run();
    }

    @Override
    public void onDeactivate() {
        reset.run();
    }

    private BlockPos findPositions(PlayerEntity bestTarget) {
        return bestTarget.getBlockPos();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        synchronized (this) {
            mc.executeSync(() -> {
                if (mc.world != null) {
                    PlayerEntity bestTarget = CombatUtils.filter(mc.world.getPlayers(), filterMode.get(), range.get());
                    if (bestTarget != null) {
                        position = findPositions(bestTarget);
                    }

                    if (position != null) {
                        useMinecart(position);
                    }
                }
            });
        }
    }

    private void useMinecart(BlockPos pos) {
        FindItemResult minecart = InvUtils.findInHotbar(Items.TNT_MINECART);
        FindItemResult rail = InvUtils.findInHotbar(Items.RAIL);

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPlacedTime < (1000 / speed.get())) return;

        if (minecart.found() && rail.found()) {
            if (!maxMinecartsReached) {
                isPlacing = true;
                if (rotate.get()) {
                    Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), 100);
                }
                WorldUtils.placeBlock(rail, pos, WorldUtils.HandMode.MainHand, WorldUtils.DirectionMode.Down, true, swapMode.get(), rotate.get());
                WorldUtils.placeBlock(minecart, pos, WorldUtils.HandMode.MainHand, WorldUtils.DirectionMode.Down, true, swapMode.get(), rotate.get());

                isPlacing = false;
                minecartCount++;

                if (minecartCount >= maxMinecarts.get()) {
                    maxMinecartsReached = true;
                    shootArrow();
                }
            } else {
                shootArrow();
            }
        }
    }

    private void shootArrow() {
        if (isPlacing) return; // Prevent shooting while placing

        FindItemResult bow = InvUtils.findInHotbar(Items.BOW);
        if (!bow.found()) return;

        InvUtils.swap(bow.slot(), false);

        if (!bowSpam.isActive()) {
            bowSpam.toggle();
            info("Shooting arrows");
            Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                if (bowSpam.isActive()) {
                    Rotations.rotate(Rotations.getYaw(position), Rotations.getPitch(position), 100, () -> {
                        bowSpam.toggle();
                        maxMinecartsReached = false; // Reset after shooting
                        minecartCount = 0; // Reset minecart count
                    });

                }
            }, bowDelay.get(), TimeUnit.MILLISECONDS); // Adjust the delay as needed
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (render.get() && position != null) {
            event.renderer.box(position, sideColor.get(), lineColor.get(), ShapeMode.Both, 0);
        }
    }
}