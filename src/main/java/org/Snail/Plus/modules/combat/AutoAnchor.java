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
import meteordevelopment.meteorclient.utils.player.ChatUtils;
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
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.Snail.Plus.Addon;
import org.Snail.Plus.utils.CombatUtils;
import org.Snail.Plus.utils.SwapUtils;

import java.util.Objects;


public class AutoAnchor extends Module {

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
            .visible(() -> renderMode.get() == RenderMode.fading)
            .build());

    private final Setting<Integer> Smoothness = sgGeneral.add(new IntSetting.Builder()
            .name("smoothness")
            .description("the smoothness")
            .defaultValue(3)
            .sliderMax(100)
            .sliderMin(1)
            .visible(() -> renderMode.get() == RenderMode.smooth)
            .build());
    private final Setting<SwingMode> swingMode = sgGeneral.add(new EnumSetting.Builder<SwingMode>()
            .name("swing type")
            .description("swing type")
            .defaultValue(SwingMode.mainhand)
            .build());
    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build());
    private long lastAnchorPlaceTime = 0;
    private long lastGlowstonePlaceTime = 0;


    private PlayerEntity target;
    private boolean Swapped;
    private FindItemResult anchor;
    private FindItemResult glowstone;
    private BlockPos AnchorPos;
    private Box renderBoxOne, renderBoxTwo;
    private boolean anchorPlaced = false;

    private BlockPos anchorWest;
    private BlockPos anchorEast;
    private BlockPos anchorNorth;
    private BlockPos anchorSouth;
    private BlockPos targetHeadPos;
    private BlockPos TargetFeetPos;

    public AutoAnchor() {
        super(Addon.Snail, "Auto Anchor+", "Anchor aura but better");

    }

    @Override
    public void onActivate() {
        target = null;
        AnchorPos = null;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        try {
            if (Objects.requireNonNull(mc.world).getDimension().respawnAnchorWorks()) {
                toggle();
                return;
            }
            target = TargetUtils.getPlayerTarget(range.get(), priority.get());
            if (TargetUtils.isBadTarget(target, range.get())) return;
            if (target != null) {
                long currentTime = System.currentTimeMillis();
                if ((currentTime - lastAnchorPlaceTime) < AnchorDelay.get() * 1000) return;
                lastAnchorPlaceTime = currentTime;


                if ((currentTime - lastGlowstonePlaceTime) < GlowstoneDelay.get() * 1000) return;
                lastGlowstonePlaceTime = currentTime;

                Vec3d targetPos = predictMovement.get() ? predictTargetPosition(target) : target.getPos();

                ClientPlayerEntity player = mc.player;


                boolean obsidianFound = false;
                boolean airFound = false;
                boolean respawnAnchorFound = false;
                boolean eastChecked = false;
                boolean northChecked = false;
                boolean southChecked = false;
                boolean westChecked = false;

                anchorWest = target.getBlockPos().west(1);
                anchorEast = target.getBlockPos().east(1);
                anchorNorth = target.getBlockPos().west(1);
                anchorSouth = target.getBlockPos().west(1);
                targetHeadPos = target.getBlockPos().up(2);
                TargetFeetPos = target.getBlockPos().down(1);
                anchorPlaced = false;
                Swapped = false;


                if (Objects.requireNonNull(player).getHealth() <= DisableHealth.get() && DisableHealth.get() > 0)
                    return;

                if (CombatUtils.isBurrowed(target)) return;

                float targetDamage = DamageUtils.anchorDamage(target, target.getBlockPos().toCenterPos());
                float selfDamage = DamageUtils.anchorDamage(player, target.getBlockPos().toCenterPos());

                if (targetDamage < minDamage.get()) {
                    return;
                }

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

                if (!Smart.get() && (mc.world.getBlockState(targetHeadPos).isAir() || this.mc.world.getBlockState(targetHeadPos).getBlock() == Blocks.RESPAWN_ANCHOR)) {
                    TargetHeadPos(target);
                    anchorPlaced = true;
                } else if (Smart.get() && CombatUtils.isSurrounded(target) && mc.world.getBlockState(targetHeadPos).isAir() || mc.world.getBlockState(targetHeadPos).getBlock() == Blocks.RESPAWN_ANCHOR) {
                    TargetHeadPos(target);
                    anchorPlaced = true;
                } else if (!Smart.get() && mc.world.getBlockState(targetHeadPos).getBlock() == Blocks.OBSIDIAN && this.mc.world.getBlockState(TargetFeetPos).isAir()) {
                    TargetFeetPos(target);
                    anchorPlaced = true;
                }

                if (obsidianFound) {

                } else {
                    airFound = true;
                }

                int maxIterations = 10; // Limit the number of iterations to avoid infinite loops and crashing you
                int iterationCount = 0;

                while (!respawnAnchorFound && (!eastChecked || !northChecked || !southChecked || !westChecked) && iterationCount < maxIterations) {
                    iterationCount++;
                    if (safety.get() == SafetyMode.safe && selfDamage > this.maxSelfDamage.get()) continue;
                    if (safety.get() == SafetyMode.balance && selfDamage <= this.maxSelfDamage.get()) continue;
                    if (safety.get() == SafetyMode.off && (selfDamage >= this.maxSelfDamage.get() || selfDamage <= this.maxSelfDamage.get()))
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
        } catch (Exception e) {
            ChatUtils.sendMsg(Text.of("found error" + e.getClass().getName() + "Please report this to seasnail1:" + e.getMessage()));
        }
    }

    private Vec3d predictTargetPosition(PlayerEntity target) {
        return target.getPos().add(target.getVelocity());
    }

    @EventHandler
    public void AnchorRender(Render3DEvent event) {

        RenderMode mode = renderMode.get();
        if (TargetUtils.isBadTarget(target, range.get())) return;
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
            } else if (mode == RenderMode.smooth) {
                if (renderBoxOne == null) renderBoxOne = new Box(AnchorPos);
                if (renderBoxTwo == null) renderBoxTwo = new Box(AnchorPos);

                // Set renderBoxTwo to the target position (AnchorPos)
                ((IBox) renderBoxTwo).set(
                        AnchorPos.getX(), AnchorPos.getY(), AnchorPos.getZ(),
                        AnchorPos.getX() + 1, AnchorPos.getY() + 1, AnchorPos.getZ() + 1
                );

                // Calculate the offsets
                double offsetX = (renderBoxTwo.minX - renderBoxOne.minX) / Smoothness.get();
                double offsetY = (renderBoxTwo.minY - renderBoxOne.minY) / Smoothness.get();
                double offsetZ = (renderBoxTwo.minZ - renderBoxOne.minZ) / Smoothness.get();

                // Update renderBoxOne position
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
        ClientPlayerEntity player = mc.player;
        AnchorPos = anchorEast;
        if (!Swapped) {
            SwapAndPlace();
        }

    }

    private void PlaceWestAnchor(PlayerEntity target) {
        AnchorPos = anchorWest;
        if (!Swapped) {
            SwapAndPlace();
        }
    }

    private void PlaceSouthAnchor(PlayerEntity target) {
        AnchorPos = anchorSouth;
        if (!Swapped) {
            SwapAndPlace();
        }
    }

    private void PlaceNorthAnchor(PlayerEntity target) {
        AnchorPos = anchorNorth;
        if (!Swapped) {
            SwapAndPlace();
        }
    }

    private void TargetHeadPos(PlayerEntity target) {
        AnchorPos = targetHeadPos;
        if (!Swapped) {
            SwapAndPlace();
        }
    }

    private void TargetFeetPos(PlayerEntity target) {
        AnchorPos = TargetFeetPos;
        if (!Swapped) {
            SwapAndPlace();
        }
    }

    @Override
    public void onDeactivate() {
        target = null;
        AnchorPos = null;
    }

    private void SwapAndPlace() {
        ClientPlayerEntity player = mc.player;

        switch (swapMethod.get()) {
            case normal:
                anchor = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
                glowstone = InvUtils.findInHotbar(Items.GLOWSTONE);

                if (mc.world.getBlockState(AnchorPos).getBlock() == Blocks.RESPAWN_ANCHOR || mc.world.getBlockState(AnchorPos).getBlock() == Blocks.FIRE || mc.world.getBlockState(AnchorPos).isAir()) {
                    BlockUtils.place(AnchorPos, anchor, rotate.get(), 100, true);
                }
                if (mc.world.getBlockState(AnchorPos).getBlock() == Blocks.RESPAWN_ANCHOR) {
                    SwapUtils.Normal(glowstone.slot(), 1.0F);
                    Objects.requireNonNull(mc.interactionManager).interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(AnchorPos.getX() + 0.5, AnchorPos.getY() + 0.5, AnchorPos.getZ() + 0.5), Direction.UP, AnchorPos, true));
                    SwapUtils.Normal(anchor.slot(), 0.0F);
                    Objects.requireNonNull(mc.interactionManager).interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(AnchorPos.getX() + 0.5, AnchorPos.getY() + 0.5, anchorEast.getZ() + 0.5), Direction.UP, AnchorPos, true));
                    SwapUtils.Normal(anchor.slot(), 0.0F);
                    Swapped = true;
                }
                break;
            case silent:
                anchor = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
                glowstone = InvUtils.findInHotbar(Items.GLOWSTONE);

                if (mc.world.getBlockState(AnchorPos).getBlock() == Blocks.RESPAWN_ANCHOR || mc.world.getBlockState(AnchorPos).getBlock() == Blocks.FIRE || mc.world.getBlockState(AnchorPos).isAir()) {
                    BlockUtils.place(AnchorPos, anchor, rotate.get(), 100, true);
                }
                if (mc.world.getBlockState(AnchorPos).getBlock() == Blocks.RESPAWN_ANCHOR) {
                    SwapUtils.SilentSwap(glowstone.slot(), 1.0F);
                    Objects.requireNonNull(mc.interactionManager).interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(AnchorPos.getX() + 0.5, AnchorPos.getY() + 0.5, AnchorPos.getZ() + 0.5), Direction.UP, AnchorPos, true));
                    SwapUtils.SilentSwap(anchor.slot(), 0.0F);
                    Objects.requireNonNull(mc.interactionManager).interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(AnchorPos.getX() + 0.5, AnchorPos.getY() + 0.5, AnchorPos.getZ() + 0.5), Direction.UP, AnchorPos, true));
                    Swapped = true;
                }
                break;
            case invSwitch:
                int originalSlot = Objects.requireNonNull(mc.player).getInventory().selectedSlot;
                anchor = InvUtils.find(Items.RESPAWN_ANCHOR);
                glowstone = InvUtils.find(Items.GLOWSTONE);

                if (mc.world.getBlockState(AnchorPos).getBlock() == Blocks.RESPAWN_ANCHOR || mc.world.getBlockState(AnchorPos).getBlock() == Blocks.FIRE || mc.world.getBlockState(AnchorPos).isAir())
                    BlockUtils.place(AnchorPos, anchor, rotate.get(), 100, true);

                if (mc.world.getBlockState(AnchorPos).getBlock() == Blocks.RESPAWN_ANCHOR) {
                    SwapUtils.invSwitch(originalSlot, anchor.slot(), true, 0.0F);
                    Objects.requireNonNull(mc.interactionManager).interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(AnchorPos.getX() + 0.5, AnchorPos.getY() + 0.5, AnchorPos.getZ() + 0.5), Direction.UP, AnchorPos, true));
                    SwapUtils.SilentSwap(anchor.slot(), 0.0F);
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