package com.example.addon.modules.combat;

import com.example.addon.Addon;
import com.example.addon.utils.PlayerUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.*;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Debug;

import java.util.Objects;



public class AutoAnchor extends Module {

    private long lastAnchorPlaceTime = 0;
    private long lastGlowstonePlaceTime = 0;

    public enum SafetyMode {
        safe,
        balance,
        off,
    }

    public enum PlaceMode {
        side,
        line,
    }


    public enum RenderMode {
        fading,
        normal,

    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private long lastPlaceTime = 0;


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

    private final Setting<SafetyMode> safety = sgGeneral.add(new EnumSetting.Builder<SafetyMode>()
            .name("safe-mode")
            .description("Safety mode")
            .defaultValue(SafetyMode.safe)
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

    private final Setting<Double> DisableHealth = sgGeneral.add(new DoubleSetting.Builder()
            .name("Health disable")
            .description("when you reach this amount of health, the module will pause (0 = no pause)")
            .defaultValue(3.0)
            .sliderMax(36.0)
            .sliderMin(0.0)
            .build());

    private final Setting<Double> maxSelfDamage = sgGeneral.add(new DoubleSetting.Builder()
            .name("self damage")
            .description("the max amount to deal to you")
            .defaultValue(3.0)
            .sliderMax(36.0)
            .sliderMin(0.0)
            .build());

    private final Setting<Double> minDamage = sgGeneral.add(new DoubleSetting.Builder()
            .name("min damage")
            .description("the lowest amount of damage you should deal to the target (higher = less targets | lower = more targets)")
            .defaultValue(3.0)
            .sliderMax(36.0)
            .sliderMin(0.0)
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

    private final Setting<Boolean> Smart = sgGeneral.add(new BoolSetting.Builder()
            .name("smart")
            .description("more calculations. Highly recommended")
            .defaultValue(true)
            .build());

    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder()
            .name("side color")
            .description("Side color")
            .defaultValue(new SettingColor(255, 0, 0, 75))
            .build());

    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
            .name("line color")
            .description("Line color")
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .build());


    private final Setting<Boolean> shrink = sgGeneral.add(new BoolSetting.Builder()
            .name("shrink")
            .description("shrinks the render")
            .defaultValue(true)
            .build());


    private final Setting<Integer> rendertime = sgGeneral.add(new IntSetting.Builder()
            .name("render time")
            .description("render time")
            .defaultValue(3)
            .sliderMax(100)
            .sliderMin(1)
            .build());


    private final Setting<RenderMode> renderMode = sgGeneral.add(new EnumSetting.Builder<RenderMode>()
            .name("render mode")
            .description("render mode")
            .defaultValue(RenderMode.normal)
            .build());

    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build());

    public AutoAnchor() {
        super(Addon.Snail, "Auto Anchor+", "Automatically places Respawn Anchors near players.");
    }

