package org.snail.plus.modules.combat;


import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IBox;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.snail.plus.Addon;
import org.snail.plus.utils.WorldUtils;

import java.util.ArrayList;
import java.util.Objects;

public class AutoAnchor extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgDamage = settings.createGroup("Damage");
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgMisc = settings.createGroup("Misc");

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("Range to target player(s)")
            .defaultValue(3.0)
            .sliderMax(10.0)
            .sliderMin(1.0)
            .build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotation")
            .description("Rotates towards the block when placing.")
            .defaultValue(false)
            .build());
    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
            .name("target-priority")
            .description("How to filter targets within range.")
            .defaultValue(SortPriority.LowestDistance)
            .build());

    private final Setting<SwapMode> swapMethod = sgGeneral.add(new EnumSetting.Builder<SwapMode>()
            .name("swap mode")
            .description("swap mode. Silent is most consistent, but invswitch is more convenient")
            .defaultValue(SwapMode.silent)
            .build());

    private final Setting<Double> AnchorDelay = sgGeneral.add(new DoubleSetting.Builder()
            .name("anchor delay")
            .description("the anchor delay")
            .defaultValue(1.0)
            .min(0)
            .sliderMax(5)
            .build());
    private final Setting<Double> GlowstoneDelay = sgGeneral.add(new DoubleSetting.Builder()
            .name("glowstone delay")
            .description("the glowstone delay")
            .defaultValue(1.0)
            .min(0)
            .sliderMax(5)
            .build());
    private final Setting<Boolean> TpsSync = sgGeneral.add(new BoolSetting.Builder()
            .name("TPS sync")
            .description("syncs delay with current server tps")
            .defaultValue(true)
            .build());
    private final Setting<SafetyMode> safety = sgDamage.add(new EnumSetting.Builder<SafetyMode>()
            .name("safe-mode")
            .description("Safety mode")
            .defaultValue(SafetyMode.safe)
            .build());
    private final Setting<Double> maxSelfDamage = sgDamage.add(new DoubleSetting.Builder()
            .name("max self damage score")
            .description("the max amount to deal to you")
            .defaultValue(3.0)
            .sliderMax(36.0)
            .sliderMin(0.0)
            .visible(() -> safety.get() != SafetyMode.off)
            .build());
    private final Setting<Double> minDamage = sgDamage.add(new DoubleSetting.Builder()
            .name("min damage score")
            .description("the lowest amount of damage you should deal to the target (higher = less targets | lower = more targets)")
            .defaultValue(3.0)
            .sliderMax(36.0)
            .sliderMin(0.0)
            .visible(() -> safety.get() != SafetyMode.off)
            .build());
    private final Setting<Double> DamageRatio = sgDamage.add(new DoubleSetting.Builder()
            .name("damage ratio")
            .description("the ratio. min damage / maxself")
            .defaultValue(3.0)
            .sliderMax(36.0)
            .sliderMin(0.0)
            .visible(() -> safety.get() != SafetyMode.off)
            .visible(() -> safety.get() == SafetyMode.safe)
            .build());
    private final Setting<Boolean> strictDirection = sgGeneral.add(new BoolSetting.Builder()
            .name("strict direction")
            .description("Only places anchors in the direction you are facing")
            .defaultValue(false)
            .build());
    private final Setting<Boolean> placeSupport = sgGeneral.add(new BoolSetting.Builder()
            .name("place-support")
            .description("Whether to place support blocks.")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> predictMovement = sgGeneral.add(new BoolSetting.Builder()
            .name("movement predict")
            .description("predicts the targets movement")
            .defaultValue(false)
            .build());
    private final Setting<Boolean> Breaker = sgGeneral.add(new BoolSetting.Builder()
            .name("Breaker")
            .description("Breaks string and glowstone to prevent the anchor placements")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> Smart = sgGeneral.add(new BoolSetting.Builder()
            .name("smart")
            .description("Places respawn anchors at south, east, west, north. And places up top and below if the player is surrounded")
            .defaultValue(true)
            .build());

    private final Setting<Integer> RadiusZ = sgGeneral.add(new IntSetting.Builder()
            .name("Radius Z")
            .description("the radius for Z")
            .defaultValue(1)
            .sliderMax(5)
            .sliderMin(1)
            .visible(Smart::get)
            .build());
    private final Setting<Integer> RadiusX = sgGeneral.add(new IntSetting.Builder()
            .name("Radius X")
            .description("the radius for X")
            .defaultValue(1)
            .sliderMax(5)
            .sliderMin(1)
            .visible(Smart::get)
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
            .description("render mode. Smooth is cool")
            .defaultValue(RenderMode.smooth)
            .build());
    private final Setting<Integer> rendertime = sgRender.add(new IntSetting.Builder()
            .name("render time")
            .description("render time")
            .defaultValue(3)
            .sliderMax(100)
            .sliderMin(1)
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
            .sliderMax(100)
            .sliderMin(1)
            .visible(() -> renderMode.get() == RenderMode.smooth)
            .build());
    private final Setting<SwingMode> swingMode = sgMisc.add(new EnumSetting.Builder<SwingMode>()
            .name("swing type")
            .description("swing type")
            .defaultValue(SwingMode.mainhand)
            .build());
    private final Setting<Boolean> MultiTask = sgMisc.add(new BoolSetting.Builder()
            .name("multi-task")
            .description("allows you to use different items when the module is interacting / placing")
            .defaultValue(false)
            .build());
    private final Setting<Boolean> pauseUse = sgMisc.add(new BoolSetting.Builder()
            .name("pause on use")
            .description("pauses the module when you use a item")
            .defaultValue(false)
            .visible(() -> !MultiTask.get())
            .build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build());
    private boolean Swapped;
    private BlockPos[] AnchorPos;
    private Box renderBoxOne, renderBoxTwo;

    private Thread anchorThread;
    private volatile boolean isRunning = false;

    public AutoAnchor() {
        super(Addon.Snail, "Anchor Bomb+", "explodes anchors near targets");
    }

    @Override
    public void onActivate() {
        AnchorPos = null;
        isRunning = true;
        anchorThread = new Thread(this::runAnchorLogic);
        anchorThread.start();
    }

    @Override
    public void onDeactivate() {
        AnchorPos = null;
        isRunning = false;
        if (anchorThread != null) {
            try {
                anchorThread.join();
            } catch (InterruptedException e) {
                // Handle the interruption
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (isRunning) {
            if (Objects.requireNonNull(mc.world).getDimension().respawnAnchorWorks()) {
                toggle();
                return;
            }
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player || Friends.get().isFriend(player)) continue;
                if (mc.player.distanceTo(player) < range.get()) {
                    AnchorPos = positions(player);
                    breakAnchor();
                    for (BlockPos pos : AnchorPos) {
                        Rotations.rotate(Rotations.getYaw(pos), (Rotations.getPitch(pos)));
                    }
                }
            }
        }
    }

    private void runAnchorLogic() {
        while (isRunning) {
            if (Objects.requireNonNull(mc.world).getDimension().respawnAnchorWorks()) {
                toggle();
                return;
            }
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player || Friends.get().isFriend(player)) continue;
                if (mc.player.distanceTo(player) < range.get()) {
                    AnchorPos = positions(player);
                    breakAnchor();
                    for (BlockPos pos : AnchorPos) {
                        Rotations.rotate(Rotations.getYaw(pos), (Rotations.getPitch(pos)));
                    }
                }
            }
            try {
                Thread.sleep(100); // Sleep for 100 milliseconds
            } catch (InterruptedException e) {
                // Handle the interruption
            }
        }
    }

    public BlockPos[] positions(PlayerEntity entity) {
        ArrayList<BlockPos> posList = new ArrayList<>();
        int radiusSquared = RadiusX.get() * RadiusX.get(); // Calculate the square of the radius

        // Iterate through a square area around the target
        for (int x = -RadiusX.get(); x <= RadiusX.get(); x++) {
            for (int z = -RadiusZ.get(); z <= RadiusZ.get(); z++) {
                // Calculate the squared distance from the target
                int distanceSquared = x * x + z * z;

                // If the squared distance is within the radius squared, add the position
                if (distanceSquared <= radiusSquared) {
                    BlockPos pos = entity.getBlockPos().add(x, 0, z);
                    Rotations.rotate(Rotations.getYaw(pos), (Rotations.getPitch(pos)));
                    if (WorldUtils.isAir(pos) && !entity.getBoundingBox().intersects(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1)) {
                        posList.add(pos);
                        return posList.toArray(new BlockPos[0]); // Return the first valid position
                    } else {
                        // If it's not air, try to find a valid position nearby
                        for (int yOffset = -1; yOffset <= 1; yOffset++) {
                            BlockPos nearbyPos = pos.add(0, yOffset, 0);
                            if (WorldUtils.isAir(nearbyPos)  && !entity.getBoundingBox().intersects(nearbyPos.getX(), nearbyPos.getY(), nearbyPos.getZ(), nearbyPos.getX() + 1, nearbyPos.getY() + 1, nearbyPos.getZ() + 1)) {
                                posList.add(nearbyPos);
                                return posList.toArray(new BlockPos[0]); // Return the first valid position
                            }
                        }
                    }
                }
            }
        }

        // Always place anchors above and below the target
        if (WorldUtils.isAir(entity.getBlockPos().up(2)) && !entity.getBoundingBox().intersects(entity.getBlockPos().up(2).getX(), entity.getBlockPos().up(2).getY(), entity.getBlockPos().up(2).getZ(), entity.getBlockPos().up(2).getX() + 1, entity.getBlockPos().up(2).getY() + 1, entity.getBlockPos().up(2).getZ() + 1)) {
            posList.add(entity.getBlockPos().up(2));
            return posList.toArray(new BlockPos[0]); // Return the first valid position
        }
        if (WorldUtils.isAir(entity.getBlockPos().up(2))  && !entity.getBoundingBox().intersects(entity.getBlockPos().down(1).getX(), entity.getBlockPos().down(1).getY(), entity.getBlockPos().down(1).getZ(), entity.getBlockPos().down(1).getX() + 1, entity.getBlockPos().down(1).getY() + 1, entity.getBlockPos().down(1).getZ() + 1)) {
            posList.add(entity.getBlockPos().down(1));
            return posList.toArray(new BlockPos[0]); // Return the first valid position
        }

        return posList.toArray(new BlockPos[0]); // If no valid positions are found, return an empty array
    }

    public void breakAnchor() {
        for (BlockPos pos : AnchorPos) {
            FindItemResult stone = InvUtils.findInHotbar(Items.GLOWSTONE);
            FindItemResult anchor = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
            if (stone.found() && anchor.found()) {
                InvUtils.swap(anchor.slot(), true);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(pos.getX(), pos.getY(), pos.getZ()), Direction.DOWN, pos, false));
                InvUtils.swapBack();

                InvUtils.swap(stone.slot(), true);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(pos.getX(), pos.getY(), pos.getZ()), Direction.DOWN, pos, true));
                InvUtils.swapBack();

                InvUtils.swap(anchor.slot(), true);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(pos.getX(), pos.getY(), pos.getZ()), Direction.DOWN, pos, false));
                InvUtils.swapBack();
            }
        }
        Swapped = true;
    }


    @EventHandler
    public void onrender(Render3DEvent event) {
        if (AnchorPos == null) return;
        switch (renderMode.get()) {
            case normal -> {
                for (BlockPos pos : AnchorPos) {
                    event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                }
            }
            case fading -> {
                for (BlockPos pos : AnchorPos) {
                    RenderUtils.renderTickingBlock(
                            pos, sideColor.get(),
                            lineColor.get(),
                            shapeMode.get(), 0, rendertime.get(),
                            true, shrink.get()
                    );
                }
            }
            case smooth -> {
                for (BlockPos pos : AnchorPos) {
                    if (renderBoxOne == null) renderBoxOne = new Box(pos);
                    if (renderBoxTwo == null) renderBoxTwo = new Box(pos);


                    if (renderBoxTwo instanceof IBox) {
                        ((IBox) renderBoxTwo).set(
                                pos.getX(), pos.getY(), pos.getZ(),
                                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1
                        );
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
            }
        }
    }

    public enum SafetyMode {
        safe,
        balance,
        off,
    }

    public enum SwapMode {
        silent,
        normal,
    }

    public enum RenderMode {
        fading,
        normal,
        smooth
    }

    public enum SwingMode {
        offhand,
        mainhand,
        packet,
        none
    }
}
