package com.example.addon.modules.combat;

import com.example.addon.Addon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.movement.Anchor;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
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
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import com.example.addon.utils.PlayerUtils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;

import java.util.Objects;

public class AutoAnchor extends Module {


    public enum SafetyMode {
        SAFE,
        BALANCE,
        OFF,
    }

    public enum PlaceMode {
        side,
        line,
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private long lastPlaceTime = 0;
    private BlockPos AnchorPos;

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
            .defaultValue(SafetyMode.SAFE)
            .build());

    private final Setting<Double> delay = sgGeneral.add(new DoubleSetting.Builder()
            .name("delay")
            .description("Delay in seconds between each block placement.")
            .defaultValue(1.0)
            .min(0)
            .sliderMax(5)
            .build());

    private final Setting<Double> maxSelfDamage = sgGeneral.add(new DoubleSetting.Builder()
            .name("self damage")
            .description("the max amount to deal to you")
            .defaultValue(3.0)
            .sliderMax(10.0)
            .sliderMin(1.0)
            .build());

    private final Setting<Boolean> placeSupport = sgGeneral.add(new BoolSetting.Builder()
            .name("place-support")
            .description("Whether to place support blocks.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> Smart = sgGeneral.add(new BoolSetting.Builder()
            .name("smart")
            .description("more calculations (eats more cpu)")
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

    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build());

    public AutoAnchor() {
        super(Addon.Snail, "Auto Anchor", "Automatically places Respawn Anchors near players.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        ClientPlayerEntity player = mc.player;
        long time = System.currentTimeMillis();
        if ((time - lastPlaceTime) < delay.get() * 1000) return;
        lastPlaceTime = time;

        if (player == null || player.getHealth() <= 0) return;
        PlayerEntity target = TargetUtils.getPlayerTarget(range.get(), priority.get());
        FindItemResult anchor = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
        FindItemResult glowStone = InvUtils.findInHotbar(Items.GLOWSTONE);

        if (target == null || target == player || target.distanceTo(player) > range.get()) {
            return;
        }

        BlockPos targetHeadPos = target.getBlockPos().up(2);



        BlockPos anchorWest = target.getBlockPos().west(1);
        BlockPos anchorEast = target.getBlockPos().east(1);
        BlockPos anchorSouth = target.getBlockPos().south(1);
        BlockPos anchorNorth = target.getBlockPos().north(1);

        if (placeSupport.get()) {
            placeSupportBlocks(target);
        }

        if(PlayerUtils.isSurrounded(target) && Smart.get()) {
            // Place Respawn Anchor
            if (mc.world.getBlockState(targetHeadPos).getBlock() == Blocks.AIR) {
                BlockUtils.place(targetHeadPos, anchor, rotate.get(), 0, false);
                InvUtils.swapBack();
            }

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
        if (!PlayerUtils.isSurrounded(target) && Smart.get()) {
            // TODO: Find the best pos to place the anchor
            float SelfDamage = DamageUtils.anchorDamage(mc.player, anchorEast.toCenterPos());

        if(SelfDamage > maxSelfDamage.get() || (safety.get() == SafetyMode.SAFE && SelfDamage >= EntityUtils.getTotalHealth(Objects.requireNonNull(mc.player)))) {
            ChatUtils.sendMsg(Text.of("found a bad location..."));
            return;
            } else if(SelfDamage < maxSelfDamage.get() || (safety.get() == SafetyMode.SAFE && SelfDamage <= EntityUtils.getTotalHealth(Objects.requireNonNull(mc.player)))) {
            ChatUtils.sendMsg(Text.of("found a good location..."));
            AnchorPos = anchorEast;
            }
        }
    }

    @EventHandler
    public void AnchorRender(Render3DEvent event) {

        event.renderer.box(AnchorPos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }

    private void placeSupportBlocks(PlayerEntity target) {
        BlockPos supportPosNorth = target.getBlockPos().north(1);
        BlockPos supportPosNorthUpOne = target.getBlockPos().north(1).up(1);
        BlockPos supportPosNorthUpTwo = target.getBlockPos().north(1).up(2);

        BlockUtils.place(supportPosNorth, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, false);
        BlockUtils.place(supportPosNorthUpOne, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, false);
        BlockUtils.place(supportPosNorthUpTwo, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, false);
    }
}