    private BlockPos AnchorPos;
    @EventHandler
    private void onTick(TickEvent.Post event) {
        PlayerEntity target = TargetUtils.getPlayerTarget(range.get(), priority.get());

        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        if (target != null) {
            BlockPos targetHeadPos = target.getBlockPos().up(2);
            BlockPos anchorEast = target.getBlockPos().east(1);
            BlockPos anchorWest = target.getBlockPos().west(1);
            BlockPos anchorNorth = target.getBlockPos().north(1);
            BlockPos anchorSouth = target.getBlockPos().south(1);
            assert mc.world != null;
            boolean obsidianFound = false;
            boolean airFound = false;
            boolean respawnAnchorFound = false;
            boolean eastChecked = false;
            boolean northChecked = false;
            boolean southChecked = false;
            boolean westChecked = false;
            boolean anchorPlaced = false;


            // Update the anchor placement timer
            long currentTime = System.currentTimeMillis();
            if ((currentTime - lastAnchorPlaceTime) < AnchorDelay.get() * 1000) return;
            lastAnchorPlaceTime = currentTime;

            // Update the glowstone placement timer
            if ((currentTime - lastGlowstonePlaceTime) < GlowstoneDelay.get() * 1000) return;
            lastGlowstonePlaceTime = currentTime;

            if (player.getHealth() <= DisableHealth.get() && DisableHealth.get() > 0) {
                return;
            }

            if (target.distanceTo(player) > range.get()) {
                return;
            }

            float targetDamage = DamageUtils.anchorDamage(target, target.getBlockPos().toCenterPos());
            float selfDamage = DamageUtils.anchorDamage(player, target.getBlockPos().toCenterPos());
            Vec3d targetPos = predictMovement.get() ? predictTargetPosition(target) : target.getPos();

            if (targetDamage < minDamage.get()) {
                return;
            }

            if (placeSupport.get() && PlayerUtils.isSurrounded(target)) {
                placeSupportBlocks(target);
            }

            if (safety.get() == SafetyMode.safe && selfDamage > maxSelfDamage.get()) {
                return;
            }

            if (PlayerUtils.isSurrounded(target) && (mc.world.getBlockState(targetHeadPos).getBlock() == Blocks.AIR || mc.world.getBlockState(targetHeadPos).getBlock() == Blocks.RESPAWN_ANCHOR)) {
                TargetHeadPos(target);
                anchorPlaced = true; // Update anchorPlaced
            } else {
                if (mc.world.getBlockState(targetHeadPos).getBlock() == Blocks.OBSIDIAN) {
                    obsidianFound = true;
                }

                if (obsidianFound) {

                } else {
                    airFound = true;
                }

                while (!respawnAnchorFound && (!eastChecked || !northChecked || !southChecked || !westChecked)) {
                    if (!eastChecked) {
                        eastChecked = true;
                        if (mc.world.getBlockState(anchorEast).isAir() || mc.world.getBlockState(anchorEast).getBlock() == Blocks.RESPAWN_ANCHOR) {
                            if (!anchorPlaced) {
                                PlaceEastAnchor(target);
                                anchorPlaced = true;
                            }

                            respawnAnchorFound = true;
                        } else {
                            Block eastBlock = mc.world.getBlockState(anchorEast).getBlock();
                            if (eastBlock == Blocks.OBSIDIAN) {
                                obsidianFound = true;
                            } else if (eastBlock != Blocks.FIRE) {
                                airFound = true;
                            }
                        }
                    }

                    if (!northChecked) {
                        northChecked = true;
                        if (mc.world.getBlockState(anchorNorth).isAir() || mc.world.getBlockState(anchorNorth).getBlock() == Blocks.RESPAWN_ANCHOR) {
                            if (!anchorPlaced) {
                                PlaceNorthAnchor(target);
                                anchorPlaced = true;
                            }
                            respawnAnchorFound = true;
                        } else {
                            Block northBlock = mc.world.getBlockState(anchorNorth).getBlock();
                            if (northBlock == Blocks.OBSIDIAN) {
                                obsidianFound = true;
                            } else if (northBlock != Blocks.FIRE) {
                                airFound = true;
                            }
                        }
                    }

                    if (!southChecked) {
                        southChecked = true;
                        if (mc.world.getBlockState(anchorSouth).isAir() || mc.world.getBlockState(anchorSouth).getBlock() == Blocks.RESPAWN_ANCHOR) {
                            if (!anchorPlaced) {
                                PlaceSouthAnchor(target);
                                anchorPlaced = true;
                            }
                        } else {
                            Block southBlock = mc.world.getBlockState(anchorSouth).getBlock();
                            if (southBlock == Blocks.OBSIDIAN) {
                                obsidianFound = true;
                            } else if (southBlock != Blocks.FIRE) {
                                airFound = true;
                            }
                        }
                    }

                    if (!westChecked) {
                        westChecked = true;
                        if (mc.world.getBlockState(anchorWest).isAir() || mc.world.getBlockState(anchorWest).getBlock() == Blocks.RESPAWN_ANCHOR) {
                            if (!anchorPlaced) {
                                PlaceWestAnchor(target);
                                anchorPlaced = true;
                            }


                        } else {
                            Block westBlock = mc.world.getBlockState(anchorWest).getBlock();
                            if (westBlock == Blocks.OBSIDIAN) {
                                obsidianFound = true;
                            } else if (westBlock != Blocks.FIRE) {
                                airFound = true;
                            }
                        }
                    }
                }
                if (!respawnAnchorFound) {
                    if (airFound) {

                    } else {
                    }
                }
            }
        }
    }

