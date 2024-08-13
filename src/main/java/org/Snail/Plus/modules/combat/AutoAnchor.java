package org.Snail.Plus.modules.combat;


import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IBox;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.block.SlabBlock;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.Snail.Plus.Addon;
import org.Snail.Plus.utils.CombatUtils;
import org.Snail.Plus.utils.SwapUtils;
import org.Snail.Plus.utils.TPSSyncUtil;
import org.Snail.Plus.utils.WorldUtils;

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
            .defaultValue(2)
            .sliderMax(5)
            .sliderMin(-5)
            .visible(Smart::get)
            .build());
    private final Setting<Integer> RadiusX = sgGeneral.add(new IntSetting.Builder()
            .name("Radius X")
            .description("the radius for X")
            .defaultValue(2)
            .sliderMax(5)
            .sliderMin(-5)
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
    private final Setting<Boolean> MultiTask= sgMisc.add(new BoolSetting.Builder()
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
    private long lastAnchorPlaceTime = 0;
    private long lastGlowstonePlaceTime = 0;
    private PlayerEntity target;
    private boolean Swapped;
    private BlockPos AnchorPos = BlockPos.ORIGIN;;
    private Box renderBoxOne, renderBoxTwo;
    private boolean anchorPlaced = false;
    private BlockPos pos = BlockPos.ORIGIN;;

    public AutoAnchor() {
        super(Addon.Snail, "Anchor Bomb+", "explodes anchors near targets");

    }

    @Override
    public void onActivate() {
        target = null;
        AnchorPos = null;
        pos = null;
    }

    @Override
    public void onDeactivate() {
        target = null;
        AnchorPos = null;
        pos = null;
    }

    private float DamageScore(PlayerEntity target, BlockPos pos) {
        return DamageUtils.anchorDamage(target, pos.toCenterPos());
    }
    protected void FindPossiblePositions(double radiusX, double radiusZ, PlayerEntity target) {
        AnchorPos = findAirPosition(target.getX() - radiusX, target.getY(), target.getZ() - radiusZ, radiusX, radiusZ);
    }
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (Objects.requireNonNull(mc.world).getDimension().respawnAnchorWorks()) {
            toggle();
            return;
        }
        target = TargetUtils.getPlayerTarget(range.get(), priority.get());
        if (TargetUtils.isBadTarget(target, range.get())) return;
        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastAnchorPlaceTime) < AnchorDelay.get() * 1000) return;
        lastAnchorPlaceTime = currentTime;
        if ((currentTime - lastGlowstonePlaceTime) < GlowstoneDelay.get() * 1000) return;
        lastGlowstonePlaceTime = currentTime;
        if (TpsSync.get()) {
            /* sync delay to tps */
            double currentDelay = TpsSync.get() ? 1.0 / TPSSyncUtil.getCurrentTPS() : AnchorDelay.get();

            /* glowstone delay */
            if ((currentDelay - lastGlowstonePlaceTime) < lastGlowstonePlaceTime * 1000) return;
            lastGlowstonePlaceTime = currentTime;

            /* anchor delay */
            if ((currentDelay - lastAnchorPlaceTime) < lastAnchorPlaceTime * 1000) return;
            lastAnchorPlaceTime = currentTime;
        }
        BlockPos targetHeadPos = target.getBlockPos().up(2);
        if (predictMovement.get()) {
            Vec3d targetPos = Vec3d.of(target.getBlockPos());
            targetPos.add(
                    target.getX() - target.prevX,
                    target.getY() - target.prevY,
                    target.getZ() - target.prevZ
            );
            targetHeadPos = target.getBlockPos().up(2);
            anchorPlaced = false;
            Swapped = false;
        }

        if(pauseUse.get() && Objects.requireNonNull(mc.player).isUsingItem()) {
            return;
        }

        if (CombatUtils.isBurrowed(target)) return;

        if (!CombatUtils.isSurrounded(target) && Smart.get()) {
            FindPossiblePositions(RadiusX.get(), RadiusZ.get(),target);
        } else {
            if (Smart.get() && CombatUtils.isSurrounded(target) && WorldUtils.isAir(targetHeadPos) || mc.world.getBlockState(targetHeadPos).getBlock() == Blocks.RESPAWN_ANCHOR) {
                AnchorPos = targetHeadPos;
                TargetHeadPos();
                SwapAndPlace();
            }
        }

        if (!Smart.get() && WorldUtils.isAir(targetHeadPos) || mc.world.getBlockState(targetHeadPos).getBlock() == Blocks.RESPAWN_ANCHOR) {
            AnchorPos = targetHeadPos;
            TargetHeadPos();
            SwapAndPlace();
        }
        if (placeSupport.get() && CombatUtils.isSurrounded(target)) {
            placeSupportBlocks(target);
        }
        if (anchorPlaced) {
            switch (swingMode.get()) {
                case none:
                    break;
                case mainhand:
                    Objects.requireNonNull(mc.player).swingHand(Hand.MAIN_HAND);
                    break;
                case offhand:
                    Objects.requireNonNull(mc.player).swingHand(Hand.OFF_HAND);
                case packet:
                    Objects.requireNonNull(mc.interactionManager).interactItem(mc.player, Hand.MAIN_HAND);
                    break;
            }
        }
        if (Breaker.get() && mc.world.getBlockState(targetHeadPos).getBlock() == Blocks.GLOWSTONE || Breaker.get() && mc.world.getBlockState(targetHeadPos).getBlock() instanceof SlabBlock) {
            Objects.requireNonNull(mc.interactionManager).breakBlock(targetHeadPos);
        }
    }
    //calc positions
    private BlockPos findAirPosition(double startX, double startY, double startZ, double radiusX, double radiusZ) {
        for (double x = startX - radiusX; x <= startX + radiusX; x++) {
            for (double z = startZ - radiusZ; z <= startZ + radiusZ; z++) {
                BlockPos potentialPos = new BlockPos((int) x, (int) startY, (int) z);
                SafetyChecks(potentialPos);
                if (Objects.requireNonNull(mc.world).getBlockState(potentialPos).isAir() || mc.world.getBlockState(potentialPos).getBlock() == Blocks.FIRE || mc.world.getBlockState(potentialPos).getBlock() == Blocks.RESPAWN_ANCHOR) {
                    AnchorPos = potentialPos;
                    for (Direction dir : Direction.values()) {
                        if (strictDirection.get() && WorldUtils.strictDirection(AnchorPos.offset(dir), dir.getOpposite())) continue;
                    }
                    SwapAndPlace();
                    return AnchorPos;
                }
            }
        }
        return null; // No air position found within the radius
    }

    private void findDamagePosition(BlockPos position, double Damage, double SelfDamage) {
        int maxInteractions = 10;
        int interactions = 0;
        while(Damage < minDamage.get() || SelfDamage < maxSelfDamage.get() && maxInteractions < interactions) {
            interactions++;
            position = position.add(position.getX() + 1, position.getY(), position.getZ() + 1);
            if(WorldUtils.willMiss(position, target) && Damage >= minDamage.get() && SelfDamage < maxSelfDamage.get()) {
                continue;
            } else {
                position = position.add(position.getX() - 1, position.getY(), position.getZ() - 1);

                if(WorldUtils.willMiss(position, target) && Damage >= minDamage.get() && SelfDamage < maxSelfDamage.get()) {
                    continue;
                } else {
                    position = position.add(position.getX() - 1, position.getY(), position.getZ() + 1);

                    if (WorldUtils.willMiss(position, target) && Damage >= minDamage.get() && SelfDamage < maxSelfDamage.get()) {
                        continue;
                    } else {
                        position = position.add(position.getX() + 1, position.getY(), position.getZ() - 1);
                        if (WorldUtils.willMiss(position, target) && Damage >= minDamage.get() && SelfDamage < maxSelfDamage.get()) {
                            continue;
                        }
                    }
                }
            }
        }
    }

    private void SafetyChecks(BlockPos pos) {
        int maxInteractions = 10;
        int interactions = 1;
        float bestSelfScore = DamageScore(mc.player, pos);
        float bestDamageScore = DamageScore(target, pos);
        while (maxInteractions > interactions) {
            interactions++;
            switch (safety.get()) {
                case safe -> {
                    if (bestSelfScore < maxSelfDamage.get() || Objects.requireNonNull(mc.player).getHealth() > maxSelfDamage.get()) {
                        findDamagePosition(pos, bestDamageScore, bestSelfScore);
                    }
                    if (bestDamageScore < minDamage.get()) {
                        findDamagePosition(pos, bestDamageScore, bestSelfScore);

                    } else {
                        continue;
                    }
                    float damageRatio = bestDamageScore / bestSelfScore;
                    if (damageRatio >= this.DamageRatio.get() || damageRatio == this.DamageRatio.get()) {
                        findDamagePosition(pos, bestDamageScore, bestSelfScore);

                    }
                }
                case balance -> {
                    if (Objects.requireNonNull(mc.player).getHealth() >= maxSelfDamage.get()) {
                        findDamagePosition(pos, bestDamageScore, bestSelfScore);

                    } else {
                        continue;
                    }
                    if (bestDamageScore >= minDamage.get()) {
                        findDamagePosition(pos, bestDamageScore, bestSelfScore);

                    }
                }
                case off -> {
                    break;
                }
            }
        }
    }

    private void placeSupportBlocks(PlayerEntity target) {
        BlockPos supportPosNorth = target.getBlockPos().north(1);
        BlockPos supportPosNorthUpOne = target.getBlockPos().north(1).up(1);
        BlockPos supportPosNorthUpTwo = target.getBlockPos().north(1).up(2);
        BlockPos supportPosNorthUpThree = target.getBlockPos().north(1).up(3);
        BlockPos supportPosNorthUpFour = target.getBlockPos().up(3);
        BlockUtils.place(supportPosNorth, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 100, true);
        BlockUtils.place(supportPosNorthUpOne, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 100, true);
        BlockUtils.place(supportPosNorthUpTwo, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 100, true);
        BlockUtils.place(supportPosNorthUpThree, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 100, true);
        BlockUtils.place(supportPosNorthUpFour, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 100, true);
    }


    @EventHandler
    public void AnchorRender(Render3DEvent event) {
        RenderMode mode = renderMode.get();
        boolean ShrinkNow = shrink.get() && TargetUtils.isBadTarget(target, range.get()) || AnchorPos == null;
        if (TargetUtils.isBadTarget(target, range.get())) return;
        if (AnchorPos != null) {
            if (mode == RenderMode.normal) {
                event.renderer.box(AnchorPos, sideColor.get(), lineColor.get(), shapeMode.get(), (int) 1.0f);
            } else if (mode == RenderMode.fading) {
                RenderUtils.renderTickingBlock(
                        AnchorPos, sideColor.get(),
                        lineColor.get(), shapeMode.get(),
                        0, rendertime.get(), true, ShrinkNow
                );
            }
            if (mode == RenderMode.smooth) {
                if (renderBoxOne == null) renderBoxOne = new Box(AnchorPos);
                if (renderBoxTwo == null) renderBoxTwo = new Box(AnchorPos);


                if (renderBoxTwo instanceof IBox) {
                    ((IBox) renderBoxTwo).set(
                            AnchorPos.getX(), AnchorPos.getY(), AnchorPos.getZ(),
                            AnchorPos.getX() + 1, AnchorPos.getY() + 1, AnchorPos.getZ() + 1
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

    private void TargetHeadPos() {
        if (!Swapped) {
            SwapAndPlace();
        }
    }

    private void SwapAndPlace() {
        FindItemResult glowstone = InvUtils.findInHotbar(Items.GLOWSTONE);
        FindItemResult anchor = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
        ClientPlayerEntity player = mc.player;
        switch (swapMethod.get()) {
            case normal:
                if (Objects.requireNonNull(mc.world).getBlockState(AnchorPos).getBlock() == Blocks.RESPAWN_ANCHOR || mc.world.getBlockState(AnchorPos).getBlock() == Blocks.FIRE || mc.world.getBlockState(AnchorPos).isAir()) {
                    if (BlockUtils.canPlace(AnchorPos)) {
                        BlockUtils.place(AnchorPos, anchor, rotate.get(), 100, true);
                    }
                }
                if (mc.world.getBlockState(AnchorPos).getBlock() == Blocks.RESPAWN_ANCHOR) {
                    SwapUtils.Normal(glowstone.slot(), 1.0F);
                    Objects.requireNonNull(mc.interactionManager).interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(AnchorPos.getX() + 0.5, AnchorPos.getY() + 0.5, AnchorPos.getZ() + 0.5), Direction.UP, AnchorPos, true));
                    SwapUtils.Normal(anchor.slot(), 0.0F);
                    Objects.requireNonNull(mc.interactionManager).interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(AnchorPos.getX() + 0.5, AnchorPos.getY() + 0.5, AnchorPos.getZ() + 0.5), Direction.UP, AnchorPos, true));
                    SwapUtils.Normal(anchor.slot(), 0.0F);
                    Swapped = true;
                }
                break;
            case silent:
                anchor = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
                glowstone = InvUtils.findInHotbar(Items.GLOWSTONE);
                if (Objects.requireNonNull(mc.world).getBlockState(AnchorPos).getBlock() == Blocks.RESPAWN_ANCHOR || mc.world.getBlockState(AnchorPos).getBlock() == Blocks.FIRE || mc.world.getBlockState(AnchorPos).isAir()) {
                    if (BlockUtils.canPlace(AnchorPos)) {
                        BlockUtils.place(AnchorPos, anchor, rotate.get(), 100, true);
                    }
                }
                if (mc.world.getBlockState(AnchorPos).getBlock() == Blocks.RESPAWN_ANCHOR) {
                    SwapUtils.SilentSwap(glowstone.slot(), 1.0);
                    Objects.requireNonNull(mc.interactionManager).interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(AnchorPos.getX() + 0.5, AnchorPos.getY() + 0.5, AnchorPos.getZ() + 0.5), Direction.UP, AnchorPos, true));
                    SwapUtils.SilentSwap(anchor.slot(), 0.0);
                    Objects.requireNonNull(mc.interactionManager).interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(AnchorPos.getX() + 0.5, AnchorPos.getY() + 0.5, AnchorPos.getZ() + 0.5), Direction.UP, AnchorPos, true));
                    Swapped = true;
                }
                break;
            case invSwitch:
                int originalSlot = Objects.requireNonNull(mc.player).getInventory().selectedSlot;
                anchor = InvUtils.find(Items.RESPAWN_ANCHOR);
                glowstone = InvUtils.find(Items.GLOWSTONE);

                if (Objects.requireNonNull(mc.world).getBlockState(AnchorPos).getBlock() == Blocks.RESPAWN_ANCHOR || mc.world.getBlockState(AnchorPos).getBlock() == Blocks.FIRE || mc.world.getBlockState(AnchorPos).isAir())

                    if (BlockUtils.canPlace(AnchorPos)) {
                        BlockUtils.place(AnchorPos, anchor, rotate.get(), 100, true);
                    }

                if (mc.world.getBlockState(AnchorPos).getBlock() == Blocks.RESPAWN_ANCHOR) {
                    SwapUtils.invSwitch(originalSlot, anchor.slot(), true, 0.0F);
                    Objects.requireNonNull(mc.interactionManager).interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(AnchorPos.getX() + 0.5, AnchorPos.getY() + 0.5, AnchorPos.getZ() + 0.5), Direction.UP, AnchorPos, true));
                    SwapUtils.SilentSwap(anchor.slot(), 0.0);
                    Objects.requireNonNull(mc.interactionManager).interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(AnchorPos.getX() + 0.5, AnchorPos.getY() + 0.5, AnchorPos.getZ() + 0.5), Direction.UP, AnchorPos, true));
                    Swapped = true;
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
        invSwitch,
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