package org.Snail.Plus.modules.combat;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
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
import org.Snail.Plus.Addon;
import org.Snail.Plus.utils.CombatUtils;

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

    private final Setting<Boolean> strictDirection = sgGeneral.add(new BoolSetting.Builder()
            .name("strict direction")
            .description("Only places anchors in the direction you are facing. Will crash if you are falling")
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

    private final Setting<Boolean> AntiStuck = sgGeneral.add(new BoolSetting.Builder()
            .name("anti stuck")
            .description("breaks glowstone in the way")
            .defaultValue(true)
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

    private final Setting<RenderMode> renderMode = sgGeneral.add(new EnumSetting.Builder<RenderMode>()
            .name("render mode")
            .description("render mode")
            .defaultValue(RenderMode.normal)
            .build());

    private final Setting<Integer> rendertime = sgGeneral.add(new IntSetting.Builder()
            .name("render time")
            .description("render time")
            .defaultValue(3)
            .sliderMax(100)
            .sliderMin(1)
            .build());

    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build());
    private BlockPos AnchorPos;
    public AutoAnchor() {
        super(Addon.Snail, "Auto Anchor+", "Anchor aura but better");
    }
    @EventHandler
    private void onTick(TickEvent.Post event) {

        if (Objects.requireNonNull(mc.world).getDimension().respawnAnchorWorks()) {
            toggle();
            return;
        }

        PlayerEntity target = TargetUtils.getPlayerTarget(range.get(), priority.get());
        if (target != null) {
            long currentTime = System.currentTimeMillis();
            if ((currentTime - lastAnchorPlaceTime) < AnchorDelay.get() * 1000) return;
            lastAnchorPlaceTime = currentTime;


            if ((currentTime - lastGlowstonePlaceTime) < GlowstoneDelay.get() * 1000) return;
            lastGlowstonePlaceTime = currentTime;

            Vec3d targetPos = predictMovement.get() ? predictTargetPosition(target) : target.getPos();

            if (TargetUtils.isBadTarget(target, range.get())) return;

            ClientPlayerEntity player = mc.player;

            BlockPos targetHeadPos = target.getBlockPos().up(2);
            BlockPos anchorEast = target.getBlockPos().east(1);
            BlockPos anchorWest = target.getBlockPos().west(1);
            BlockPos anchorNorth = target.getBlockPos().north(1);
            BlockPos anchorSouth = target.getBlockPos().south(1);
            BlockPos targetFeetPos = target.getBlockPos().down(1);

            boolean obsidianFound = false;
            boolean airFound = false;
            boolean respawnAnchorFound = false;
            boolean eastChecked = false;
            boolean northChecked = false;
            boolean southChecked = false;
            boolean westChecked = false;
            boolean anchorPlaced = false;


            if (Objects.requireNonNull(player).getHealth() <= DisableHealth.get() && DisableHealth.get() > 0) return;

            if (CombatUtils.isBurrowed(target)) return;

            if (target.distanceTo(player) > range.get()) return;

            float targetDamage = DamageUtils.anchorDamage(target, target.getBlockPos().toCenterPos());
            float selfDamage = DamageUtils.anchorDamage(player, target.getBlockPos().toCenterPos());

            if (targetDamage < minDamage.get()) {
                return;
            }
            //will crash
            for (CardinalDirection dir : CardinalDirection.values()) {
                if (this.strictDirection.get()
                        && dir.toDirection() != Objects.requireNonNull(mc.player).getHorizontalFacing()
                        && dir.toDirection().getOpposite() != mc.player.getHorizontalFacing()) continue;
            }

            if (AntiStuck.get() && mc.world.getBlockState(targetHeadPos).getBlock() == Blocks.GLOWSTONE) {
                BlockUtils.breakBlock(targetHeadPos, false);
            }

            if (placeSupport.get() && CombatUtils.isSurrounded(target)) {
                placeSupportBlocks(target);
            }

            if (!Smart.get() && (mc.world.getBlockState(targetHeadPos).getBlock() == Blocks.AIR || this.mc.world.getBlockState(targetHeadPos).getBlock() == Blocks.RESPAWN_ANCHOR)) {
                TargetHeadPos(target);
                anchorPlaced = true;
            } else if (Smart.get() && CombatUtils.isSurrounded(target) && mc.world.getBlockState(targetHeadPos).isAir()) {
                TargetHeadPos(target);
                anchorPlaced = true;
            } else if (!Smart.get() && mc.world.getBlockState(targetHeadPos).getBlock() == Blocks.OBSIDIAN && this.mc.world.getBlockState(targetFeetPos).isAir()) {
                TargetFeetPos(target);
                anchorPlaced = true;
            }

            if (obsidianFound) {

                } else {
                    airFound = true;
                }

                while (!respawnAnchorFound && (!eastChecked || !northChecked || !southChecked || !westChecked)) {
                    if (safety.get() == SafetyMode.safe && selfDamage > this.maxSelfDamage.get()) continue;

                    if (safety.get() == SafetyMode.balance && selfDamage <= this.maxSelfDamage.get())
                        continue;

                    if (safety.get() == SafetyMode.off && selfDamage >= this.maxSelfDamage.get() || safety.get() == SafetyMode.off && selfDamage <= this.maxSelfDamage.get())
                        continue;

                    if (!eastChecked) {
                        eastChecked = true;
                        if (Smart.get() && mc.world.getBlockState(anchorEast).isAir() || this.mc.world.getBlockState(anchorEast).getBlock() == Blocks.RESPAWN_ANCHOR || this.mc.world.getBlockState(anchorEast).getBlock() == Blocks.FIRE) {
                            if (!anchorPlaced) {
                                this.PlaceEastAnchor(target);
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
                        if (Smart.get() && mc.world.getBlockState(anchorNorth).isAir() || mc.world.getBlockState(anchorNorth).getBlock() == Blocks.RESPAWN_ANCHOR || mc.world.getBlockState(anchorNorth).getBlock() == Blocks.FIRE) {
                            if (!anchorPlaced) {
                                this.PlaceNorthAnchor(target);
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
                        if (Smart.get() && this.mc.world.getBlockState(anchorSouth).isAir() || mc.world.getBlockState(anchorSouth).getBlock() == Blocks.RESPAWN_ANCHOR || this.mc.world.getBlockState(anchorSouth).getBlock() == Blocks.FIRE) {
                            if (!anchorPlaced) {
                                this.PlaceSouthAnchor(target);
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
                        if (Smart.get() && this.mc.world.getBlockState(anchorWest).isAir() || mc.world.getBlockState(anchorWest).getBlock() == Blocks.RESPAWN_ANCHOR || this.mc.world.getBlockState(anchorWest).getBlock() == Blocks.FIRE) {
                            if (!anchorPlaced) {
                                PlaceWestAnchor(target);
                                anchorPlaced = true;
                            }


                        } else {
                            Block westBlock = this.mc.world.getBlockState(anchorWest).getBlock();
                            if (westBlock == Blocks.OBSIDIAN) {
                                obsidianFound = true;
                            } else if (westBlock != Blocks.FIRE) {
                                airFound = true;
                            }
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
        RenderMode mode = renderMode.get();

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

        BlockUtils.place(supportPosNorth, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 100, true);
        BlockUtils.place(supportPosNorthUpOne, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 100, true);
        BlockUtils.place(supportPosNorthUpTwo, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 100, true);
        BlockUtils.place(supportPosNorthUpThree, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 100, true);
        BlockUtils.place(supportPosNorthUpFour, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 100, true);
    }

    private void PlaceEastAnchor(PlayerEntity target) {
        BlockPos anchorEast = target.getBlockPos().east(1);

        FindItemResult anchor = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
        FindItemResult glowStone = InvUtils.findInHotbar(Items.GLOWSTONE);
        ClientPlayerEntity player = mc.player;
        float SelfDamage = DamageUtils.anchorDamage(mc.player, anchorEast.toCenterPos());

        if (SelfDamage < this.maxSelfDamage.get() || (this.safety.get() == SafetyMode.safe && SelfDamage <= EntityUtils.getTotalHealth(Objects.requireNonNull(mc.player)))) {

            if (Objects.requireNonNull(mc.world).getBlockState(anchorEast).getBlock() == Blocks.AIR) {
                BlockUtils.place(anchorEast, anchor, rotate.get(), 100, true);
                InvUtils.swapBack();
            }
            assert mc.interactionManager != null;

            if (mc.world.getBlockState(anchorEast).getBlock() == Blocks.RESPAWN_ANCHOR) {
                if (glowStone.found()) {
                    InvUtils.swap(glowStone.slot(), true);
                    mc.interactionManager.interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(anchorEast.getX() + 0.5, anchorEast.getY() + 0.5, anchorEast.getZ() + 0.5), Direction.UP, anchorEast, true));
                }
            }
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

        if (SelfDamage < maxSelfDamage.get() || (safety.get() == SafetyMode.safe && SelfDamage <= EntityUtils.getTotalHealth(Objects.requireNonNull(mc.player)))) {

            if (Objects.requireNonNull(mc.world).getBlockState(anchorWest).getBlock() == Blocks.AIR) {
                BlockUtils.place(anchorWest, anchor, rotate.get(), 100, true);
                InvUtils.swapBack();
            }

            assert mc.interactionManager != null;
            if (mc.world.getBlockState(anchorWest).getBlock() == Blocks.RESPAWN_ANCHOR) {
                if (glowStone.found()) {
                    InvUtils.swap(glowStone.slot(), true);
                    mc.interactionManager.interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(anchorWest.getX() + 0.5, anchorWest.getY() + 0.5, anchorWest.getZ() + 0.5), Direction.UP, anchorWest, true));
                }
            }

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


        if (SelfDamage < maxSelfDamage.get() || (safety.get() == SafetyMode.safe && SelfDamage <= EntityUtils.getTotalHealth(Objects.requireNonNull(mc.player)))) {

            assert mc.world != null;
            if (mc.world.getBlockState(anchorSouth).getBlock() == Blocks.AIR) {
                BlockUtils.place(anchorSouth, anchor, rotate.get(), 100, true);
                InvUtils.swapBack();
            }
            if (mc.world.getBlockState(anchorSouth).getBlock() == Blocks.RESPAWN_ANCHOR) {
                if (glowStone.found()) {
                    InvUtils.swap(glowStone.slot(), true);

                    Objects.requireNonNull(mc.interactionManager).interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(anchorSouth.getX() + 0.5, anchorSouth.getY() + 0.5, anchorSouth.getZ() + 0.5), Direction.UP, anchorSouth, true));
                }
            }
            if (mc.world.getBlockState(anchorSouth).getBlock() == Blocks.RESPAWN_ANCHOR)
                InvUtils.swap(anchor.slot(), true);

            Objects.requireNonNull(mc.interactionManager).interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(anchorSouth.getX() + 0.5, anchorSouth.getY() + 0.5, anchorSouth.getZ() + 0.5), Direction.UP, anchorSouth, true));
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


        if (SelfDamage < maxSelfDamage.get() || (safety.get() == SafetyMode.safe && SelfDamage <= EntityUtils.getTotalHealth(Objects.requireNonNull(mc.player)))) {


            if (Objects.requireNonNull(mc.world).getBlockState(anchorNorth).getBlock() == Blocks.AIR) {
                BlockUtils.place(anchorNorth, anchor, rotate.get(), 100, true);
                InvUtils.swapBack();
            }
            assert mc.interactionManager != null;

            if (mc.world.getBlockState(anchorNorth).getBlock() == Blocks.RESPAWN_ANCHOR) {
                if (glowStone.found()) {
                    InvUtils.swap(glowStone.slot(), true);
                    mc.interactionManager.interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(anchorNorth.getX() + 0.5, anchorNorth.getY() + 0.5, anchorNorth.getZ() + 0.5), Direction.UP, anchorNorth, true));
                }
            }
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

        if (Objects.requireNonNull(mc.world).getBlockState(targetHeadPos).getBlock() == Blocks.AIR) {
            BlockUtils.place(targetHeadPos, anchor, rotate.get(), 100, true);
            InvUtils.swapBack();
            AnchorPos = targetHeadPos;
        }
        if (mc.world.getBlockState(targetHeadPos).getBlock() == Blocks.RESPAWN_ANCHOR) {
            if (glowStone.found()) {
                InvUtils.swap(glowStone.slot(), true);
                Objects.requireNonNull(mc.interactionManager).interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(targetHeadPos.getX() + 0.5, targetHeadPos.getY() + 0.5, targetHeadPos.getZ() + 0.5), Direction.UP, targetHeadPos, true));
            }
        }


        if (mc.world.getBlockState(targetHeadPos).getBlock() == Blocks.RESPAWN_ANCHOR) {
            InvUtils.swap(anchor.slot(), true);

            Objects.requireNonNull(mc.interactionManager).interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(targetHeadPos.getX() + 0.5, targetHeadPos.getY() + 0.5, targetHeadPos.getZ() + 0.5), Direction.UP, targetHeadPos, true));
            InvUtils.swapBack();
        }
    }

    private void TargetFeetPos(PlayerEntity target) {
        BlockPos TargetFeetPos = target.getBlockPos().down(1);

        BlockPos targetHeadPos = target.getBlockPos().up(2);
        FindItemResult anchor = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
        FindItemResult glowStone = InvUtils.findInHotbar(Items.GLOWSTONE);
        ClientPlayerEntity player = mc.player;
        float SelfDamage = DamageUtils.anchorDamage(mc.player, TargetFeetPos.toCenterPos());

        if (Objects.requireNonNull(mc.world).getBlockState(TargetFeetPos).getBlock() == Blocks.AIR) {
            BlockUtils.place(TargetFeetPos, anchor, rotate.get(), 100, true);
            InvUtils.swapBack();
            AnchorPos = TargetFeetPos;
        }
        assert mc.interactionManager != null;

        if (mc.world.getBlockState(TargetFeetPos).getBlock() == Blocks.RESPAWN_ANCHOR) {
            if (glowStone.found()) {
                InvUtils.swap(glowStone.slot(), true);
                mc.interactionManager.interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(TargetFeetPos.getX() + 0.5, TargetFeetPos.getY() + 0.5, TargetFeetPos.getZ() + 0.5), Direction.UP, TargetFeetPos, true));
            }
        }

        if (mc.world.getBlockState(TargetFeetPos).getBlock() == Blocks.RESPAWN_ANCHOR) {
            InvUtils.swap(anchor.slot(), true);

            mc.interactionManager.interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(TargetFeetPos.getX() + 0.5, TargetFeetPos.getY() + 0.5, TargetFeetPos.getZ() + 0.5), Direction.UP, TargetFeetPos, true));
            InvUtils.swapBack();
        }
    }
}