    private Vec3d predictTargetPosition(PlayerEntity target) {
        return target.getPos().add(target.getVelocity());
    }

    @EventHandler
    public void AnchorRender(Render3DEvent event) {
        Integer renderTime = rendertime.get();
        // Ensure rendertime is properly initialized and not null
        if (rendertime == null) {
            throw new IllegalStateException("rendertime is not initialized");
        }
        renderTime = rendertime.get();

        // Ensure RenderMode is properly initialized and not null
        if (renderMode == null) {
            throw new IllegalStateException("RenderMode is not initialized");
        }
        RenderMode mode = renderMode.get();

        // Ensure sideColor, lineColor, and shapeMode are properly initialized and not null
        if (sideColor == null || lineColor == null || shapeMode == null) {
            throw new IllegalStateException("One or more rendering properties are not initialized");
        }

        if (AnchorPos != null) {
            if (mode == RenderMode.normal) {
                event.renderer.box(AnchorPos, sideColor.get(), lineColor.get(), shapeMode.get(), (int) 1.0f);
            } else if (mode == RenderMode.fading) {
                RenderUtils.renderTickingBlock(
                        AnchorPos, sideColor.get(),
                        lineColor.get(), shapeMode.get(),
                        0, rendertime.get(), true,
                        shrink.get()
                );
            }
        }
    }


    private void placeSupportBlocks(PlayerEntity target) {
        BlockPos supportPosNorth = target.getBlockPos().north(1);
        BlockPos supportPosNorthUpOne = target.getBlockPos().north(1).up(1);
        BlockPos supportPosNorthUpTwo = target.getBlockPos().north(1).up(2);
        BlockPos supportPosNorthUpThree = target.getBlockPos().north(1).up(3);
        BlockPos supportPosNorthUpFour = target.getBlockPos().up(3);

        BlockUtils.place(supportPosNorth, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, false);
        BlockUtils.place(supportPosNorthUpOne, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, false);
        BlockUtils.place(supportPosNorthUpTwo, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, false);
        BlockUtils.place(supportPosNorthUpThree, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, false);
        BlockUtils.place(supportPosNorthUpFour, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, false);


    }

    private void PlaceEastAnchor(PlayerEntity target) {
        BlockPos anchorEast = target.getBlockPos().east(1);
        BlockPos anchorNorth = target.getBlockPos().north(1);

        FindItemResult anchor = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
        FindItemResult glowStone = InvUtils.findInHotbar(Items.GLOWSTONE);
        ClientPlayerEntity player = mc.player;
        float SelfDamage = DamageUtils.anchorDamage(mc.player, anchorEast.toCenterPos());


        if (SelfDamage > maxSelfDamage.get() || (safety.get() == SafetyMode.safe && SelfDamage >= EntityUtils.getTotalHealth(Objects.requireNonNull(mc.player)))) {
        } else if (SelfDamage < maxSelfDamage.get() || (safety.get() == SafetyMode.safe && SelfDamage <= EntityUtils.getTotalHealth(Objects.requireNonNull(mc.player)))) {
            // Place Respawn Anchor
            assert mc.world != null;
            if (mc.world.getBlockState(anchorEast).getBlock() == Blocks.AIR) {
                BlockUtils.place(anchorEast, anchor, rotate.get(), 0, false);
                InvUtils.swapBack();
            }
            assert mc.interactionManager != null;
            // Interact with Respawn Anchor using glowstone
            if (mc.world.getBlockState(anchorEast).getBlock() == Blocks.RESPAWN_ANCHOR) {
                if (glowStone.found()) {
                    InvUtils.swap(glowStone.slot(), true);
                    mc.interactionManager.interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(anchorEast.getX() + 0.5, anchorEast.getY() + 0.5, anchorEast.getZ() + 0.5), Direction.UP, anchorEast, true));
                }
            }

            // Break Respawn Anchor
            if (mc.world.getBlockState(anchorEast).getBlock() == Blocks.RESPAWN_ANCHOR || mc.world.getBlockState(anchorEast).getBlock() == Blocks.FIRE)
                InvUtils.swap(anchor.slot(), true);

            mc.interactionManager.interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(anchorEast.getX() + 0.5, anchorEast.getY() + 0.5, anchorEast.getZ() + 0.5), Direction.UP, anchorEast, true));
            InvUtils.swapBack();
            AnchorPos = anchorEast;
        }
    }

    private void PlaceWestAnchor(PlayerEntity target) {
        BlockPos anchorWest = target.getBlockPos().west(1);

        FindItemResult anchor = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
        FindItemResult glowStone = InvUtils.findInHotbar(Items.GLOWSTONE);
        ClientPlayerEntity player = mc.player;
        float SelfDamage = DamageUtils.anchorDamage(mc.player, anchorWest.toCenterPos());

        if (SelfDamage > maxSelfDamage.get() || (safety.get() == SafetyMode.safe && SelfDamage >= EntityUtils.getTotalHealth(Objects.requireNonNull(mc.player)))) {
        } else if (SelfDamage < maxSelfDamage.get() || (safety.get() == SafetyMode.safe && SelfDamage <= EntityUtils.getTotalHealth(Objects.requireNonNull(mc.player)))) {
            // Place Respawn Anchor
            assert mc.world != null;
            if (mc.world.getBlockState(anchorWest).getBlock() == Blocks.AIR) {
                BlockUtils.place(anchorWest, anchor, rotate.get(), 0, false);
                InvUtils.swapBack();
            }
            // Interact with Respawn Anchor using Glow Stone
            assert mc.interactionManager != null;
            if (mc.world.getBlockState(anchorWest).getBlock() == Blocks.RESPAWN_ANCHOR) {
                if (glowStone.found()) {
                    InvUtils.swap(glowStone.slot(), true);
                    mc.interactionManager.interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(anchorWest.getX() + 0.5, anchorWest.getY() + 0.5, anchorWest.getZ() + 0.5), Direction.UP, anchorWest, true));
                }
            }
            // Break Respawn Anchor
            if (mc.world.getBlockState(anchorWest).getBlock() == Blocks.RESPAWN_ANCHOR)
                InvUtils.swap(anchor.slot(), true);

            mc.interactionManager.interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(anchorWest.getX() + 0.5, anchorWest.getY() + 0.5, anchorWest.getZ() + 0.5), Direction.UP, anchorWest, true));
            InvUtils.swapBack();
            AnchorPos = anchorWest;
        }
    }

    private void PlaceSouthAnchor(PlayerEntity target) {
        BlockPos anchorSouth = target.getBlockPos().south(1);

        FindItemResult anchor = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
        FindItemResult glowStone = InvUtils.findInHotbar(Items.GLOWSTONE);
        ClientPlayerEntity player = mc.player;
        float SelfDamage = DamageUtils.anchorDamage(mc.player, anchorSouth.toCenterPos());


        if (SelfDamage > maxSelfDamage.get() || (safety.get() == SafetyMode.safe && SelfDamage >= EntityUtils.getTotalHealth(Objects.requireNonNull(mc.player)))) {
        } else if (SelfDamage < maxSelfDamage.get() || (safety.get() == SafetyMode.safe && SelfDamage <= EntityUtils.getTotalHealth(Objects.requireNonNull(mc.player)))) {
            // Place Respawn Anchor
            assert mc.world != null;
            if (mc.world.getBlockState(anchorSouth).getBlock() == Blocks.AIR) {
                BlockUtils.place(anchorSouth, anchor, rotate.get(), 0, false);
                InvUtils.swapBack();
            }
            assert mc.interactionManager != null;
            // Interact with Respawn Anchor using Glow Stone
            if (mc.world.getBlockState(anchorSouth).getBlock() == Blocks.RESPAWN_ANCHOR) {
                if (glowStone.found()) {
                    InvUtils.swap(glowStone.slot(), true);
                    mc.interactionManager.interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(anchorSouth.getX() + 0.5, anchorSouth.getY() + 0.5, anchorSouth.getZ() + 0.5), Direction.UP, anchorSouth, true));
                }
            }

            // Break Respawn Anchor
            if (mc.world.getBlockState(anchorSouth).getBlock() == Blocks.RESPAWN_ANCHOR)
                InvUtils.swap(anchor.slot(), true);

            mc.interactionManager.interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(anchorSouth.getX() + 0.5, anchorSouth.getY() + 0.5, anchorSouth.getZ() + 0.5), Direction.UP, anchorSouth, true));
            InvUtils.swapBack();
            AnchorPos = anchorSouth;
        }
    }

    private void PlaceNorthAnchor(PlayerEntity target) {
        BlockPos anchorNorth = target.getBlockPos().north(1);

        FindItemResult anchor = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
        FindItemResult glowStone = InvUtils.findInHotbar(Items.GLOWSTONE);
        ClientPlayerEntity player = mc.player;
        float SelfDamage = DamageUtils.anchorDamage(mc.player, anchorNorth.toCenterPos());


        if (SelfDamage > maxSelfDamage.get() || (safety.get() == SafetyMode.safe && SelfDamage >= EntityUtils.getTotalHealth(Objects.requireNonNull(mc.player)))) {
        } else if (SelfDamage < maxSelfDamage.get() || (safety.get() == SafetyMode.safe && SelfDamage <= EntityUtils.getTotalHealth(Objects.requireNonNull(mc.player)))) {
            // Place Respawn Anchor
            assert mc.world != null;
            if (mc.world.getBlockState(anchorNorth).getBlock() == Blocks.AIR) {
                BlockUtils.place(anchorNorth, anchor, rotate.get(), 0, false);
                InvUtils.swapBack();
            }
            assert mc.interactionManager != null;
            // Interact with Respawn Anchor using Glow Stone
            if (mc.world.getBlockState(anchorNorth).getBlock() == Blocks.RESPAWN_ANCHOR) {
                if (glowStone.found()) {
                    InvUtils.swap(glowStone.slot(), true);
                    mc.interactionManager.interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(anchorNorth.getX() + 0.5, anchorNorth.getY() + 0.5, anchorNorth.getZ() + 0.5), Direction.UP, anchorNorth, true));
                }
            }

            // Break Respawn Anchor
            if (mc.world.getBlockState(anchorNorth).getBlock() == Blocks.RESPAWN_ANCHOR)
                InvUtils.swap(anchor.slot(), true);

            mc.interactionManager.interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(anchorNorth.getX() + 0.5, anchorNorth.getY() + 0.5, anchorNorth.getZ() + 0.5), Direction.UP, anchorNorth, true));
            InvUtils.swapBack();
            AnchorPos = anchorNorth;
        }
    }

    private void TargetHeadPos(PlayerEntity target) {
        BlockPos targetHeadPos = target.getBlockPos().up(2);
        FindItemResult anchor = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
        FindItemResult glowStone = InvUtils.findInHotbar(Items.GLOWSTONE);
        ClientPlayerEntity player = mc.player;
        float SelfDamage = DamageUtils.anchorDamage(mc.player, targetHeadPos.toCenterPos());

        // Place Respawn Anchor
        assert mc.world != null;
        if (mc.world.getBlockState(targetHeadPos).getBlock() == Blocks.AIR) {
            BlockUtils.place(targetHeadPos, anchor, rotate.get(), 0, false);
            InvUtils.swapBack();
            AnchorPos = targetHeadPos;
        }
        assert mc.interactionManager != null;
        // Interact with Respawn Anchor
        if (mc.world.getBlockState(targetHeadPos).getBlock() == Blocks.RESPAWN_ANCHOR) {
            if (glowStone.found()) {
                InvUtils.swap(glowStone.slot(), true);
                mc.interactionManager.interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(targetHeadPos.getX() + 0.5, targetHeadPos.getY() + 0.5, targetHeadPos.getZ() + 0.5), Direction.UP, targetHeadPos, true));
            }
        }

        // Break Respawn Anchor
        if (mc.world.getBlockState(targetHeadPos).getBlock() == Blocks.RESPAWN_ANCHOR) {
            InvUtils.swap(anchor.slot(), true);

            mc.interactionManager.interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(targetHeadPos.getX() + 0.5, targetHeadPos.getY() + 0.5, targetHeadPos.getZ() + 0.5), Direction.UP, targetHeadPos, true));
            InvUtils.swapBack();
        }
    }
}